# TradeGuard — 代處理與後續工作

更新時間：2026-10-28（本機時間，Asia/Taipei）

## 今日交付目標與預估

目標是在今天 16:00 前完成一個可驗證階段。本次已完成前四個後端階段；階段 4 估時約 8–12 小時，包含不可變事件記錄、同交易商品序號配置與啟動重播驗證。

| 階段 | 範圍 | 預估投入 | 狀態 |
|---|---|---:|---|
| 1. 後端骨架與商品主檔 CRUD | Java 21 / Spring Boot、Postgres、Flyway、分頁 CRUD、DTO 驗證、統一例外處理、整合測試 | 3–4 小時 | 已完成；H2 整合測試 4 項全數通過 |
| 2. 認證、RBAC 與安全基線 | 使用者/角色、登入登出、密碼雜湊、JWT Cookie、授權、CORS/Security Headers | 6–10 小時 | 已完成程式與 H2 測試；待真實 Redis/Postgres 整合驗證 |
| 3. 委託、驗證與撮合 | 訂單 CRUD/取消、規則驗證、價格/時間優先、部分成交、成交紀錄 | 12–18 小時 | 已完成程式與 H2 測試；待真實 Postgres 驗證 |
| 4. 事件日誌、冪等與重播 | Postgres 序號、append-only order events、冪等鍵、委託簿重建 | 8–12 小時 | 已完成程式與 H2 測試；待真實 Postgres 驗證 |
| 5. 行情、Redis 與市場監視 | 行情/委託簿查詢、快取、滑動視窗、異常規則、警示與案件雙人覆核 | 8–12 小時 | 未完成 |
| 6. 稽核與日終作業 | 雜湊鏈、驗證 API、三方對帳、Cron 入口、XML 日報表 | 6–9 小時 | 未完成 |
| 7. Next.js 前端與 API 串接 | 導覽、首頁、行情、下單、警示/案件/稽核頁、SSR/SSG/串流策略 | 12–20 小時 | 未完成 |
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
- Flyway V1–V4 migrations；本機 Postgres 16 與 Redis 7 Compose 設定。
- H2 整合測試涵蓋 CRUD、分頁、驗證、重複代碼、錯誤格式及無效排序。
- `README.md`、`.env.example` 與本文件。
- 後端 19 項 H2 測試全數通過（4 CRUD、7 認證/RBAC、6 委託/事件重播、2 規則驗證）；`clean verify` 成功並產生 JAR。

## 待處理清單（依序建議）

1. 安裝/啟用 Docker Desktop 後執行 `docker compose up -d postgres redis`，驗證 V1–V4 migrations（包含既有 V3 委託/成交資料回填）、委託與交易資料庫交易、PESSIMISTIC_WRITE 商品鎖、事件重播、登入失敗計數/TTL 及 JWT logout 黑名單的實際整合。
2. 正式環境強化 `order_events` 防竄改：配置最小權限 DB role 或 PostgreSQL trigger/權限，確保應用外部也不能 UPDATE/DELETE；另評估加入 Redis 冪等快速路徑，保留 Postgres 唯一約束為最終保障。
3. 接著完成 Redis 行情/委託簿快取、監視警示與案件雙人覆核、稽核雜湊鏈及日終對帳/XML 報表。
4. 前端與後端 API 契約穩定後完成 Next.js 頁面，最後補 CI、OWASP 檢核與 Render/Vercel 部署。正式環境需設定獨立 `JWT_SECRET`、`COOKIE_SECURE=true`、精確 `CORS_ALLOWED_ORIGINS`，並安全管理首位管理員帳密。
5. RAG、XML 擴充、模擬器及 C# 對帳工具可在核心流程完成後依時間選做；RAG 僅本機開啟且不得用於違規判定。

## 尚未驗證的環境限制

開始時本機可找到 JDK 21，但未找到 Maven、Node.js、Docker 或 Gradle 指令。已透過 Maven Wrapper 完成 clean verify 與打包；CRUD、認證、委託及事件重播共 19 項 H2 測試全數通過。測試使用 H2 相容模式，並 mock Redis-backed auth state；真實 Postgres V1–V4 migration、列鎖、事件回填/重播及 Redis 整合仍需安裝/啟用 Docker Desktop 驗證。序號存在鎖定的 `instruments.last_seq_no` 欄位，與委託/事件同一交易提交；事件投影不一致會阻止啟動。事件表目前由應用層 insert-only repository 維持，不具 PostgreSQL 權限/觸發器防竄改保護。Wrapper JAR 已納入專案；首次執行需連線下載 Maven 發行版與相依套件。登入設定需至少 32-byte `JWT_SECRET`；local profile 才提供明確限於本機的開發 fallback key。部署不可使用該 local profile/key。

附件規格來源：`TradeGuard.md` 與 `Spring Boot RESTful CRUD Guide.md`。請以這兩份規格及本文件的待處理清單接續實作。
