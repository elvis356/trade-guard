# TradeGuard — 代處理與後續工作

更新時間：2026-10-28（本機時間，Asia/Taipei）

## 今日交付目標與預估

目標是在今天 16:00 前完成一個可驗證階段。本次先交付「後端基礎骨架＋商品主檔 REST CRUD」；估時約 3–4 小時，包含專案初始化、資料庫 migration、API 分層、全域錯誤格式與自動化整合測試。

| 階段 | 範圍 | 預估投入 | 狀態 |
|---|---|---:|---|
| 1. 後端骨架與商品主檔 CRUD | Java 21 / Spring Boot、Postgres、Flyway、分頁 CRUD、DTO 驗證、統一例外處理、整合測試 | 3–4 小時 | 已完成；H2 整合測試 4 項全數通過 |
| 2. 認證、RBAC 與安全基線 | 使用者/角色、登入登出、密碼雜湊、JWT Cookie、授權、CORS/Security Headers | 6–10 小時 | 未完成 |
| 3. 委託、驗證與撮合 | 訂單 CRUD/取消、規則驗證、價格/時間優先、部分成交、成交紀錄 | 12–18 小時 | 未完成 |
| 4. 事件日誌、冪等與重播 | Postgres 序號、append-only order events、冪等鍵、委託簿重建 | 8–12 小時 | 未完成 |
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
- Flyway V1 migration；本機 Postgres 16 與 Redis 7 Compose 設定。Redis 目前只預備、不由後端使用。
- H2 整合測試涵蓋 CRUD、分頁、驗證、重複代碼、錯誤格式及無效排序。
- `README.md` 與 `.env.example`。

## 待處理清單（依序建議）

1. 在具備 Docker Desktop/Compose 的環境啟動 `docker compose up -d postgres redis`，以 PostgreSQL 實際驗證 migration 與應用啟動；確認後端測試 `.\mvnw.cmd test` 通過。
2. 先完成階段 2：新增認證與 RBAC。現階段 CRUD API 沒有登入保護，僅允許本機開發，勿部署或公開。
3. 按 TradeGuard 的業務順序實作訂單驗證、撮合、成交與事件日誌；撮合持久化、序號分配及冪等必須使用 Postgres 作為最終事實來源，不可依賴 Redis 保證正確性。
4. 再做重播恢復、快取/行情、監視警示與案件覆核、稽核雜湊鏈及日終報表。
5. 前端與後端 API 契約穩定後再完成 Next.js 頁面，最後補 CI、OWASP 檢核與 Render/Vercel 部署。
6. RAG、XML 擴充、模擬器及 C# 對帳工具可在核心流程完成後依時間選做；RAG 僅本機開啟且不得用於違規判定。

## 尚未驗證的環境限制

開始時本機可找到 JDK 21，但未找到 Maven、Node.js、Docker 或 Gradle 指令。已透過 Maven Wrapper 完成 `mvnw.cmd test`，4 項 H2 整合測試全數通過；Wrapper JAR 已納入專案，首次執行仍需連線下載 Maven 發行版與相依套件。PostgreSQL 真實資料庫啟動驗證仍需先安裝/啟用 Docker Desktop。若後續 wrapper 或依賴下載受網路限制，應先恢復網路或安裝 Maven。

附件規格來源：`TradeGuard.md` 與 `Spring Boot RESTful CRUD Guide.md`。請以這兩份規格及本文件的待處理清單接續實作。
