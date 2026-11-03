# TradeGuard

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot) [![Next.js](https://img.shields.io/badge/Next.js-15.5-black.svg)](https://nextjs.org/) [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/) [![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

證券交易模擬器：限價撮合、市場監控、防竄改稽核鏈。

> ⚠️ 僅供學習用途，非真實交易系統，不構成投資建議。

## 核心功能

- **撮合**：價格／時間優先、部分成交、以 1,000 股為一張、驗證台北平日交易時段。
- **事件重播**：啟動時重播事件並驗證持久化狀態一致性；事件序號與資料庫不一致時拒絕啟動。
- **監控**：偵測同帳戶洗售（`WASH_TRADE`）及尾盤價格拉抬（`CLOSING_MARKUP`）；案件的提報者與審核者不得為同一人。
- **稽核**：SHA-256 鏈式稽核紀錄，與業務資料寫入位於同一個資料庫交易（transaction）中；可驗證鏈的完整性，並對委託、成交、委託事件三方資料進行對帳（核對已成交數量、成交數量與委託序號連續性）。
- **日終報告**：每日 XML 報表，由 GitHub Actions 排程產生。

## 線上測試

前端部署於 Vercel，後端部署於 Render，資料庫使用 Neon PostgreSQL。

> Render 免費方案閒置 15 分鐘會休眠，首次連線約需 1 分鐘喚醒。

1. 開啟[前端](https://trade-guard-frontend-next.vercel.app)（按住 Ctrl 或 Mac 上的 ⌘ 鍵再點連結，可在新分頁開啟）。首頁是公開市場總覽，首次載入會自動等待 API、PostgreSQL 與 Redis 就緒，不必先手動喚醒後端；服務準備期間會顯示檢查狀態，並每 3 秒自動重試。
2. 在市場總覽選取報價卡，可前往標的頁查看委託簿與近期成交。
3. 到[註冊頁面](https://trade-guard-frontend-next.vercel.app/register)建立 `TRADER` 帳號（帳號 3–50 字元；密碼 12–72 字元），登入後前往[交易頁面](https://trade-guard-frontend-next.vercel.app/trade)下限價單。
4. 若市場總覽沒有報價，需由 `ADMIN` 先到[管理頁面](https://trade-guard-frontend-next.vercel.app/admin)建立並啟用商品。
5. 想體驗監控／稽核：由 `ADMIN` 建立 `SURVEILLANCE`、`AUDITOR` 帳號後登入，見下表。

| 角色 | 前端路徑 | 可做什麼 |
|------|----------|----------|
| `TRADER` | [`/trade`](https://trade-guard-frontend-next.vercel.app/trade) | 下單、查詢自己的委託、取消未成交委託 |
| `SURVEILLANCE` | [`/surveillance`](https://trade-guard-frontend-next.vercel.app/surveillance) | 看警示、提報案件、審核他人提報的案件 |
| `AUDITOR` | [`/audit`](https://trade-guard-frontend-next.vercel.app/audit)、[`/reports/daily`](https://trade-guard-frontend-next.vercel.app/reports/daily) | 驗證稽核鏈、三方資料對帳、檢視每日 XML 報表 |
| `ADMIN` | [`/admin`](https://trade-guard-frontend-next.vercel.app/admin) | 管理商品與帳號，並擁有稽核權限 |

各頁面完整權限請見[前端說明](docs/frontend.md)。
依角色操作市場、委託、監控案件與稽核報表的逐步說明，請見[前端使用者操作指南](docs/user-guide.md)。

手動檢查後端狀態：

- [後端依賴服務檢查](https://tradeguard-api-vy1t.onrender.com/api/health/readiness)：檢查後端 API、PostgreSQL、Redis 狀態，頁面每 3 秒自動更新。

## 本機啟動

需求：Docker Desktop、Node.js 20.9+（JDK 21 僅在不使用 Docker 或執行測試時需要）。

若根目錄已有 `.env.local`，請跳過複製指令，以免覆蓋既有設定。
根目錄的 `.env.local` 供 Docker Compose 使用；`frontend-next/.env.local` 則是前端設定，兩者不同。
執行 Compose 前，請先啟動 Docker Desktop，等待 Docker Engine 顯示執行中（使用 Linux containers），並在 PowerShell 執行 `docker info` 確認可連線。

```bash
# 1. 準備環境變數
cp .env.example .env.local
#    範例已附本機測試用 ADMIN 帳號；若要更改，請編輯 .env.local 中 BOOTSTRAP_ADMIN_USERNAME /
#    BOOTSTRAP_ADMIN_PASSWORD
#    範例帳密僅供本機測試，不可用於 Render 等可從網際網路存取的環境。

# 2. 啟動 Docker Desktop

# 3. 使用 Docker Compose 啟動 PostgreSQL + Redis + 後端
docker compose --env-file .env.local up --build -d

# 4. 驗證後端（在瀏覽器開啟 http://localhost:8080/api/health/readiness，檢查是否顯示「所有服務正常」）

# 5. 啟動前端
cd frontend-next
cp .env.example .env.local
npm install
npm run dev
# 在瀏覽器開啟 http://localhost:3000，檢視前端畫面
```

Windows PowerShell、不使用 Docker 的啟動方式與全部環境變數，見[本機開發](docs/local-development.md)。

## 驗證

```bash
cd backend-java
./mvnw test          # Windows: .\mvnw.cmd test
```

測試使用 H2（PostgreSQL 相容模式），不需 Docker；也涵蓋 Redis 中斷時的 PostgreSQL 備援路徑。CI 內容見[測試與 CI](docs/testing-and-ci.md)。

## 專案結構

```
backend-java/     Spring Boot REST API、Flyway、撮合與稽核
frontend-next/    Next.js App Router 前端
docs/             詳細文件與 UML 工作台
docker-compose.yml  本機 PostgreSQL 16 + Redis 7 + 後端
```

## 詳細文件

| 文件 | 內容 |
|------|------|
| [docs/user-guide.md](docs/user-guide.md) | 訪客與各角色的前端體驗流程及操作說明 |
| [docs/functional-requirements-specification.md](docs/functional-requirements-specification.md) | 功能需求、角色權限、業務規則 |
| [docs/uml-workbench.html](docs/uml-workbench.html) | 互動式 UML 工作台，系統架構與設計圖（[線上檢視](https://elvis356.github.io/trade-guard/uml-workbench.html)） |
| [docs/local-development.md](docs/local-development.md) | 本機啟動、不使用 Docker、環境變數表 |
| [docs/api.md](docs/api.md) | 驗證、市場／委託、監控案件、稽核與每日報告 API |
| [docs/frontend.md](docs/frontend.md) | 前端頁面、角色權限、環境變數、報表快取策略 |
| [docs/owasp-mapping.md](docs/owasp-mapping.md) | OWASP Top 10:2025 對照、程式位置與目前差距 |
| [docs/nonstop-tandem.md](docs/nonstop-tandem.md) | TradeGuard 與 NonStop Tandem 交易一致性、復原及高可用概念的對照與差距 |
| [docs/deployment.md](docs/deployment.md) | Render、Vercel、GitHub Actions 日終排程 |
| [docs/testing-and-ci.md](docs/testing-and-ci.md) | 測試方式、CI 與安全檢查 |
| [docs/limitations.md](docs/limitations.md) | 目前限制與後續工作 |