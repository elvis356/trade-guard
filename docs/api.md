# API 功能

← [回 README](../README.md)

Base URL：本機 `http://localhost:8080`；線上 `https://tradeguard-api-vy1t.onrender.com`

## 驗證與使用者管理

使用 `POST /api/auth/register` 公開建立交易員帳號，或由管理員建立其他角色；公開註冊固定建立 `TRADER`，不接受角色選擇。帳號需為 3 至 50 個英數字或 `._-`，密碼需為 12 至 72 個字元（UTF-8 編碼最多 72 bytes）。使用 `POST /api/auth/login` 登入；API 會設定短效 HttpOnly、SameSite=Lax Cookie。`POST /api/auth/logout` 會在 Redis 撤銷權杖並清除 Cookie。

以初始管理員身分登入後，可透過 `POST /api/admin/users` 建立特定角色帳戶（`TRADER`、`SURVEILLANCE`、`AUDITOR` 或 `ADMIN`）。管理員可使用 `GET /api/admin/users` 分頁瀏覽帳戶，或以 `GET /api/admin/users/{id}` 取得單一帳戶。

`GET /api/auth/me` 需登入，回傳目前帳號與角色，供前端初始化登入狀態。

## 市場資料與委託

- 標的 CRUD：`/api/market/instruments`
- 市場資訊與近期交易：`GET /api/market/{symbol}/info`、`GET /api/market/{symbol}/trades`
- 市場報價與委託簿快照：`GET /api/market/quotes`、`GET /api/market/{symbol}/book`
- 建立／取消限價單：`POST /api/orders`、`DELETE /api/orders/{id}`
- 個人委託紀錄：`GET /api/orders/mine`

委託請求須包含 UUID 格式的 `client_order_id`、標的代碼、方向（`B`／`S`）、限價及數量。同一交易者重複提交相同的用戶端委託 ID 時，會取得原委託；若重用 ID 但委託內容不同，則回傳 `409`。

市場讀取及健康檢查為公開端點；標的資料寫入及使用者管理須具備 `ADMIN` 角色。委託清單支援分頁（`page`、`size`、`sort`；`size` 上限為 100）。受保護的交易、監控及稽核路由會依 `SecurityConfiguration` 中的角色對應規則限制存取。帶有 `Origin` 標頭的非安全 `/api` 請求，只接受來自 `CORS_ALLOWED_ORIGINS` 的來源。錯誤採用共用 JSON 格式，包含 `timestamp`、`status`、`error`、`message`、`path` 及可選的 `details`。

### 成功回應狀態碼

| 端點 | 成功狀態碼與標頭 |
|------|------------------|
| `POST /api/auth/register` | `201 Created`；不回傳 `Location`。 |
| `POST /api/auth/login` | `200 OK`；設定登入 Cookie。 |
| `POST /api/auth/logout` | `204 No Content`；清除登入 Cookie。 |
| `GET /api/auth/me` | `200 OK`。 |
| `POST /api/admin/users`、`POST /api/market/instruments` | `201 Created`，附 `Location`。 |
| `POST /api/orders` | `201 Created`，附 `Location`。 |
| `POST /api/alerts/{id}/case` | `200 OK`。 |
| 其他成功的查詢、更新、刪除及案件覆核 | 通常為 `200 OK`。 |

`GET /api/audit/verify` 與 `GET /api/audit/reconcile` 在驗證／對帳成功時回 `200 OK`，結果無效或不平衡時回 `409 Conflict`。`POST /internal/eod` 成功時回 `200 OK`、對帳未完成時回 `409 Conflict`，缺少或錯誤的 `X-EOD-Token` 回 `401 Unauthorized`。驗證失敗、未授權、資源不存在及衝突等錯誤會依情況回 `400`、`401`／`403`、`404` 或 `409`。

## 監控與案件審查

監控使用者可透過 `GET /api/alerts` 分頁檢視警示、以 `POST /api/alerts/{id}/case` 提報案件、透過 `GET /api/cases` 列出案件，並以 `POST /api/cases/{id}/review` 提交附註的核准／駁回決定。提報者不能審核自己提報的案件；警示是供人工審查的規則式線索，並非不當行為的定論。

## 稽核與每日報告

稽核員與管理員可使用下列端點：

- `GET /api/audit/verify`：驗證稽核雜湊鏈。
- `GET /api/audit/reconcile`：核對委託、交易及委託事件。
- `GET /api/reports/daily.xml?date=YYYY-MM-DD`：取得台北時區的每日交易報告。

日終排程（`POST /internal/eod`，以 `X-EOD-Token` 驗證）的設定見 [部署說明](deployment.md#github-actions-日終排程)。帳本核對成功時，工作會將 XML 報告存入 PostgreSQL 並回傳其 SHA-256 校驗和；核對失敗時不會發布報告。

## 健康檢查

| 端點 | 用途 | 回應 |
|------|------|------|
| `GET /api/health` | 人類檢視的基本健康頁面。 | `200 OK` HTML。 |
| `GET /api/health/status` | 僅檢查 API 程序，供機器監控。 | `200 OK` JSON，例如 `{"status":"UP"}`。 |
| `GET /api/health/readiness` | 檢查 API、PostgreSQL 與 Redis 是否就緒。 | `Accept: text/html` 時回 `200 OK` HTML 頁面；`Accept: application/json` 時依依賴狀態回 `200 OK` 或 `503 Service Unavailable` JSON。 |
