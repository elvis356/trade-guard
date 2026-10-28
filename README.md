# TradeGuard

TradeGuard is an educational securities trading simulator. It uses fictitious data and is not a real trading system or investment advice.

## Current implementation

- `backend-java/`: Java 21 / Spring Boot REST API, Flyway-managed PostgreSQL schema, instrument CRUD, JWT-cookie authentication, and role-based authorization.
- Limit orders are validated against the configurable reference-price band, illustrative tick table, 1,000-share lot, and Taipei weekday trading session. Matching uses price/time priority, maker price, partial fills, and PostgreSQL transactions.
- Order acceptance, cancellation, and fills are recorded in the append-only application event log in the same transaction. Startup replays events to rebuild open-order state and fails closed if replayed orders or per-symbol sequence counters disagree with PostgreSQL.
- Public quote and order-book snapshots use short-lived Redis caches and fall back to PostgreSQL when Redis is unavailable. Surveillance persists same-account wash-trade and configurable closing-window price/volume alerts; case proposal and review require different surveillance users.
- Key order, trade, and case actions are written to a SHA-256 chained audit log in the same PostgreSQL transaction. Auditor/admin APIs verify the chain and reconcile order, trade, and event ledgers; a protected cron endpoint runs end-of-day reconciliation, and daily trades are available as XML.
- `docker-compose.yml`: local PostgreSQL 16 and Redis 7 services. Redis backs login lockout and JWT revocation.
- `HANDOFF.md`: phase estimates, current delivery status, and the remaining implementation checklist.

## Local backend

Prerequisites: JDK 21, Docker Desktop, and network access on the first Maven Wrapper run.

1. Copy `.env.example` to `.env`. Set a unique `JWT_SECRET` of at least 32 bytes and change the database password if this environment is shared. Set one-time bootstrap administrator credentials as well. Keep real secrets out of Git.
2. Set one-time bootstrap administrator credentials in `.env`, then start the complete local stack with `docker compose up --build -d`. The backend is available at `http://localhost:8080`.
3. After the first successful startup, remove `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` from `.env`. For a non-container backend start, configure the local profile and required settings in PowerShell:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "local"
   $env:BOOTSTRAP_ADMIN_USERNAME = "admin"
   $env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Set a unique admin password (at least 12 characters)"
   $env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   cd backend-java
   .\mvnw.cmd spring-boot:run
   ```

   After the first admin account is created, remove `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` from the process environment. Deployments must set a unique `JWT_SECRET` (at least 32 bytes) and `COOKIE_SECURE=true`.

   If you changed `DB_PASSWORD` in `.env`, set the same value in the backend process (`$env:DB_PASSWORD`) before starting Spring Boot; Docker Compose reads `.env`, but PowerShell does not load it automatically.

4. Open `http://localhost:8080/api/health`.

Log in at `POST /api/auth/login`; the API sets a short-lived HttpOnly, SameSite=Lax cookie. `POST /api/auth/logout` revokes the token in Redis and clears the cookie. Create role-specific accounts while signed in as the bootstrap admin with `POST /api/admin/users` (`TRADER`, `SURVEILLANCE`, `AUDITOR`, or `ADMIN`); admins can page through accounts with `GET /api/admin/users` or fetch one with `GET /api/admin/users/{id}`.

Instrument CRUD is at `http://localhost:8080/api/market/instruments`; market info and recent trades are at `GET /api/market/{symbol}/info` and `GET /api/market/{symbol}/trades`. Place and cancel limit orders at `POST /api/orders` and `DELETE /api/orders/{id}`; `GET /api/orders/mine` returns the authenticated trader's paginated order history. Order requests include a UUID `client_order_id`, symbol, side (`B`/`S`), limit price, and quantity. Repeating an identical client order ID for the same trader returns its original order; reusing it for different order content returns `409`. Market reads and health checks are public; instrument writes and user administration require `ADMIN`. Order lists are paginated (`page`, `size`, `sort`; maximum size 100). Creates return `201 Created` with a `Location` header. Protected trading, surveillance, and audit routes enforce the role mappings in `SecurityConfiguration`. Unsafe `/api` requests with an `Origin` header are accepted only from `CORS_ALLOWED_ORIGINS`. Errors use the common JSON shape with `timestamp`, `status`, `error`, `message`, `path`, and optional `details`.

