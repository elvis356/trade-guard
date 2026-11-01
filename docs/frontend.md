# 前端說明

← [回 README](../README.md)

`frontend-next/` App Router 專案提供 SSR 市場總覽（每五秒輪詢報價）、標的頁面（每兩秒更新委託簿）、靜態撮合規則指南、登入／登出與角色導覽，以及角色工作區。登入狀態由後端的 HttpOnly Cookie 維護，瀏覽器操作使用同源 `/api/*` 重寫，不會將 token 暴露給 JavaScript；後端仍是所有受保護 API 的授權最終依據。

## 頁面與角色

| 前端路徑 | 可用角色 | 功能 |
|----------|----------|------|
| `/login` | 所有使用者 | 登入；可前往公開註冊建立交易員帳號。 |
| `/register` | 所有使用者 | 公開建立 `TRADER` 帳號；其他角色僅能由管理員建立。 |
| `/admin` | `ADMIN` | 建立／編輯／啟用／停用／刪除商品，以及建立角色帳號。 |
| `/trade` | `TRADER` | 建立限價委託、查看自己的委託與取消尚有未成交量的委託。 |
| `/surveillance` | `SURVEILLANCE`、`AUDITOR`、`ADMIN` | 查看警示與案件；監控角色可提報案件、對他人提報的案件核准或駁回。稽核／管理角色唯讀。 |
| `/audit` | `AUDITOR`、`ADMIN` | 執行稽核鏈驗證、三方帳本對帳，並開啟日終報表。 |
| `/reports/daily` | `AUDITOR`、`ADMIN` | 選擇日期、檢視或下載每日 XML 報表。 |

若市場總覽沒有報價，管理員可先登入 `/admin` 建立並啟用商品。每個工作區會呈現載入、空資料及 API 錯誤狀態；導覽依登入角色顯示，且前後端權限檢查並行，不能以隱藏連結取代後端授權。伺服器端後端存取使用僅供伺服器端使用的 `BACKEND_URL`。

## 本機啟動

使用 Node.js 20.9 或更新版本，並確認後端正在執行；接著（PowerShell）：

```powershell
cd frontend-next
Copy-Item .env.example .env.local
npm install
npm run dev
```

## 環境變數

`frontend-next/.env.example` 列出前端環境變數：

- `BACKEND_URL`：Next.js 伺服器端連線後端的網址，不應加上 `NEXT_PUBLIC_`。
- `REVALIDATE_SECRET`：前端 `POST /api/revalidate` 路由用來驗證呼叫端的密鑰。目前後端沒有呼叫這個路由，因此現階段不需要在後端設定 `REVALIDATE_SECRET`，也不必為尚未使用的路由設定密鑰。未來若實作受信任的呼叫端（例如後端日終工作），才在前端與該呼叫端設定同一個高熵密鑰，並由呼叫端在 `X-Revalidate-Secret` 標頭傳送；不可將密鑰放進瀏覽器端程式碼。

## 每日報告與快取

每日報告包含受角色保護的資料，因此瀏覽器會透過同源 `/api/reports/daily.xml` 即時擷取，並由瀏覽器自動附上 HttpOnly Cookie；不使用共用 ISR 快取，也不在 Next.js 伺服器端轉送或暴露 Cookie。共用快取不得繞過後端授權。
