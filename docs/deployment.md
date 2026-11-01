# 部署

← [回 README](../README.md)

| 服務 | 網址 |
|------|------|
| 前端（Vercel） | https://trade-guard-frontend-next.vercel.app |
| 後端（Render） | https://tradeguard-api-e32l.onrender.com |
| 健康檢查 | https://tradeguard-api-e32l.onrender.com/api/health |

## Render

Render Blueprint（`render.yaml`）將 Redis 和 Docker 化 API 設為免費方案；PostgreSQL 使用外部 Neon 資料庫，日終排程由 GitHub Actions 執行，不建立 Render Cron Job。

**免費方案限制**（不適合正式環境）：

- 免費 Redis 重啟時資料會清空。
- 免費 Web Service 閒置 15 分鐘後會休眠，下次請求可能需要約一分鐘喚醒。
- Neon 免費額度與暫停規則依 Neon 方案為準。

設定重點：

- 將 `CORS_ALLOWED_ORIGINS` 設為 Vercel 部署來源的精確網址。
- 初次佈建時，請提供 `BOOTSTRAP_ADMIN_USERNAME` 及唯一的 `BOOTSTRAP_ADMIN_PASSWORD`；管理員建立後請移除這兩項設定。
- Render 會產生 `JWT_SECRET` 和 `EOD_TOKEN`；請將 `EOD_TOKEN` 安全地複製到 GitHub Actions secret（見下方）。
- 資料庫連線欄位（Neon）請見 [本機開發：使用 Neon PostgreSQL](local-development.md#使用-neon-postgresql)。

## Vercel

- 專案名稱：`trade-guard-frontend-next`
- Root Directory：`frontend-next`；Framework：Next.js
- Production 環境變數 `BACKEND_URL` 設為 `https://tradeguard-api-e32l.onrender.com`，由 Next.js rewrite 將瀏覽器的同源 `/api/*` 請求轉送至後端。
- Render 的 `CORS_ALLOWED_ORIGINS` 應設為 `https://trade-guard-frontend-next.vercel.app`（只含 scheme 與 host，不含路徑或結尾斜線）。
- 目前沒有後端服務呼叫前端重新驗證路由，因此不需要設定 `REVALIDATE_SECRET`；未來新增受信任呼叫端時，再於 Vercel 與呼叫端設定相同的高熵密鑰。
- CI 不會提交任何憑證，也不會執行外部部署。

## GitHub Actions 日終排程

`Daily EOD` workflow 會在平日 06:00 UTC（台灣時間 14:00）呼叫 `POST /internal/eod`，也可從 Actions 頁面手動執行並指定日期。請在 GitHub 儲存庫 **Settings → Secrets and variables → Actions** 設定：

- Repository variable `EOD_API_URL`：後端 Render base URL，目前為 `https://tradeguard-api-e32l.onrender.com`。
- Repository secret `EOD_TOKEN`：從 Render 後端服務環境變數安全複製的同一個 token。

帳本核對成功時，工作會將 XML 報告存入 PostgreSQL 並回傳其 SHA-256 校驗和；核對失敗時不會發布報告，workflow 也會顯示失敗。若之前已在 Render 建立付費 EOD Cron Job，確認 GitHub Actions 執行成功後，請在 Render 刪除該 Cron Job，避免繼續計費。

## 線上檢視 UML 工作台（GitHub Pages）

`docs/uml-workbench.html` 無法在 GitHub 檔案頁直接渲染。啟用 GitHub Pages 後即可線上互動檢視：

1. 儲存庫 **Settings → Pages**。
2. Source 選 **Deploy from a branch**，Branch 選 `main`，資料夾選 `/docs`，儲存。
3. 約 1 分鐘後可開啟：<https://elvis356.github.io/trade-guard/uml-workbench.html>

免設定的替代方式：<https://htmlpreview.github.io/?https://github.com/elvis356/trade-guard/blob/main/docs/uml-workbench.html>
