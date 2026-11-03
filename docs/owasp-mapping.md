# OWASP Top 10 對照說明

← [回 README](../README.md)

本文以 **OWASP Top 10:2025** 作為分類索引，整理 TradeGuard 目前程式中與各風險類別相關的安全措施、實作位置及尚未涵蓋之處。這是程式導覽與差距摘要，**不是 OWASP 認證、完整安全稽核或正式環境安全保證**。某項控制與風險類別相關，不代表該類風險已完全消除。

TradeGuard 是教學用途的虛構證券交易模擬平台，不應用來處理真實交易或敏感金融資料。

## 對照摘要

| OWASP Top 10:2025 | TradeGuard 中的相關措施 | 覆蓋狀況 |
|---|---|---|
| A01: Broken Access Control（存取控制失效） | Spring Security 依路由及角色授權；本人委託限制；監控案件雙人覆核；未匹配 API 預設要求登入。 | 已有多層控制；仍須持續測試每個端點及物件層級授權。 |
| A02: Security Misconfiguration（安全設定錯誤） | CSP、frame 與 referrer 標頭；CORS 白名單及 unsafe Origin 檢查；部署秘密以環境變數注入；Docker runtime 使用非 root 帳號。 | 已有基線；生產環境設定與完整安全標頭需持續核對。 |
| A03: Software Supply Chain Failures（軟體供應鏈失效） | 前端鎖定檔、CI `npm audit`、Pull Request Dependency Review、後端及前端建置測試。 | 部分覆蓋；不等於供應鏈完整性或套件來源已獲保證。 |
| A04: Cryptographic Failures（加密失效） | BCrypt 密碼雜湊；有最低長度要求的 JWT 簽署金鑰與短效 token；安全 Cookie；EOD token 常數時間比較；稽核雜湊鏈。 | 已有相關措施；金鑰管理、TLS 與資料保護仍依部署環境。 |
| A05: Injection（注入） | 請求 DTO Bean Validation；以 Spring Data/JPA repository 存取資料；資料庫約束；分頁大小上限。 | 對已知輸入有防護；新查詢及輸入仍需個別審查和測試。 |
| A06: Insecure Design（不安全設計） | 登入失敗鎖定、冪等鍵、交易一致性、事件重播與帳本對帳、監控案件四眼原則。 | 有針對核心流程的控制；尚無證據顯示完成正式威脅建模或全面濫用情境分析。 |
| A07: Authentication Failures（驗證失效） | BCrypt、統一登入錯誤、未知帳號 dummy hash、Redis 登入失敗計數、短效 JWT、登出撤銷。 | 已有認證控制；Redis 重啟／清空會影響鎖定與撤銷狀態。 |
| A08: Software or Data Integrity Failures（軟體或資料完整性失效） | 委託事件與業務資料同交易寫入；啟動重播驗證持久化狀態；SHA-256 稽核鏈與三方對帳；CI 執行建置與測試。 | 應用層有完整性檢查；資料庫層級防竄改及建置產物簽署尚未完成。 |
| A09: Security Logging and Alerting Failures（安全記錄與警示失效） | 重要業務操作寫入稽核鏈；非預期伺服器錯誤及部分資料完整性問題會記錄應用程式日誌。 | 部分覆蓋；業務稽核不等於安全事件監控，尚無集中告警／SIEM 流程。 |
| A10: Mishandling of Exceptional Conditions（例外狀況處理不當） | 統一錯誤格式；避免向 API 使用者輸出未預期例外細節；Redis 認證狀態不可用時 fail-closed；健康檢查回報依賴狀態。 | 已有共通處理；仍須測試資源耗盡、逾時及各種依賴故障情境。 |

## 各類別與程式位置

### A01: Broken Access Control

- `backend-java/src/main/java/com/tradeguard/config/SecurityConfiguration.java` 將 API 路由限制為公開、已登入或指定角色；最後的 `.anyRequest().authenticated()` 讓沒有明確規則的路由不會因此成為公開端點。
- `OrderService` 以目前登入者查詢及取消本人委託；不以隱藏前端按鈕作為授權依據。
- `SurveillanceService` 限制監控操作，並要求案件提報者與覆核者不同。
- `frontend-next/components/role-gate.tsx` 及導覽角色判斷提供使用體驗上的限制；真正的安全邊界仍是後端授權。

**注意：**角色路由保護不會自動保證每個物件都符合擁有者限制。新增資源讀寫端點時，需一併驗證「使用者是否可操作該筆資料」。

### A02: Security Misconfiguration

- `SecurityConfiguration` 設定 Content Security Policy、禁止 iframe 嵌入及 Referrer-Policy。
- CORS 使用明確來源清單；`OriginValidationFilter` 對帶有不允許 Origin 的 unsafe `/api/` 請求拒絕處理。
- JWT、資料庫密碼、EOD token 與部署來源透過環境設定提供；`docs/deployment.md` 說明雲端秘密設定方式。
- `backend-java/Dockerfile` 的 runtime image 以非 root `spring` 使用者執行。

**注意：**CORS 不是 API 授權機制，也不能單獨取代 CSRF 防護。部署前仍需確認 HTTPS、Cookie `Secure`、精確 CORS origins、資料庫 TLS、管理員 bootstrap secrets 清理及雲端服務權限；本機開發設定不應直接複製到公開環境。

### A03: Software Supply Chain Failures

- `frontend-next/package-lock.json` 鎖定前端依賴版本。
- `.github/workflows/ci.yml` 執行 Maven `clean verify`、前端 lint/build 及 `npm audit --audit-level=high`。
- `.github/workflows/dependency-review.yml` 在 Pull Request 上檢查高嚴重性依賴公告。

