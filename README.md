# TradeGuard

TradeGuard 是一款證券交易模擬器，僅供教育用途。系統使用虛構資料，並非真實交易系統，也不構成投資建議。

## 專案內容

### 後端與交易

`backend-java/` 使用 Java 21、Spring Boot REST API、Flyway 管理的 PostgreSQL 結構，並提供標的 CRUD、JWT Cookie 驗證及角色型授權。

限價單依可設定的參考價格區間、示意性升降單位表、1,000 股整股交易單位及台北平日交易時段進行驗證。撮合採價格／時間優先、做市方價格、部分成交及 PostgreSQL 交易機制。委託接受、取消及成交會在同一筆交易中寫入僅附加的應用程式事件紀錄。啟動時系統會重播事件以重建未結委託狀態；若委託或各標的序號計數器與 PostgreSQL 不一致，系統會拒絕啟動。

### 市場監控與稽核

公開報價與委託簿快照使用短效 Redis 快取；Redis 無法使用時會改由 PostgreSQL 提供資料。監控功能會保存同帳戶洗售交易，以及可設定收盤時段的價格／成交量警示。案件提報與審核必須由不同的監控使用者執行。

重要委託、交易及案件操作會在同一筆 PostgreSQL 交易中寫入 SHA-256 鏈式稽核紀錄。稽核員／管理員 API 可驗證紀錄鏈並核對委託、交易及事件帳本；受保護的 Cron 端點會執行每日收盤核對，每日交易資料也可下載為 XML。

### 其他專案檔案

- `docker-compose.yml`：本機 PostgreSQL 16 與 Redis 7 服務；Redis 用於登入鎖定及 JWT 撤銷。
- `HANDOFF.md`：階段估算、目前交付狀態及尚待完成的實作清單。

## 本機開發

### 必要條件

後端需要 JDK 21、Docker Desktop，以及首次執行 Maven Wrapper 時所需的網路連線。

### 使用 Docker Compose 啟動

1. 將 `.env.example` 複製為 `.env`。
2. 設定至少 32 位元組且唯一的 `JWT_SECRET`，並設定一次性管理員初始帳號密碼。若此環境由多人共用，也請更換資料庫密碼。請勿將真實密鑰提交至 Git。
3. 啟動完整本機服務：

   ```powershell
   docker compose up --build -d
   ```

   後端啟動後位於 `http://localhost:8080`。首次成功啟動並建立管理員後，請從 `.env` 移除 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`。

4. 確認服務健康狀態：開啟 `http://localhost:8080/api/health`。

### 不使用容器啟動後端

若要直接在本機執行 Spring Boot，請先設定本機環境變數，再啟動 Maven Wrapper：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Set a unique admin password (at least 12 characters)"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
cd backend-java
.\mvnw.cmd spring-boot:run
```

建立第一個管理員帳號後，請從處理程序環境變數移除 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`。Docker Compose 會讀取 `.env`，但 PowerShell 不會自動載入該檔案；若直接啟動 Spring Boot，請在後端處理程序中設定所需的環境變數。部署時必須設定唯一的 `JWT_SECRET`（至少 32 位元組）及 `COOKIE_SECURE=true`。

### 後端環境變數說明

根目錄的 `.env.example` 列出本機 Docker Compose 使用的後端設定。PostgreSQL 連線變數也與 Render 資料庫欄位一一對應；後端會用它們組成 JDBC 連線網址：

