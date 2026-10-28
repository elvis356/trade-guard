# TradeGuard — 代處理與後續工作

更新時間：2026-10-28（本機時間，Asia/Taipei）

## 今日交付目標與預估

目標是在今天 16:00 前完成一個可驗證階段。階段 1–6 後端已完成；階段 7 前端已建立首個可執行垂直切片，尚未完成整階段。

| 階段 | 範圍 | 預估投入 | 狀態 |
|---|---|---:|---|
| 1. 後端骨架與商品主檔 CRUD | Java 21 / Spring Boot、Postgres、Flyway、分頁 CRUD、DTO 驗證、統一例外處理、整合測試 | 3–4 小時 | 已完成；H2 整合測試 4 項全數通過 |
| 2. 認證、RBAC 與安全基線 | 使用者/角色、登入登出、密碼雜湊、JWT Cookie、授權、CORS/Security Headers | 6–10 小時 | 已完成程式與 H2 測試；待真實 Redis/Postgres 整合驗證 |
| 3. 委託、驗證與撮合 | 訂單 CRUD/取消、規則驗證、價格/時間優先、部分成交、成交紀錄 | 12–18 小時 | 已完成程式與 H2 測試；待真實 Postgres 驗證 |
| 4. 事件日誌、冪等與重播 | Postgres 序號、append-only order events、冪等鍵、委託簿重建 | 8–12 小時 | 已完成程式與 H2 測試；待真實 Postgres 驗證 |
| 5. 行情、Redis 與市場監視 | 行情/委託簿查詢、快取、滑動視窗、異常規則、警示與案件雙人覆核 | 8–12 小時 | 已完成程式與 H2 測試；待真實 Redis/Postgres 驗證 |
| 6. 稽核與日終作業 | 雜湊鏈、驗證 API、三方對帳、Cron 入口、XML 日報表 | 6–9 小時 | 已完成程式與 H2 測試；待真實 Postgres 驗證 |
| 7. Next.js 前端與 API 串接 | 導覽、首頁、行情、下單、警示/案件/稽核頁、SSR/SSG/串流策略 | 12–20 小時 | 進行中：SSR/SSG/串流行情切片已驗證；登入及操作頁待完成 |
| 8. 整合、資安掃描與部署 | Docker、CI、OWASP 檢核、測試、Render/Vercel 設定與部署 | 6–10 小時 | 未完成 |
| 選配擴充 | Python 模擬資料/RAG、Ollama、C# 對帳工具、Roadmap 功能 | 8–16 小時 | 未開始 |

核心功能（階段 1–8）粗估約 **61–95 小時**；選配項目另加約 **8–16 小時**。估時以單人熟悉 Java/Spring/React、需求不變、依賴與部署服務可用為前提；不包含等候雲端審核、網路下載及需求變更。

## 本次已建立

