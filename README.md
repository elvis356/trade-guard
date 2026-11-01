# TradeGuard

證券交易模擬器：限價撮合、市場監控、防竄改稽核鏈。**僅供教育用途**，使用虛構資料，非真實交易系統，也不構成投資建議。

[![Next.js](https://img.shields.io/badge/Next.js-15.5-black.svg)](https://nextjs.org/) [![MUI](https://img.shields.io/badge/MUI-9.5-blue.svg)](https://mui.com/) [![Vercel](https://img.shields.io/badge/Vercel-Deployment-black.svg)](https://vercel.com/) [![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot) [![Render](https://img.shields.io/badge/Render-Hosting-46E3B7.svg)](https://render.com/) [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/) [![Neon](https://img.shields.io/badge/Neon-PostgreSQL-00E599.svg)](https://neon.tech/) [![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

## 線上測試

> Render 免費方案閒置 15 分鐘會休眠，首次連線可能需要約 1 分鐘喚醒。

1. 開啟[前端](https://trade-guard-frontend-next.vercel.app)（按住 Ctrl 或 Mac 上的 ⌘ 鍵再點連結，可在新分頁開啟）。首次載入會自動等待 API、PostgreSQL 與 Redis 就緒，不必先手動喚醒後端；服務準備期間會顯示檢查狀態並每 3 秒重試。
2. 到 `/register` 建立 `TRADER` 帳號（帳號 3–50 字元；密碼 12–72 字元）。
3. 到 `/trade` 下限價單，回到市場總覽與標的頁查看報價、委託簿與成交。
4. 若市場總覽沒有報價，需由 `ADMIN` 先到 `/admin` 建立並啟用商品。
5. 想體驗監控／稽核：由 `ADMIN` 建立 `SURVEILLANCE`、`AUDITOR` 帳號後登入，見下表。

| 角色 | 前端路徑 | 可做什麼 |
|------|----------|----------|
| `TRADER` | `/trade` | 下單、查詢自己的委託、取消未成交委託 |
| `SURVEILLANCE` | `/surveillance` | 看警示、提報案件、審核他人提報的案件 |
| `AUDITOR` | `/audit`、`/reports/daily` | 驗證稽核鏈、帳本對帳、檢視每日 XML 報表 |
| `ADMIN` | `/admin` | 管理商品與帳號，並擁有稽核權限 |

完整頁面權限請見 [前端說明](docs/frontend.md)。

## 本地端啟動

需求：Docker Desktop、Node.js 20.9+（JDK 21 僅「不用容器」或跑測試時需要）。

```bash
# 1. 準備環境變數
cp .env.example .env
#    編輯 .env：設定 JWT_SECRET（至少 32 bytes）、COOKIE_SECURE=false、
#    BOOTSTRAP_ADMIN_USERNAME / BOOTSTRAP_ADMIN_PASSWORD

# 2. 啟動 PostgreSQL + Redis + 後端
docker compose up --build -d

# 3. 驗證後端
curl http://localhost:8080/api/health

# 4. 啟動前端
cd frontend-next
cp .env.example .env.local
npm install
npm run dev          # http://localhost:3000
```

首次啟動成功、管理員建立後，請從 `.env` 移除 `BOOTSTRAP_ADMIN_USERNAME` 與 `BOOTSTRAP_ADMIN_PASSWORD`。
Windows PowerShell、不使用 Docker 的啟動方式與全部環境變數，見 [本機開發](docs/local-development.md)。

## 驗證

```bash
cd backend-java
./mvnw test          # Windows: .\mvnw.cmd test
```

測試使用 H2（PostgreSQL 相容模式），不需 Docker；也涵蓋 Redis 中斷時的 PostgreSQL 備援路徑。CI 內容見 [測試與 CI](docs/testing-and-ci.md)。

## 詳細文件

| 文件 | 內容 |
|------|------|
| [docs/functional-requirements-specification.md](https://github.com/elvis356/trade-guard/blob/main/docs/functional-requirements-specification.md) | 功能需求規格 |
| [https://elvis356.github.io/trade-guard/uml-workbench.html](https://elvis356.github.io/trade-guard/uml-workbench.html) | 系統架構與設計圖 |
| [https://trade-guard-frontend-next.vercel.app](https://trade-guard-frontend-next.vercel.app/) | 前端 |
| [https://tradeguard-api-e32l.onrender.com/api/health](https://tradeguard-api-e32l.onrender.com/api/health) | 後端服務狀態頁（人工檢查） |
| [https://tradeguard-api-e32l.onrender.com/api/health/readiness](https://tradeguard-api-e32l.onrender.com/api/health/readiness) | 後端依賴就緒檢查頁（人工檢查、自動更新；API、PostgreSQL、Redis） |
| [docs/local-development.md](docs/local-development.md) | 本機啟動、不使用容器、環境變數表、Neon 設定 |
| [docs/api.md](docs/api.md) | 驗證、市場／委託、監控案件、稽核與每日報告 API |
| [docs/frontend.md](docs/frontend.md) | 前端頁面、角色權限、環境變數、報表快取策略 |
| [docs/deployment.md](docs/deployment.md) | Render、Vercel、GitHub Actions 日終排程 |
| [docs/testing-and-ci.md](docs/testing-and-ci.md) | 測試方式、CI 與安全檢查 |
| [docs/limitations.md](docs/limitations.md) | 目前限制與後續工作 |
| [docs/functional-requirements-specification.md](docs/functional-requirements-specification.md) | 功能需求、角色權限、業務規則 |
| [docs/uml-workbench.html](docs/uml-workbench.html) | 互動式 UML 工作台（[線上檢視](https://elvis356.github.io/trade-guard/uml-workbench.html)） |

> 提示：在 GitHub 上，按住 `Ctrl`（Mac 上按 `⌘`）再點連結，可在新分頁開啟。就緒檢查連結會顯示符合前端主題的狀態頁並每 3 秒更新；前端則以 `Accept: application/json` 取得相同端點的機器可讀狀態。

## 專案結構

```
backend-java/     Spring Boot REST API、Flyway、撮合與稽核
frontend-next/    Next.js App Router 前端
docs/             詳細文件與 UML 工作台
render.yaml       Render Blueprint
docker-compose.yml  本機 PostgreSQL 16 + Redis 7 + 後端
```

## 核心功能

- **撮合**：價格／時間優先、部分成交、1,000 股整股單位、台北平日交易時段驗證。
- **事件重播**：啟動時重建未結委託；序號與資料庫不一致時拒絕啟動。
- **監控**：同帳戶洗售、價格／成交量警示；案件提報與審核須由不同人執行。
- **稽核**：SHA-256 鏈式稽核紀錄，與業務寫入同一筆交易；可驗證鏈與三方帳本對帳。
- **日終報告**：每日 XML 報表，由 GitHub Actions 排程產生。
