# 本機開發

← [回 README](../README.md)

## 專案內容

### 後端與交易

`backend-java/` 使用 Java 21、Spring Boot REST API、Flyway 管理的 PostgreSQL 結構，並提供標的 CRUD、JWT Cookie 驗證及角色型授權。

限價單依可設定的參考價格區間、示意性升降單位表、1,000 股整股交易單位及台北平日交易時段進行驗證。撮合採價格／時間優先、做市方價格、部分成交及 PostgreSQL 交易機制。委託接受、取消及成交會在同一筆交易中寫入僅附加的應用程式事件紀錄。啟動時系統會重播事件並驗證委託投影與序號的一致性；撮合仍直接讀取 PostgreSQL 委託資料。

### 市場監控與稽核

公開報價與委託簿快照使用短效 Redis 快取；Redis 無法使用時會改由 PostgreSQL 提供資料。監控功能會保存同帳戶洗售交易，以及可設定收盤時段的價格／成交量警示。案件提報與審核必須由不同的監控使用者執行。

重要委託、交易及案件操作會在同一筆 PostgreSQL 交易中寫入 SHA-256 鏈式稽核紀錄。稽核員／管理員 API 可驗證紀錄鏈並核對委託、交易及事件帳本；受保護的 Cron 端點會執行每日收盤核對，每日交易資料也可下載為 XML。

### 其他專案檔案

