# TradeGuard

TradeGuard is an educational securities trading simulator. It uses fictitious data and is not a real trading system or investment advice.

## Current implementation

- `backend-java/`: Java 21 / Spring Boot REST API, Flyway-managed PostgreSQL schema, instrument CRUD, JWT-cookie authentication, and role-based authorization.
- Limit orders are validated against the configurable reference-price band, illustrative tick table, 1,000-share lot, and Taipei weekday trading session. Matching uses price/time priority, maker price, partial fills, and PostgreSQL transactions.
- Order acceptance, cancellation, and fills are recorded in the append-only application event log in the same transaction. Startup replays events to rebuild open-order state and fails closed if replayed orders or per-symbol sequence counters disagree with PostgreSQL.
- `docker-compose.yml`: local PostgreSQL 16 and Redis 7 services. Redis backs login lockout and JWT revocation.
- `HANDOFF.md`: phase estimates, current delivery status, and the remaining implementation checklist.

## Local backend

Prerequisites: JDK 21, Docker Desktop, and network access on the first Maven Wrapper run.

1. Copy `.env.example` to `.env`. Change the database password if this environment is shared.
2. Start the local data services with `docker compose up -d postgres redis`.
3. Before the first backend start, configure a one-time bootstrap administrator and a signing key in PowerShell. Use unique secrets and do not commit them:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "local"
   $env:BOOTSTRAP_ADMIN_USERNAME = "admin"
   $env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Set a unique admin password (at least 12 characters)"
   $env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   cd backend-java
   .\mvnw.cmd spring-boot:run
   ```

   After the first admin account is created, remove `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` from the process environment. The local profile has a development-only signing-key fallback; deployments must set a unique `JWT_SECRET` (at least 32 bytes) and `COOKIE_SECURE=true`.

   If you changed `DB_PASSWORD` in `.env`, set the same value in the backend process (`$env:DB_PASSWORD`) before starting Spring Boot; Docker Compose reads `.env`, but PowerShell does not load it automatically.

4. Open `http://localhost:8080/api/health`.

Log in at `POST /api/auth/login`; the API sets a short-lived HttpOnly, SameSite=Lax cookie. `POST /api/auth/logout` revokes the token in Redis and clears the cookie. Create role-specific accounts while signed in as the bootstrap admin with `POST /api/admin/users` (`TRADER`, `SURVEILLANCE`, `AUDITOR`, or `ADMIN`); admins can page through accounts with `GET /api/admin/users` or fetch one with `GET /api/admin/users/{id}`.

Instrument CRUD is at `http://localhost:8080/api/market/instruments`; market info and recent trades are at `GET /api/market/{symbol}/info` and `GET /api/market/{symbol}/trades`. Place and cancel limit orders at `POST /api/orders` and `DELETE /api/orders/{id}`; `GET /api/orders/mine` returns the authenticated trader's paginated order history. Order requests include a UUID `client_order_id`, symbol, side (`B`/`S`), limit price, and quantity. Repeating an identical client order ID for the same trader returns its original order; reusing it for different order content returns `409`. Market reads and health checks are public; instrument writes and user administration require `ADMIN`. Order lists are paginated (`page`, `size`, `sort`; maximum size 100). Creates return `201 Created` with a `Location` header. Protected trading, surveillance, and audit routes enforce the role mappings in `SecurityConfiguration`. Unsafe `/api` requests with an `Origin` header are accepted only from `CORS_ALLOWED_ORIGINS`. Errors use the common JSON shape with `timestamp`, `status`, `error`, `message`, `path`, and optional `details`.

Run backend integration tests without Docker:

```powershell
cd backend-java
.\mvnw.cmd test
```

Tests use H2 in PostgreSQL compatibility mode and mock Redis-backed authentication state; local development uses PostgreSQL, Redis, and Flyway migrations. PostgreSQL/Redis runtime integration has not yet been verified on this machine. The event log and startup replay are implemented, but database-level write-protection for the event table and Redis idempotency acceleration remain future hardening work; surveillance, audit, the frontend, and production deployment are also incomplete.
