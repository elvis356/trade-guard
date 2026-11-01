# 測試與 CI

← [回 README](../README.md)

## 後端測試

不使用 Docker 執行後端整合測試：

```powershell
cd backend-java
.\mvnw.cmd test
```

macOS / Linux 請使用 `./mvnw test`。

測試使用 PostgreSQL 相容模式的 H2，並模擬由 Redis 管理的驗證狀態；市場與監控功能在 Redis 中斷時改用 PostgreSQL 的備援路徑也會納入測試。

## UML 工作台資料驗證

修改 `docs/uml-workbench.html` 的 `uml-data` 區塊後執行：

```
node docs/uml-workbench.check.js
```

## CI 與安全檢查

GitHub Actions 會在推送及 Pull Request 上執行後端 `clean verify`、前端 lint／build 及 npm 相依套件稽核。Dependency Review 會阻擋引入高嚴重性相依套件安全公告的 Pull Request。相依套件鎖定檔已提交，因此 CI 使用可重現的前端套件版本。