- `docker-compose.yml`：本機 PostgreSQL 16、Redis 7 與後端服務；Redis 用於登入鎖定及 JWT 撤銷。
- `HANDOFF.md`：目前狀態摘要與後續工作。
- `docs/functional-requirements-specification.md`：依目前程式實作整理的功能需求、角色權限、業務規則與已知限制。
- `docs/uml-workbench.html`：互動式 UML 工作台；圖表資料集中於 HTML 的 `uml-data` 區塊，含架構、後端模組、使用案例、活動、時序、類別、ERD、狀態與部署／CI 流程圖，支援跨圖追溯、跨圖搜尋與清單檢視。修改資料後執行 `node docs/uml-workbench.check.js` 驗證。線上檢視：[https://elvis356.github.io/trade-guard/uml-workbench.html](https://elvis356.github.io/trade-guard/uml-workbench.html)

## 啟動方式總覽

本機有兩種啟動方式，擇一即可：

| 方式 | 需要安裝 | 適合情境 |
| ---- | -------- | -------- |
| [使用 Docker Compose 啟動](#使用-docker-compose-啟動) | JDK 21、Docker Desktop | 一鍵啟動 PostgreSQL、Redis 與後端 |
| [不使用容器啟動後端](#不使用容器啟動後端) | JDK 21、PostgreSQL 16、Redis 7 | 不想安裝 Docker，或想直接除錯 Spring Boot |

兩種方式的 PostgreSQL 與 Redis 都開在 `localhost`，因此 [DBeaver](#5-使用-dbeaver-檢視-postgresql) 與 [Redis Insight](#6-使用-redis-insight-檢視-redis) 的檢視步驟相同。前端的啟動方式見[啟動前端](#啟動前端)。

## 必要條件

- 後端：JDK 21，以及首次執行 Maven Wrapper 時所需的網路連線。
- 前端：Node.js 20.9 或更新版本。
- 使用 Docker Compose 時：另需 Docker Desktop。
- 不使用容器時：改為需要 PostgreSQL 16 與 Redis 7（詳見[不使用容器啟動後端](#不使用容器啟動後端)），不需要 Docker。

## 使用 Docker Compose 啟動

1. 將 `.env.example` 複製為根目錄的 `.env.local`。若檔案已存在，不要重複複製，以免覆蓋原有設定。此範例含本機專用設定與 JWT 值，不可用於共用或部署環境。

2. `.env.example` 已附本機測試用的初始管理員帳號；複製後即可使用。如需更改，編輯 `.env.local` 中的 `BOOTSTRAP_ADMIN_USERNAME` 與 `BOOTSTRAP_ADMIN_PASSWORD`。這組公開範例帳密僅供本機測試，不可用於部署或可由網際網路存取的環境。

3. 啟動 Docker Desktop，等待 Docker Engine 顯示正在執行，並確認使用 Linux containers。可在 PowerShell 執行 `docker info` 確認 Docker CLI 能連線。

4. 啟動完整本機服務：

   ```
   docker compose --env-file .env.local up --build -d
   ```

   後端啟動後位於 `http://localhost:8080`。

5. 確認服務依賴狀態：開啟 `http://localhost:8080/api/health/readiness`。人類檢視使用 readiness HTML 頁面；監控程式可傳送 `Accept: application/json` 取得 JSON（服務未就緒時回 `503`）。`/api/health/status` 是供機器檢查 API 本身的 JSON 端點，不檢查資料庫或 Redis。

Docker Compose 指令需明確使用 `--env-file .env.local`；Compose 讀取該檔案中的 `DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_PASSWORD` 等設定。Compose 內部資料庫與 Redis 主機仍分別使用服務名稱 `postgres`、`redis` 和容器連接埠 `5432`、`6379`。PostgreSQL 的 `POSTGRES_*` 初始化設定只在資料目錄首次建立時生效；若 named volume 已初始化，修改 `.env.local` 不會自動建立新使用者、改資料庫名稱或更新既有密碼，需先妥善遷移／調整資料庫角色，勿直接刪除 volume 以免遺失資料。Redis 密碼會同時設定 Compose Redis 伺服器與後端連線；本機預設留空。Compose 只把 PostgreSQL 的 5432 綁定在 `127.0.0.1`，因此僅能從本機連線。

## 不使用容器啟動後端

完全不使用 Docker 時，需要自行在本機安裝 PostgreSQL 16 與 Redis 7，再啟動後端。整個流程為：安裝 PostgreSQL → 建立資料庫 → 安裝 Redis → 啟動後端 → 以 DBeaver／Redis Insight 檢視資料。

### 1. 安裝 PostgreSQL 16

- 從 [postgresql.org/download](https://www.postgresql.org/download/windows/) 下載 Windows 安裝程式，或執行 `winget install PostgreSQL.PostgreSQL.16`。
- 安裝時記下 `postgres` 超級使用者密碼，連接埠維持 `5432`。
- 安裝後確認服務正在執行：

```
Get-Service postgresql*
```

### 2. 建立專用資料庫與使用者

以超級使用者進入 `psql`（路徑視安裝位置調整；若找不到指令，請使用安裝目錄下 `bin` 內的 `psql.exe`）：

```
psql -h localhost -U postgres
```

執行下列 SQL。使用者名稱、密碼與資料庫名稱刻意與 `application.yml` 的預設值一致，後端不需額外設定連線變數：

```
CREATE ROLE tradeguard LOGIN PASSWORD 'tradeguard_local_only';
CREATE DATABASE tradeguard OWNER tradeguard;
```

資料表結構由後端啟動時的 Flyway 自動建立，不需手動匯入。此密碼僅供本機使用，不可用於共用或部署環境。

### 3. 安裝 Redis 7

Redis 官方沒有提供原生 Windows 版本，請擇一（兩者都不需要 Docker）：

- **WSL2（建議）**：在 WSL 的 Ubuntu 內安裝並啟動。

  ```
  wsl --install
  sudo apt update && sudo apt install -y redis-server
  redis-server --appendonly yes --daemonize yes
  redis-cli ping
  ```

  `redis-cli ping` 應回傳 `PONG`。WSL2 預設會將 `localhost:6379` 轉送到 Windows。每次重新開機後需再次啟動 `redis-server`。

- **Memurai Developer**：Windows 原生、相容 Redis 協定的免費開發版，從 [memurai.com](https://www.memurai.com/) 下載安裝，安裝後會以 Windows 服務執行於 `6379`。

Redis 用於登入鎖定、JWT 撤銷與報價快取，後端需要它才能完整運作。

### 4. 設定環境變數並啟動後端

`application.yml` 已為資料庫與 Redis 連線提供本機預設值（見〈後端環境變數說明〉），只要依步驟 2、3 建立服務，就只需設定下列必要變數。PowerShell 不會自動載入 `.env.local`，請在啟動後端的同一個視窗設定：

```
$env:SPRING_PROFILES_ACTIVE = "local"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Set a unique admin password (at least 12 characters)"
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
cd backend-java
.\mvnw.cmd spring-boot:run
```

- 上述寫法相容於 Windows PowerShell 5.1；若使用 PowerShell 7，可直接以 `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` 產生 `JWT_SECRET`。
- `SPRING_PROFILES_ACTIVE=local` 不可省略：未啟用時 `COOKIE_SECURE` 預設為 `true`，純 HTTP 登入不會送出 Cookie。
- `CORS_ALLOWED_ORIGINS` 預設為 `http://localhost:3000`；前端若使用其他埠，請在啟動後端前設定 `$env:CORS_ALLOWED_ORIGINS` 為該來源網址。
- 若資料庫或 Redis 的位置、帳號、密碼與步驟 2、3 不同，請另外設定對應變數，例如：

  ```
  $env:DB_HOST = "localhost"
  $env:DB_PORT = "5432"
  $env:DB_NAME = "tradeguard"
  $env:DB_USERNAME = "tradeguard"
  $env:DB_PASSWORD = "<你的資料庫密碼>"
  $env:REDIS_HOST = "localhost"
  $env:REDIS_PORT = "6379"
  $env:REDIS_PASSWORD = "<Redis 密碼，未設定可省略>"
  ```

- 部署時必須設定唯一的 `JWT_SECRET`（至少 32 位元組）及 `COOKIE_SECURE=true`。

確認啟動成功：開啟 `http://localhost:8080/api/health/readiness`，PostgreSQL 與 Redis 都應顯示正常。

### 5. 使用 DBeaver 檢視 PostgreSQL

DBeaver Community 是免費的資料庫管理工具，可連線本機 PostgreSQL，檢視 Flyway 建立的結構與資料。

- 下載：[https://dbeaver.io/download/](https://dbeaver.io/download/)（Windows 可選安裝檔或 ZIP，另有 macOS、Linux 版本）。也可用 `choco install dbeaver` 安裝。
- 在 DBeaver 新增 PostgreSQL 連線，填入下列設定：

| 欄位     | 值                                                              |
| -------- | -------------------------------------------------------------- |
| Host     | `localhost`                                                    |
| Port     | `5432`                                                         |
| Database | `tradeguard`（`DB_NAME`）                                      |
| Username | `tradeguard`（`DB_USERNAME`）                                  |
| Password | 步驟 2 設定的密碼（`DB_PASSWORD`），範例為 `tradeguard_local_only` |

若連不上，先確認 PostgreSQL 服務正在執行，並用 `psql` 測試連線（使用 Docker Compose 時，改以 `docker compose --env-file .env.local ps` 確認 postgres 容器狀態）：

```
psql -h localhost -U tradeguard -d tradeguard -c "SELECT 1;"
```

常用的檢視起點：

- `flyway_schema_history`：已套用的 migration 版本與時間。
- 業務資料表：`instruments`、`app_users`、`orders`、`trades`、`order_events`、`surveillance_alerts`、`surveillance_cases`、`audit_entries`、`audit_chain_state`、`eod_reports`、`eod_runs`。

```
SELECT id, symbol, side, status, created_at
FROM orders
ORDER BY created_at DESC
LIMIT 20;

SELECT seq, event_type, created_at
FROM audit_entries
ORDER BY seq DESC
LIMIT 20;
```

使用限制：

- 以唯讀方式使用，只執行 `SELECT`。直接修改業務資料表會讓委託投影、事件帳本與稽核鏈不一致，後續核對與鏈驗證會失敗。
- 不要以手動新增 `app_users` 資料列的方式建立帳號；密碼以雜湊儲存，角色與稽核流程需經 API 完成。交易員帳號由 `/register` 建立，其他角色由管理員 API 建立。
- `audit_entries` 僅附加，任何修改都會讓鏈驗證失敗。

### 6. 使用 Redis Insight 檢視 Redis

Redis Insight 是 Redis 官方提供的免費 GUI，可瀏覽鍵值、TTL 與內容，適合確認快取與登入狀態。

- 下載：[https://redis.io/insight/](https://redis.io/insight/)（Windows、macOS、Linux）。
- 連線設定：Host `localhost`、Port `6379`；`REDIS_PASSWORD` 留空時不需要密碼，若有設定則填入該值。
- 連不上時先確認 Redis 正在執行：

```
redis-cli ping
```

本專案使用的鍵：

| 鍵樣式                                                    | 內容                                     | 有效期                                                       |
| ------------------------------------------------------ | -------------------------------------- | --------------------------------------------------------- |
| `tradeguard:login:fail:<sha256>`                       | 帳號連續登入失敗次數，達 5 次即鎖定                    | 15 分鐘（`login-lock-duration`）                              |
| `tradeguard:rate:<sha256>`                             | 登入與註冊的請求計數                             | 1 分鐘、1 小時（`login-rate-window`、`registration-rate-window`） |
| `tradeguard:jwt:deny:<jti>`                            | 已撤銷的 JWT                               | 至該權杖到期為止                                                  |
| `tradeguard:quote:<symbol>`、`tradeguard:book:<symbol>` | 報價與委託簿快照快取                             | 15 秒                                                      |
| `tradeguard:surveillance:window:<symbol>`              | 監控時間窗內的成交（sorted set，score 為 epoch 毫秒） | 監控視窗加 1 分鐘                                                |

鍵名中的 `<sha256>` 是帳號或來源的雜湊值，無法直接對應回使用者名稱，這是刻意設計。

使用限制：

- 可安全刪除 `tradeguard:quote:*`、`tradeguard:book:*`，後端會回源 PostgreSQL 重建。
- 不要刪除 `tradeguard:jwt:deny:*`，否則已撤銷的權杖會恢復可用。
- 刪除 `tradeguard:login:fail:*` 等於解除該帳號的登入鎖定，僅在測試時使用。
- 重啟後是否保留這些鍵，取決於 Redis 的持久化設定。Docker Compose 的 Redis 與上述 WSL 指令都啟用 AOF（`--appendonly yes`），重啟後不會清掉上述狀態；使用 Memurai 時請依其持久化設定為準。

## 後端環境變數說明

後端設定來自 `backend-java` 的 `application.yml`，環境變數未設定時會使用下表的預設值。根目錄的 `.env.example` 列出本機 Docker Compose 使用的設定；直接執行後端時也適用同一組變數。PostgreSQL 連線變數也與 Render 資料庫欄位一一對應；後端會用它們組成 JDBC 連線網址：

| 變數 | 預設值 | 用途 |
| ---- | ------ | ---- |
| `DB_HOST` | `localhost` | PostgreSQL 主機名稱；Compose 內固定使用服務名稱 `postgres`，直接在本機執行後端時使用 `localhost`。 |
| `DB_PORT` | `5432` | PostgreSQL 連接埠；Compose 內固定使用容器連接埠 `5432`。 |
| `DB_NAME` | `tradeguard` | PostgreSQL 資料庫名稱；Compose 同時用於初始化 PostgreSQL 與後端連線。 |
| `DB_USERNAME`、`DB_PASSWORD` | `tradeguard`、`tradeguard_local_only` | PostgreSQL 使用者名稱與密碼；預設密碼僅供本機測試。 |
| `DB_SSLMODE` | `disable` | PostgreSQL TLS 模式；本機預設為 `disable`，使用 Neon 時必須設為 `require`。 |
| `REDIS_HOST`、`REDIS_PORT` | `localhost`、`6379` | Redis 主機與連接埠；Compose 內固定為 `redis:6379`。 |
| `REDIS_PASSWORD` | 空 | Redis 密碼；在 Compose 中會同時啟用 Redis 認證並供後端使用，預設留空。 |
| `JWT_SECRET` | 空（必填） | 簽署及驗證登入 JWT 的密鑰。部署時設定唯一的高熵值，至少 32 位元組。 |
| `JWT_TTL` | `15m` | 登入 JWT 的有效時間。 |
| `JWT_COOKIE_NAME` | `tradeguard_session` | 登入 Cookie 名稱。 |
| `COOKIE_SECURE` | `true` | 為 `true` 時，登入 Cookie 僅透過 HTTPS 傳送；本機純 HTTP 開發請啟用 `SPRING_PROFILES_ACTIVE=local`。 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 允許呼叫 API 的前端來源網址，需包含協定，例如 `https://example.com`。 |
| `EOD_TOKEN` | 空 | 日終作業 API 的共享憑證；呼叫 `/internal/eod` 時放在 `X-EOD-Token` 標頭。 |
| `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD` | 空 | 首次啟動時建立初始 `ADMIN` 帳號；如需建立，兩者都要設定。已存在的同名帳號不會重複建立。 |
| `PORT` | `8080` | 後端 HTTP 連接埠。 |
| `SPRING_PROFILES_ACTIVE` | 無 | 本機開發請設為 `local`。 |

## 啟動前端

請確認後端正在執行，接著（PowerShell）：

```
cd frontend-next
Copy-Item .env.example .env.local
npm install
npm run dev
```

前端環境變數與頁面說明見 [前端說明](./frontend.md)。