| 變數 | 用途 |
|---|---|
| `DB_HOST` | PostgreSQL Hostname；Compose 容器內使用服務名稱 `postgres`，本機直連預設為 `localhost`。使用 Neon 時填 Neon 提供的 Host。 |
| `DB_PORT` | PostgreSQL port，預設為 `5432`。 |
| `DB_NAME` | PostgreSQL Database，預設為 `tradeguard`；使用 Neon 時填 Neon 提供的 Database。 |
| `DB_USERNAME`、`DB_PASSWORD` | PostgreSQL Username、Password；使用 Neon 時填 Neon 提供的 Role 與密碼。 |
| `DB_SSLMODE` | PostgreSQL TLS 模式；本機 Docker 預設 `disable`，Neon 必須設為 `require`。 |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` | Redis 主機、埠號與密碼；本機未啟用密碼時可留空。 |
| `JWT_SECRET` | 簽署及驗證登入 JWT 的密鑰。部署時設定唯一的高熵值，至少 32 位元組。 |
| `COOKIE_SECURE` | 設為 `true` 時，登入 Cookie 僅透過 HTTPS 傳送；本機純 HTTP 開發可設為 `false`。 |
| `CORS_ALLOWED_ORIGINS` | 允許呼叫 API 的前端來源網址，需包含協定，例如 `https://example.com`。 |
| `EOD_TOKEN` | 日終作業 API 的共享憑證；呼叫 `/internal/eod` 時放在 `X-EOD-Token` 標頭。 |
| `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_PASSWORD` | 首次啟動時建立初始 `ADMIN` 帳號；兩者都要設定，帳號建立後移除。 |

### 使用 Neon PostgreSQL 部署

Render Blueprint 不會建立 Render PostgreSQL；它會提示設定 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。在 Neon 專案的 **Connect** 視窗選擇資料庫與 role，將提供的 Host、Port、Database、Username、Password 分別填入這些變數。Render Blueprint 已將 `DB_SSLMODE` 設為 `require`，以符合 Neon 的 TLS 連線需求。請只在 Render 後端服務的環境變數中設定 Neon 密碼，不要提交到 Git 或放入前端變數。

本機 Docker Compose 仍使用專案內的 PostgreSQL 容器，`DB_SSLMODE=disable`；若要從本機直接連 Neon，則在本機 `.env` 填入 Neon 的連線欄位並將 `DB_SSLMODE=require`。

## API 功能

### 驗證與使用者管理

使用 `POST /api/auth/register` 公開建立交易員帳號，或由管理員建立其他角色；公開註冊固定建立 `TRADER`，不接受角色選擇。帳號需為 3 至 50 個英數字或 `._-`，密碼需為 12 至 72 個字元（UTF-8 編碼最多 72 bytes）。使用 `POST /api/auth/login` 登入；API 會設定短效 HttpOnly、SameSite=Lax Cookie。`POST /api/auth/logout` 會在 Redis 撤銷權杖並清除 Cookie。

以初始管理員身分登入後，可透過 `POST /api/admin/users` 建立特定角色帳戶（`TRADER`、`SURVEILLANCE`、`AUDITOR` 或 `ADMIN`）。管理員可使用 `GET /api/admin/users` 分頁瀏覽帳戶，或以 `GET /api/admin/users/{id}` 取得單一帳戶。

### 市場資料與委託

- 標的 CRUD：`http://localhost:8080/api/market/instruments`
- 市場資訊與近期交易：`GET /api/market/{symbol}/info`、`GET /api/market/{symbol}/trades`
- 市場報價與委託簿快照：`GET /api/market/quotes`、`GET /api/market/{symbol}/book`
- 建立／取消限價單：`POST /api/orders`、`DELETE /api/orders/{id}`
- 個人委託紀錄：`GET /api/orders/mine`

委託請求須包含 UUID 格式的 `client_order_id`、標的代碼、方向（`B`／`S`）、限價及數量。同一交易者重複提交相同的用戶端委託 ID 時，會取得原委託；若重用 ID 但委託內容不同，則回傳 `409`。

市場讀取及健康檢查為公開端點；標的資料寫入及使用者管理須具備 `ADMIN` 角色。委託清單支援分頁（`page`、`size`、`sort`；`size` 上限為 100）。建立資源時會回傳 `201 Created` 及 `Location` 標頭。受保護的交易、監控及稽核路由會依 `SecurityConfiguration` 中的角色對應規則限制存取。帶有 `Origin` 標頭的非安全 `/api` 請求，只接受來自 `CORS_ALLOWED_ORIGINS` 的來源。錯誤採用共用 JSON 格式，包含 `timestamp`、`status`、`error`、`message`、`path` 及可選的 `details`。

### 監控與案件審查

