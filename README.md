# TradeGuard

TradeGuard is an educational securities trading simulator. It uses fictitious data and is not a real trading system or investment advice.

## Current implementation

- `backend-java/`: Java 21 / Spring Boot REST API, Flyway-managed PostgreSQL schema, and instrument CRUD.
- `docker-compose.yml`: local PostgreSQL 16 and Redis 7 services. Redis is provisioned for later phases and is not yet used by the backend.
- `HANDOFF.md`: phase estimates, current delivery status, and the remaining implementation checklist.

## Local backend

Prerequisites: JDK 21, Docker Desktop, and network access on the first Maven Wrapper run.

1. Copy `.env.example` to `.env`. Change the password if this environment is shared.
2. Start the local data services with `docker compose up -d postgres redis`.
3. In PowerShell, run:

   ```powershell
   cd backend-java
   .\mvnw.cmd spring-boot:run
   ```

4. Open `http://localhost:8080/api/health`.

The API uses `http://localhost:8080/api/market/instruments` for instrument CRUD and `GET /api/market/{symbol}/info` for the TradeGuard market-info route. List responses are paginated (`page`, `size`, `sort`; maximum size 100). Creates return `201 Created` with a `Location` header. Errors use the common JSON shape with `timestamp`, `status`, `error`, `message`, `path`, and optional `details`.

Run backend integration tests without Docker:

```powershell
cd backend-java
.\mvnw.cmd test
```

Tests use H2 in PostgreSQL compatibility mode; local development uses PostgreSQL and Flyway migrations. Authentication/authorization and trading features are not implemented yet, so this API is for local development only and must not be exposed as a production service.
