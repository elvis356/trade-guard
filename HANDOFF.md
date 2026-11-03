# TradeGuard — 目前狀態與後續工作

更新日期：2026-11-03

## 目前狀態

- 後端以 Java 21、Spring Boot、PostgreSQL 與 Redis 提供認證、角色授權、限價撮合、行情、監視、稽核及日終報表 API。
- 前端以 Next.js 提供公開市場總覽與標的頁；`/trade`、管理、監視及稽核工作區依角色限制。
- 本機 Compose 提供 PostgreSQL、Redis 與後端；Vercel／Render 部署設定及 CI 工作流程位於專案內。正式雲端端到端驗證仍需另行執行。
- 啟動時會重播委託事件並驗證 PostgreSQL 持久化狀態一致性；撮合直接使用 PostgreSQL 委託資料。

## 已知限制與後續工作

1. 監視目前只有 `WASH_TRADE` 與 `CLOSING_MARKUP`；量價異常及 spoofing 規則尚未實作。
2. 密碼重設與帳號停用管理尚未提供；帳號及商品管理清單仍有單頁筆數上限。
3. 委託事件與稽核鏈的不可變性目前由應用程式介面維持，尚未加上 PostgreSQL 權限或觸發器層級的防竄改保護；Redis 冪等快速路徑也尚未實作。
4. 完整 Compose 堆疊及正式 Vercel／Render 登入流程仍需在目標環境驗證；部署與本機設定須使用各自獨立的秘密。
5. `/api/revalidate` 尚無受信任的後端呼叫端；不需設定 `REVALIDATE_SECRET`，除非未來真的啟用該呼叫流程。

功能需求以 [功能需求規格](docs/functional-requirements-specification.md) 為準；測試及 CI 說明見 [測試文件](docs/testing-and-ci.md)，部署設定見 [部署文件](docs/deployment.md)。