監控使用者可透過 `GET /api/alerts` 分頁檢視警示、以 `POST /api/alerts/{id}/case` 提報案件、透過 `GET /api/cases` 列出案件，並以 `POST /api/cases/{id}/review` 提交附註的核准／駁回決定。提報者不能審核自己提報的案件；警示是供人工審查的規則式線索，並非不當行為的定論。

### 稽核與每日報告

稽核員與管理員可使用下列端點：

- `GET /api/audit/verify`：驗證稽核雜湊鏈。
- `GET /api/audit/reconcile`：核對委託、交易及委託事件。
- `GET /api/reports/daily.xml?date=YYYY-MM-DD`：取得台北時區的每日交易報告。

GitHub Actions 的 `Daily EOD` workflow 會在平日 06:00 UTC（台灣時間 14:00）呼叫 `POST /internal/eod`，也可從 Actions 頁面手動執行並指定日期。請在 GitHub 儲存庫 **Settings → Secrets and variables → Actions** 設定：

- Repository variable `EOD_API_URL`：後端 Render base URL，目前為 `https://tradeguard-api-e32l.onrender.com`。
- Repository secret `EOD_TOKEN`：從 Render 後端服務環境變數安全複製的同一個 token。

帳本核對成功時，工作會將 XML 報告存入 PostgreSQL 並回傳其 SHA-256 校驗和；核對失敗時不會發布報告，workflow 也會顯示失敗。若之前已在 Render 建立付費 EOD Cron Job，確認 GitHub Actions 執行成功後，請在 Render 刪除該 Cron Job，避免繼續計費。

## 前端開發

`frontend-next/` App Router 專案提供 SSR 市場總覽（每五秒輪詢報價）、串流式標的頁面（每兩秒更新委託簿）、靜態撮合規則指南、登入／登出與角色導覽，以及角色工作區。登入狀態由後端的 HttpOnly Cookie 維護，瀏覽器操作使用同源 `/api/*` 重寫，不會將 token 暴露給 JavaScript；後端仍是所有受保護 API 的授權最終依據。

| 前端路徑 | 可用角色 | 功能 |
|---|---|---|
| `/login` | 所有使用者 | 登入；可前往公開註冊建立交易員帳號。 |
| `/register` | 所有使用者 | 公開建立 `TRADER` 帳號；其他角色僅能由管理員建立。 |
| `/admin` | `ADMIN` | 建立／編輯／啟用／停用／刪除商品，以及建立角色帳號。 |
| `/trade` | `TRADER` | 建立限價委託、查看自己的委託與取消尚有未成交量的委託。 |
| `/surveillance` | `SURVEILLANCE`、`AUDITOR`、`ADMIN` | 查看警示與案件；監控角色可提報案件、對他人提報的案件核准或駁回。稽核／管理角色唯讀。 |
| `/audit` | `AUDITOR`、`ADMIN` | 執行稽核鏈驗證、三方帳本對帳，並開啟日終報表。 |
| `/reports/daily` | `AUDITOR`、`ADMIN` | 選擇日期、檢視或下載每日 XML 報表。 |

若市場總覽沒有報價，管理員可先登入 `/admin` 建立並啟用商品。每個工作區會呈現載入、空資料及 API 錯誤狀態；導覽依登入角色顯示，且前後端權限檢查並行，不能以隱藏連結取代後端授權。伺服器端後端存取使用僅供伺服器端使用的 `BACKEND_URL`。

使用 Node.js 20.9 或更新版本，並確認後端正在執行；接著在 PowerShell 啟動前端：

```powershell
cd frontend-next
Copy-Item .env.example .env.local
npm install
npm run dev
```

`frontend-next/.env.example` 列出前端環境變數：`BACKEND_URL` 是 Next.js 伺服器端連線後端的網址，不應加上 `NEXT_PUBLIC_`；`REVALIDATE_SECRET` 是前端 `POST /api/revalidate` 路由用來驗證呼叫端的密鑰。目前後端沒有呼叫這個路由，因此現階段不需要在後端設定 `REVALIDATE_SECRET`，也不必為尚未使用的路由設定密鑰。未來若實作受信任的呼叫端（例如後端日終工作），才在前端與該呼叫端設定同一個高熵密鑰，並由呼叫端在 `X-Revalidate-Secret` 標頭傳送；不可將密鑰放進瀏覽器端程式碼。

