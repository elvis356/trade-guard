# 本機開發

← [回 README](../README.md)

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
- `docs/functional-requirements-specification.md`：依目前程式實作整理的功能需求、角色權限、業務規則與已知限制。
- `docs/uml-workbench.html`：互動式 UML 工作台；圖表資料集中於 HTML 的 `uml-data` 區塊，含架構、後端模組、使用案例、活動、時序、類別、ERD、狀態與部署／CI 流程圖，支援跨圖追溯、跨圖搜尋與清單檢視。修改資料後執行 `node docs/uml-workbench.check.js` 驗證。線上檢視：<https://elvis356.github.io/trade-guard/uml-workbench.html>

## 必要條件

後端需要 JDK 21、Docker Desktop，以及首次執行 Maven Wrapper 時所需的網路連線。前端需要 Node.js 20.9 或更新版本。

## 使用 Docker Compose 啟動

1. 將 `.env.example` 複製為根目錄的 `.env.local`。若檔案已存在，不要重複複製，以免覆蓋原有設定。此範例含本機專用設定與 JWT 值，不可用於共用或部署環境。
2. 如需建立初始管理員，設定 `BOOTSTRAP_ADMIN_USERNAME` 與 `BOOTSTRAP_ADMIN_PASSWORD`；若此環境由多人共用，也請更換資料庫密碼。請勿將真實密鑰提交至 Git。
3. 啟動完整本機服務：

   ```
   docker compose --env-file .env.local up --build -d
   ```

   後端啟動後位於 `http://localhost:8080`。首次成功啟動並建立管理員後，請從 `.env.local` 移除 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`。
4. 確認服務狀態：開啟 `http://localhost:8080/api/health` 查看品牌提示頁；Render 與自動化監控使用 `http://localhost:8080/api/health/status`（JSON）。

## 不使用容器啟動後端

若要直接在本機執行 Spring Boot，請先設定本機環境變數，再啟動 Maven Wrapper（PowerShell）：

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Set a unique admin password (at least 12 characters)"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
cd backend-java
.\mvnw.cmd spring-boot:run
```

建立第一個管理員帳號後，請從處理程序環境變數移除 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`。Docker Compose 會讀取 `.env`，但 PowerShell 不會自動載入該檔案；若直接啟動 Spring Boot，請在後端處理程序中設定所需的環境變數。部署時必須設定唯一的 `JWT_SECRET`（至少 32 位元組）及 `COOKIE_SECURE=true`。

## 後端環境變數說明

根目錄的 `.env.example` 列出本機 Docker Compose 使用的後端設定。PostgreSQL 連線變數也與 Render 資料庫欄位一一對應；後端會用它們組成 JDBC 連線網址：

| 變數 | 用途 |
|------|------|
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

## 使用 Neon PostgreSQL

Render Blueprint 不會建立 Render PostgreSQL；它會提示設定 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。在 Neon 專案的 **Connect** 視窗選擇資料庫與 role，將提供的 Host、Port、Database、Username、Password 分別填入這些變數。Render Blueprint 已將 `DB_SSLMODE` 設為 `require`，以符合 Neon 的 TLS 連線需求。請只在 Render 後端服務的環境變數中設定 Neon 密碼，不要提交到 Git 或放入前端變數。

本機 Docker Compose 仍使用專案內的 PostgreSQL 容器，`DB_SSLMODE=disable`；若要從本機直接連 Neon，則在根目錄 `.env.local` 填入 Neon 的連線欄位並將 `DB_SSLMODE=require`。

## 啟動前端

請確認後端正在執行，接著（PowerShell）：

```powershell
cd frontend-next
Copy-Item .env.example .env.local
npm install
npm run dev
```

前端環境變數與頁面說明見 [前端說明](frontend.md)。