- `backend-java/`：Maven Spring Boot 3 專案，採 package-by-feature。
- 商品主檔 `Instrument`：新增、分頁查詢、單筆查詢、整筆修改、部分修改、刪除；另提供 TradeGuard 計畫中的 `GET /api/market/{symbol}/info`。
- DTO 與 Bean Validation；Entity 不直接作為 HTTP request/response。
- Controller → Service → Repository 分層；新增回 `201` 與 `Location`，找不到 `404`、重複 `409`、驗證錯誤 `400`，統一錯誤 JSON。
- 認證與權限：BCrypt 密碼雜湊、短效 HS256 JWT（HttpOnly/SameSite Cookie）、安全登出 token 撤銷、Redis 登入失敗鎖定、CORS 白名單、unsafe Origin 驗證及安全回應標頭。
- RBAC：`TRADER`、`SURVEILLANCE`、`AUDITOR`、`ADMIN`，管理者透過 `/api/admin/users` 建立帳號；首位管理員以一次性環境變數建立，不提供弱預設帳密。
- 委託與撮合：限價買賣單新增、本人委託分頁查詢、取消及公開成交查詢；使用 Postgres 交易與每商品資料列鎖序列化併發委託，先鎖商品資料列再分配單調遞增 `seq_no`。
- 驗證漲跌幅、升降單位、交易單位與臺北時區週一至週五交易時間；以價格優先、`seq_no` 時間優先、多檔成交、先到委託價成交並保留部分成交量。
- 以 `(user_id, client_order_id)` 唯一約束與相同 payload 重送回原結果，payload 不一致回 `409`；Redis 冪等快速路徑尚未加入。
- 階段 4：新增 V4 migration，將既有委託/成交狀態回填至 `order_events`，並以 `instruments.last_seq_no` 作每商品持久序號；在既有商品列鎖及同一交易內分配序號。
- 新委託接受、成交、取消事件依序在委託/成交狀態變更前寫入同一資料庫交易；事件 repository 僅提供新增與讀取操作。
- 啟動後依事件 ID 重播，重建開放委託快照並比對 `orders` 投影和序號計數；事件遺漏、投影差異或序號不一致時啟動失敗，不以成功狀態掩蓋資料不一致。
- 既有冪等唯一約束與重送回應保持有效；Redis `SET NX EX` 快速路徑尚未實作。事件表的不可變限制目前由應用 repository 介面維持，尚未加上 PostgreSQL 權限/觸發器層級的防竄改保護。
- Flyway V1–V8 migrations；本機 Postgres 16 與 Redis 7 Compose 設定。
- H2 整合測試涵蓋 CRUD、分頁、驗證、重複代碼、錯誤格式及無效排序。
- `README.md`、`.env.example` 與本文件。
- 後端 23 項 H2 測試全數通過（4 CRUD、7 認證/RBAC、7 委託/事件/行情、2 規則驗證、1 監視與案件 API、2 稽核/EOD）；`clean verify` 成功並產生 JAR。
- 階段 5：新增公開 `GET /api/market/quotes` 與 `GET /api/market/{symbol}/book`，行情與委託簿使用 15 秒 Redis 快取；Redis 不可用或快取 JSON 損壞時記錄警告並回源 Postgres。
- 新增 Redis Sorted Set 成交滑動視窗，Redis 不可用時以已持久化成交回源計算；成交提交後執行監視，啟動時掃描歷史成交補處理，警示寫入 Postgres 並以成交/規則唯一鍵避免重複。
- 實作 `WASH_TRADE`（同帳戶買賣）及可設定門檻的 `CLOSING_MARKUP`（尾盤時窗、參考價偏離、單一買方數量）警示；警示僅是線索，不代表違規。
- 新增警示與案件分頁 API、提案/覆核 API；建立案件需監視角色，並以資料列鎖及不同帳號檢查實施四眼原則。
- 測試覆蓋 Redis 不可用時行情/委託簿回源，以及警示權限、提案、同人覆核拒絕、異人覆核成功。
- 階段 6：新增 V6 migration 與 SHA-256 稽核鏈；以 singleton chain-head row 的資料列鎖序列化併發 append，並在下單、成交、取消及案件提案/覆核的同一資料庫交易寫入稽核列。雜湊包含前一雜湊、user ID、actor、action、detail 與截斷至微秒的 UTC 時間。
- `GET /api/audit/verify` 驗證逐列 hash、前後連結及 chain head；竄改時回 `409` 並指出首個損壞列。
- `GET /api/audit/reconcile` 比對 orders、trades、order_events 的成交量、訂單接受/取消狀態、成交對手數量及商品序號連續性；不平衡回 `409` 並列出差異。
- `GET /api/reports/daily.xml?date=YYYY-MM-DD` 依 `market.zone` 提供 XML 成交明細，限 AUDITOR/ADMIN；V7 `eod_reports` 以業務日期保存已產生日報表及 SHA-256。
- 新增 `POST /internal/eod?date=YYYY-MM-DD` Cron 入口；未設定或 Token 不符回 `401`，使用 constant-time 比較 `X-EOD-Token`；每次執行均由 V8 `eod_runs` 保存對帳結果與差異，只有三方對帳平衡才生成/保存 XML 並回成功，結果含報表 checksum，且會寫入 EOD 稽核事件。
- 後端 23 項 H2 測試全數通過（4 CRUD、7 認證/RBAC、7 委託/事件/行情、2 規則驗證、1 監視與案件、2 稽核/EOD），包含鏈竄改偵測、對帳差異、XML 權限與 EOD Token 驗證。
- 階段 7 已新增 `frontend-next/` Next.js App Router 專案：首頁 SSR 行情總覽及五秒輪詢、商品頁 SSR 外殼與 Suspense 載入委託簿/成交、兩秒委託簿與五秒成交輪詢、商品基本資料 24 小時資料快取、SSG 撮合規則頁。
- 瀏覽器使用同源 `/api/*` rewrites 連到後端；日終報表伺服器端轉送登入 Cookie，仍由後端執行 AUDITOR/ADMIN 授權，避免共用快取洩漏受保護資料。新增密鑰保護的 `/api/revalidate` handler；報表內容採每請求讀取，不採 ISR，以符合目前後端權限設計。
- 前端使用 Node.js 24.22.1 完成 `npm run lint`、`npm run build`，`npm audit` 為 0 vulnerabilities；production server smoke test 確認首頁無後端時回明確錯誤狀態、靜態規則頁回 `200`、未帶密鑰的 revalidation 回 `401`。尚未完成登入介面與路由保護、下單/我的委託、警示/案件提案覆核、稽核驗證頁、真實端到端測試、Vercel rewrite/revalidation 整合。