每日報告包含受角色保護的資料，因此瀏覽器會透過同源 `/api/reports/daily.xml` 即時擷取，並由瀏覽器自動附上 HttpOnly Cookie；不使用共用 ISR 快取，也不在 Next.js 伺服器端轉送或暴露 Cookie。共用快取不得繞過後端授權。

## 測試

不使用 Docker 執行後端整合測試：

```powershell
cd backend-java
.\mvnw.cmd test
```

測試使用 PostgreSQL 相容模式的 H2，並模擬由 Redis 管理的驗證狀態；市場與監控功能在 Redis 中斷時改用 PostgreSQL 的備援路徑也會納入測試。

## CI 與安全檢查

GitHub Actions 會在推送及 Pull Request 上執行後端 `clean verify`、前端 lint／build 及 npm 相依套件稽核。Dependency Review 會阻擋引入高嚴重性相依套件安全公告的 Pull Request。相依套件鎖定檔已提交，因此 CI 使用可重現的前端套件版本。

## 部署

### Render

Render Blueprint（`render.yaml`）將 Redis 和 Docker 化 API 設為免費方案；PostgreSQL 使用外部 Neon 資料庫，日終排程由 GitHub Actions 執行，不建立 Render Cron Job。已部署的 API 網址為 [`https://tradeguard-api-e32l.onrender.com`](https://tradeguard-api-e32l.onrender.com)，健康檢查為 [`/api/health`](https://tradeguard-api-e32l.onrender.com/api/health)。Render 免費方案有重要限制：免費 Redis 重啟時資料會清空，免費 Web Service 閒置 15 分鐘後會休眠且下次請求可能需要約一分鐘喚醒；不適合正式環境。Neon 免費額度與暫停規則依 Neon 方案為準。請將 `CORS_ALLOWED_ORIGINS` 設為 Vercel 部署來源的精確網址。初次佈建時，請提供 `BOOTSTRAP_ADMIN_USERNAME` 及唯一的 `BOOTSTRAP_ADMIN_PASSWORD`；管理員建立後請移除這兩項設定。Render 會產生 `JWT_SECRET` 和 `EOD_TOKEN`；依上方說明將 `EOD_TOKEN` 安全地複製到 GitHub Actions secret。

### Vercel

Vercel 專案名稱為 `trade-guard-frontend-next`，Root Directory 設為 `frontend-next`，Framework 為 Next.js。Production 網址為 [`https://trade-guard-frontend-next.vercel.app`](https://trade-guard-frontend-next.vercel.app)。Production 環境變數 `BACKEND_URL` 設為 `https://tradeguard-api-e32l.onrender.com`，由 Next.js rewrite 將瀏覽器的同源 `/api/*` 請求轉送至後端。Render `CORS_ALLOWED_ORIGINS` 應設為 `https://trade-guard-frontend-next.vercel.app`（只含 scheme 與 host，不含路徑或結尾斜線）。目前沒有後端服務呼叫前端重新驗證路由，因此不需要設定 `REVALIDATE_SECRET`；未來新增受信任呼叫端時，再於 Vercel 與呼叫端設定相同的高熵密鑰。CI 不會提交任何憑證，也不會執行外部部署。

## 目前限制與後續工作

本機開發使用 PostgreSQL、Redis 及 Flyway 遷移；此電腦尚未驗證 PostgreSQL／Redis 執行階段整合。監控成交量／價格規則在實作前仍需要 20 日每日 K 線資料來源；事件與稽核資料表的資料庫層級寫入保護，以及 Redis 冪等性加速仍屬後續強化工作。

前端已提供登入、角色導覽、商品與帳號管理、委託交易、監控案件、稽核及日終報表工作區；尚未完成真實雲端端到端驗證。管理介面目前以第一頁最多 100 筆呈現帳號／商品、委託、警示與案件，完整跨頁瀏覽仍可後續擴充；系統亦尚未提供密碼重設、帳號停用管理或公開註冊。