Market quote and book snapshots are available at `GET /api/market/quotes` and `GET /api/market/{symbol}/book`. Surveillance users can review paginated alerts at `GET /api/alerts`, propose a case with `POST /api/alerts/{id}/case`, list cases at `GET /api/cases`, and submit an approve/reject decision with a note to `POST /api/cases/{id}/review`. The proposer cannot review their own case; alerts are rule-based leads for human review, not findings of misconduct.

Auditors and admins can call `GET /api/audit/verify` to validate the audit hash chain, `GET /api/audit/reconcile` to compare orders, trades, and order events, and `GET /api/reports/daily.xml?date=YYYY-MM-DD` for a Taipei-timezone daily trade report. A scheduled external Cron Job can call `POST /internal/eod?date=YYYY-MM-DD` with `X-EOD-Token`; configure a strong random `EOD_TOKEN` of at least 32 bytes in the backend environment. When the ledgers reconcile, the job stores the XML report in PostgreSQL and returns its SHA-256 checksum; unsuccessful reconciliation does not publish a report.

## Next.js frontend

The `frontend-next/` App Router project provides an SSR market overview with five-second quote polling, streaming symbol pages with a two-second order-book refresh, a static matching-rules guide, a role-protected daily XML report view, and an authenticated on-demand revalidation hook. The browser uses same-origin `/api/*` rewrites; server-side backend access uses the server-only `BACKEND_URL`.

With Node.js 20.9 or later and the backend running, start the frontend from PowerShell:

```powershell
cd frontend-next
Copy-Item .env.example .env.local
npm install
npm run dev
```

Set `REVALIDATE_SECRET` to a high-entropy secret before exposing the frontend. `POST /api/revalidate` requires the same value in `X-Revalidate-Secret`. The daily report contains role-protected data and is deliberately fetched per request without a shared ISR cache; shared caching must not bypass backend authorization.

## CI, security checks, and deployment

GitHub Actions runs backend `clean verify`, frontend lint/build, and the npm dependency audit on pushes and pull requests. Dependency Review blocks pull requests that introduce high-severity dependency advisories. CodeQL scans Java and TypeScript on pushes, pull requests, and its weekly schedule. The dependency lockfile is committed so CI uses reproducible frontend versions.

The Render Blueprint in `render.yaml` defines private Redis, PostgreSQL, the Dockerized API, and a weekday EOD Cron job. Review the selected Render plans and region before applying it. Set `CORS_ALLOWED_ORIGINS` to the exact deployed Vercel origin. On initial provisioning, supply `BOOTSTRAP_ADMIN_USERNAME` and a unique `BOOTSTRAP_ADMIN_PASSWORD`; remove both values after the admin is created. Render generates `JWT_SECRET` and `EOD_TOKEN`.

Deploy the frontend separately by importing the repository in Vercel and setting its root directory to `frontend-next`. Configure the server-only `BACKEND_URL` with the deployed Render API URL. Set the same high-entropy `REVALIDATE_SECRET` on Vercel and in any backend integration that will call the frontend revalidation route. No credentials or external deployment are committed or performed by CI.

Run backend integration tests without Docker:

```powershell
cd backend-java
.\mvnw.cmd test
```

Tests use H2 in PostgreSQL compatibility mode and mock Redis-backed authentication state; market and surveillance Redis outages are exercised through the PostgreSQL fallback. Local development uses PostgreSQL, Redis, and Flyway migrations. PostgreSQL/Redis runtime integration has not yet been verified on this machine. The surveillance volume/price rule needs a 20-day daily-bar source before implementation; database-level write-protection for event and audit tables and Redis idempotency acceleration remain future hardening work. The frontend has an initial market/read-only vertical slice; authenticated trading, surveillance, case review, and audit workflows plus production deployment remain incomplete.