## 待處理清單（依序建議）

1. 安裝/啟用 Docker Desktop 後執行 `docker compose up -d postgres redis`，驗證 V1–V8 migrations、V4 舊資料回填、Postgres 撮合/事件投影/稽核鎖、日報表與 EOD 對帳結果儲存，以及真實 Redis 快取、Sorted Set 成交視窗、登入鎖定與 JWT 撤銷。
2. 市場監視尚缺 `VOL_PRICE_ANOMALY`：先決定/建立日線行情來源與 20 日均量資料，再實作量價偏離規則；現有 `CLOSING_MARKUP` 是可調門檻的示意規則，須以測試情境校正，非正式市場監管判定。
3. 正式環境強化 `order_events` 與 `audit_entries` 防竄改：配置最小權限 DB role 或 PostgreSQL trigger/權限，確保應用外部也不能 UPDATE/DELETE；另評估加入 Redis 冪等快速路徑，保留 Postgres 唯一約束為最終保障。
4. 完成階段 7 的登入介面與路由保護、下單/委託、警示/案件、稽核頁及整合測試；日終報表 ISR 需先設計不會跨角色共用受保護資料的快取策略，再接通 EOD webhook。之後完成 CI/OWASP 檢核與 Render/Vercel 部署，正式環境設定獨立 `JWT_SECRET`、`COOKIE_SECURE=true`、精確 `CORS_ALLOWED_ORIGINS`、高熵 `EOD_TOKEN` 並安全管理首位管理員帳密。
5. RAG、XML 擴充、模擬器及 C# 對帳工具可在核心流程完成後依時間選做；RAG 僅本機開啟且不得用於違規判定。

## 尚未驗證的環境限制

開始時本機可找到 JDK 21，但未找到 Maven、Node.js、Docker 或 Gradle 指令。已透過 Maven Wrapper 完成 clean verify；共 23 項 H2 測試全數通過。測試使用 H2 PostgreSQL 相容模式，並 mock Redis-backed auth state；市場快取/滑動視窗測試涵蓋 Redis 不可用時回源 Postgres。真實 Postgres V1–V8 migration、列鎖、事件回填/重播、稽核列鎖、日報表/EOD 對帳結果持久化與 Redis 實際快取/Sorted Set 行為仍需 Docker Desktop 驗證。`VOL_PRICE_ANOMALY` 因缺少 20 日日線資料來源尚未實作；現有監視規則只產生待人工判讀的警示。事件與稽核表目前由應用層 insert-only repository 維持，不具 PostgreSQL 權限/觸發器防竄改保護。EOD endpoint 預設因未設定 `EOD_TOKEN` 而拒絕請求；部署 Cron 前需設定高熵秘密。前端已安裝 Node.js 24.22.1，lint/build、production smoke test 與 npm audit 均通過；登入、交易、監視、稽核等頁面仍未完成。日終報表因後端要求 AUDITOR/ADMIN 授權，目前刻意不啟用共用 ISR 快取；revalidation handler 已建立，待確認安全快取策略及 EOD 通知串接。Wrapper JAR 已納入專案；首次執行需連線下載 Maven 發行版與相依套件。登入設定需至少 32-byte `JWT_SECRET`；local profile 才提供明確限於本機的開發 fallback key。部署不可使用該 local profile/key。

附件規格來源：`TradeGuard.md` 與 `Spring Boot RESTful CRUD Guide.md`。請以這兩份規格及本文件的待處理清單接續實作。