**尚未涵蓋：**這些檢查不保證上游套件、建置 runner 或 Actions 本身可信，也不等同 SBOM、建置來源證明、產物簽章或部署驗證。工作流程使用的 Actions 版本應納入定期審查。

### A04: Cryptographic Failures

- `SecurityConfiguration` 使用 BCrypt cost factor 12；密碼不以明文回傳或儲存。
- `JwtService` 要求 JWT secret 至少 32 UTF-8 bytes，限制 token lifetime 為 1 秒至 1 小時；目前預設為 15 分鐘。
- 登入 Cookie 設為 HttpOnly、SameSite=Lax，Secure 屬性由環境設定控制。
- `InternalEodController` 以 `MessageDigest.isEqual` 比對 EOD token。
- `AuditService` 使用 SHA-256 將前一筆 hash、操作者與操作內容串成稽核鏈。

**注意：**SHA-256 稽核鏈是完整性檢查，不是加密，也不會阻止具有資料庫寫入權限者重算整條鏈。金鑰安全儲存與輪替、傳輸加密及資料備份保護依賴部署平台設定。

### A05: Injection

- 註冊、登入、商品、委託及案件請求使用 DTO 與 Jakarta Bean Validation 限制格式、長度及數值範圍。
- 資料存取主要透過 Spring Data/JPA repository；對外分頁大小上限為 100。
- 資料庫唯一鍵與外鍵等約束提供額外一致性檢查。

**注意：**輸入驗證不能替代安全的查詢建構。新增原生 SQL、動態排序／篩選或其他解譯器輸入時，需使用參數化查詢並測試不可信輸入；不要把使用者輸入直接串接成 SQL、shell 或 HTML。

### A06: Insecure Design

- Redis 登入失敗計數會在達門檻後暫時鎖定登入。
- `(user_id, client_order_id)` 唯一約束與 payload 比對避免同一委託重送造成重複處理。
- 委託、成交、事件與稽核寫入使用資料庫交易維持一致性；啟動時重播事件並比對持久化委託及序號。
- 稽核工作台可檢查 hash chain，並核對委託、成交、事件帳本。
- 監控案件採提報者與審核者分離的四眼原則。

**尚未涵蓋：**目前未提供正式系統威脅模型、完整濫用案例覆蓋或服務容量／資源消耗設計證據。部署環境應另行決定流量限制、備援與告警策略。

### A07: Authentication Failures

- `AuthService` 對已存在與不存在的帳號都執行密碼雜湊比對，並以通用錯誤訊息回應錯誤帳密。
- 登入失敗次數及鎖定狀態存放於 Redis；`JwtCookieAuthenticationFilter` 驗證簽章、有效期及撤銷狀態。
- 登出會撤銷 token，Cookie 限定 HttpOnly、SameSite=Lax 與 `/api` path。

**限制：**Redis 是登入鎖定及 token 撤銷狀態的暫存來源。若 Redis 重啟或資料清空，仍有效 JWT 的撤銷紀錄可能消失，登入失敗計數也可能重置；目前部署型態不應被視為具持久撤銷狀態的高保證認證服務。

### A08: Software or Data Integrity Failures

- 下單、成交及取消事件與相應業務狀態在同一資料庫交易中處理。
- `OrderEventReplayer` 在啟動時驗證事件重播結果與 PostgreSQL 委託投影、商品序號一致。
- 稽核鏈由 `AuditService` 串接前一筆 hash；`ReconciliationService` 核對委託、成交和事件帳本。
- CI 在變更後執行測試、依賴稽核與建置。

**尚未涵蓋：**事件與稽核資料的不可變性目前主要由應用程式存取方式維持，尚未加上 PostgreSQL 權限或 trigger 層級的防 UPDATE／DELETE 保護；CI 也未建立已簽署部署產物或可驗證建置證明。

### A09: Security Logging and Alerting Failures

- 業務層重要操作會寫入可驗證的稽核鏈，並提供唯讀驗證 API。
- `GlobalExceptionHandler` 會記錄非預期錯誤及部分認證狀態／資料庫完整性故障；對用戶端回傳統一錯誤格式。

**尚未涵蓋：**稽核鏈是交易與業務操作紀錄，不是集中式安全事件監控。尚無登入異常、權限拒絕、管理員行為等事件的集中告警、SIEM 整合、保留政策或告警值班流程。不要將一般應用日誌視為已完成入侵偵測。

### A10: Mishandling of Exceptional Conditions

- `GlobalExceptionHandler` 將多種驗證、衝突及伺服器錯誤轉換為共用 JSON 格式；非預期錯誤的細節記錄在伺服器端，API 僅回一般訊息。
- Redis 無法檢查登入鎖定或 token 撤銷時，認證流程會 fail-closed，而不是略過檢查放行。
- `/api/health/readiness` 能回報 API、PostgreSQL 及 Redis 的依賴狀態；依賴未就緒時 JSON 回應為 `503`。

**尚未涵蓋：**這些措施不代表已處理所有資源耗盡、外部服務長時間逾時、重試風暴、部分提交或故障復原情境；需依實際部署拓樸增加故障注入與復原測試。

## 如何驗證

- 後端授權、認證、Origin 檢查、Redis 故障 fail-closed、委託重播及稽核／對帳測試：`backend-java/src/test/java/com/tradeguard/`。
- CI、依賴稽核及 Dependency Review：`.github/workflows/ci.yml`、`.github/workflows/dependency-review.yml`。
- 執行方式與測試限制：參閱[測試與 CI](testing-and-ci.md)。

自動化測試僅證明已涵蓋的測試案例通過，不代表完成 OWASP Top 10 全面驗證、滲透測試或正式環境認證。

## 參考資料

- [OWASP Top 10:2025](https://owasp.org/Top10/2025/)
- [OWASP Top 10 官方專案](https://owasp.org/www-project-top-ten/)
