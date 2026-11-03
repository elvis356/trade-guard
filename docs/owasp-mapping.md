# OWASP Top 10 對照說明

← [回 README](../README.md)

整理基準：commit `a136b32`（2026-11-03），並納入本次工作樹中的程式與文件更新。本文以 **OWASP Top 10:2025** 作為分類索引，說明 TradeGuard 目前的安全措施、已知限制及對應測試；這不是 OWASP 認證、完整安全稽核或正式環境安全保證。

TradeGuard 是教學用途的虛構證券交易模擬平台，不應用來處理真實交易或敏感金融資料。

## 對照摘要

| OWASP Top 10:2025 | 等級 | 主要差距 |
|---|---|---|
| A01: Broken Access Control（存取控制失效） | ✔ 有 | 持續為新增端點補上物件擁有權測試；`/internal/eod` 使用獨立 token，不走角色授權。 |
| A02: Security Misconfiguration（安全設定錯誤） | ◐ 部分 | CSP 為支援 Next.js hydration 使用 `unsafe-inline`；readiness 公開依賴狀態，部署代理仍須正確清理轉送標頭。 |
| A03: Software Supply Chain Failures（軟體供應鏈失效） | ◐ 部分 | Actions 使用版本 tag、Docker base image 未固定 digest；沒有產物簽章或建置來源證明。 |
| A04: Cryptographic Failures（加密失效） | ◐ 部分 | 金鑰儲存、輪替、TLS 與備份保護仍依賴部署平台。 |
| A05: Injection（注入） | ✔ 有 | 動態排序採欄位白名單；新增查詢仍須審查與測試。 |
| A06: Insecure Design（不安全設計） | ◐ 部分 | 以 IP 限流及帳號鎖定降低濫用；尚無正式威脅模型或容量設計證據。 |
| A07: Authentication Failures（驗證失效） | ◐ 部分 | 鎖定狀態及限流依賴 Redis；JWT 角色在簽發時固定，帳號狀態變動不會即時反映。 |
| A08: Software or Data Integrity Failures（軟體或資料完整性失效） | ◐ 部分 | 尚無資料庫層級稽核／事件防竄改或簽署部署產物。 |
| A09: Security Logging and Alerting Failures（安全記錄與警示失效） | ◐ 部分 | 新增安全事件應用日誌，但未進稽核鏈，亦無集中告警或 SIEM。 |
| A10: Mishandling of Exceptional Conditions（例外狀況處理不當） | ◐ 部分 | Redis 故障 fail-closed；資源耗盡、逾時及復原情境仍需部署層級測試。 |

## 各類別

### A01: Broken Access Control

**措施**

- [SecurityConfiguration](../backend-java/src/main/java/com/tradeguard/config/SecurityConfiguration.java) 依路由及角色授權；未匹配 API 預設要求登入。
- [OrderService](../backend-java/src/main/java/com/tradeguard/order/OrderService.java) 以登入者查詢及取消本人委託；不以隱藏前端按鈕作為授權依據。
- [SurveillanceService](../backend-java/src/main/java/com/tradeguard/surveillance/SurveillanceService.java) 限制監控操作，並要求案件提報者與覆核者不同。
- `/internal/eod` 在 Security 設定中 `permitAll`，由 [InternalEodController](../backend-java/src/main/java/com/tradeguard/audit/InternalEodController.java) 獨立驗證 `X-EOD-Token`；`EOD_TOKEN` 未設定時回 `401`，設定後必須至少 32 UTF-8 bytes。此端點不使用角色授權。
- 前端 [RoleGate](../frontend-next/components/role-gate.tsx) 提供介面層角色限制；真正的安全邊界仍是後端授權。

**已知限制**

- 新增資源端點時仍須逐一驗證物件擁有權及角色，不可只依賴路由層授權。
- EOD token 是獨立的共享憑證；須以部署秘密管理，並避免重複使用其他用途的 token。

**對應測試**

- [OrderApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/order/OrderApiIntegrationTest.java)：`traderCannotReadOrCancelAnotherUsersOrder` 驗證另一位交易者無法在個人委託清單讀取或取消該筆委託。
- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`roleAuthorizationAndAdminUserCreationAreEnforced` 驗證角色路由授權及管理員建立帳號。
- [AuditEodApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/audit/AuditEodApiIntegrationTest.java)：EOD token 驗證。

### A02: Security Misconfiguration

**措施**

- 後端 API 的 CSP、禁止 iframe 嵌入及 Referrer-Policy 設於 [SecurityConfiguration](../backend-java/src/main/java/com/tradeguard/config/SecurityConfiguration.java)。
- 使用者實際瀏覽的 Next.js 前端安全標頭設於 [next.config.ts](../frontend-next/next.config.ts)：CSP、HSTS、`X-Frame-Options`、`X-Content-Type-Options`、Referrer-Policy 及 Permissions-Policy。因 Next.js hydration，CSP 使用 `unsafe-inline`；`unsafe-eval` 僅在 development。
- CORS 使用明確來源清單；[OriginValidationFilter](../backend-java/src/main/java/com/tradeguard/security/OriginValidationFilter.java) 對帶有不允許 Origin 的 unsafe `/api/` 請求拒絕處理。
- [application.yml](../backend-java/src/main/resources/application.yml) 設定 `forward-headers-strategy: framework`，讓後端辨識反向代理轉送的 scheme 與來源位址。
- JWT、資料庫密碼、EOD token 與部署來源透過環境設定提供；[backend Dockerfile](../backend-java/Dockerfile) 的 runtime image 以非 root 使用者執行。

**已知限制**

- Spring 內建 CSRF 已停用。瀏覽器 Cookie 使用 `SameSite=Lax`；Origin filter 只在請求帶有 `Origin` 時檢查 unsafe `/api/` 請求，沒有 Origin 的請求會通過該 filter。CORS 限制瀏覽器跨來源讀取及需預檢的請求，但不應視為通用 CSRF 防護。此策略依賴瀏覽器 Cookie 行為與 Origin 標頭，不能取代逐端點授權，也未涵蓋反向代理或用戶端行為錯誤。
- `forward-headers-strategy: framework` 假設應用程式只透過可信任代理對外；代理必須覆寫／清理外部送入的 `Forwarded`、`X-Forwarded-*` 標頭，否則用戶端可能偽造 scheme 或來源位址。
- 後端 readiness 端點公開，會揭露 API、PostgreSQL 與 Redis 的依賴狀態；目前是營運狀態資訊，不包含憑證。
- [.env.example](../.env.example) 含本機示範管理員帳密 `admin`／`1qaz2wsx3edc` 及其他本機設定，只可用於隔離的本機測試，不可複製到 Render 或其他可公開存取環境。
- 部署前仍須核對 HTTPS、Cookie `Secure`、精確 CORS origins、資料庫 TLS、管理員 bootstrap secrets 清理及雲端服務權限。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`rejectsUntrustedOriginAndFailsClosedWhenRedisIsUnavailable` 拒絕不允許的 Origin。
- 前端安全標頭目前沒有專項自動化測試；`npm run lint` 與 `npm run build` 驗證 Next 設定可載入及建置。
- Readiness 回應內容測試：[HealthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/health/HealthApiIntegrationTest.java)。

### A03: Software Supply Chain Failures

**措施**

- [Dependabot](../.github/dependabot.yml) 每週檢查 GitHub Actions、Maven 與 npm 依賴。
- [CI workflow](../.github/workflows/ci.yml) 執行 Maven `clean verify`、前端 lint/build 及 `npm audit --audit-level=high`。
- [Dependency Review workflow](../.github/workflows/dependency-review.yml) 在 Pull Request 上檢查高嚴重性依賴公告。
- `frontend-next/package-lock.json` 鎖定前端依賴版本。

**已知限制**

- GitHub Actions 目前以版本 tag（例如 `actions/checkout@v7`）引用，未固定到 commit SHA；[backend Dockerfile](../backend-java/Dockerfile) 的 base image 使用 tag，未固定 digest。
- 以上措施不保證上游套件、runner 或 Actions 本身可信，也不等同 SBOM、建置來源證明、產物簽章或部署驗證。

**對應測試**

- CI、依賴稽核及 Dependency Review 由上述 workflow 執行；沒有單一後端測試方法對應整體供應鏈完整性。

### A04: Cryptographic Failures

**措施**

- [SecurityConfiguration](../backend-java/src/main/java/com/tradeguard/config/SecurityConfiguration.java) 使用 BCrypt cost factor 12；密碼不以明文回傳或儲存。
- [JwtService](../backend-java/src/main/java/com/tradeguard/security/JwtService.java) 要求 JWT secret 至少 32 UTF-8 bytes；預設 token lifetime 為 15 分鐘。
- 登入 Cookie 設為 HttpOnly、SameSite=Lax、限定 `/api` path；Secure 屬性由環境設定控制。
- [InternalEodController](../backend-java/src/main/java/com/tradeguard/audit/InternalEodController.java) 以 `MessageDigest.isEqual` 比對 EOD token。
- [AuditService](../backend-java/src/main/java/com/tradeguard/audit/AuditService.java) 使用 SHA-256 將前一筆 hash、操作者與操作內容串成稽核鏈。

**已知限制**

- SHA-256 稽核鏈是完整性檢查，不是加密，也不會阻止具有資料庫寫入權限者重算整條鏈。金鑰儲存與輪替、傳輸加密及資料備份保護依賴部署平台。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`loginSetsHttpOnlyCookieAndCookieAuthenticatesCurrentUser`、`logoutRevokesTokenAndExpiresTheCookie`。
- [AuditEodApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/audit/AuditEodApiIntegrationTest.java)：`verifiesHashChainAndReportsTampering`。

### A05: Injection

**措施**

- 註冊、登入、商品、委託及案件請求使用 DTO 與 Jakarta Bean Validation 限制格式、長度及數值範圍。
- 資料存取主要透過 Spring Data/JPA repository；分頁大小上限為 100。
- [MarketQueryService.loadBook](../backend-java/src/main/java/com/tradeguard/market/MarketQueryService.java) 的原生 SQL 使用 `?` 綁定查詢參數，沒有將標的或狀態直接串接進 SQL。
- [PageableSortValidator](../backend-java/src/main/java/com/tradeguard/common/PageableSortValidator.java) 對接受分頁排序的 API 驗證欄位白名單，拒絕任意實體屬性，例如 `/api/admin/users?sort=passwordHash`。

**已知限制**

- 新增原生 SQL、動態排序／篩選或其他解譯器輸入時，仍需個別審查和測試；不可將使用者輸入直接串接成 SQL、shell 或 HTML。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`rejectsSensitiveAdminSortProperties` 拒絕管理員使用者列表的非白名單排序欄位。
- [InstrumentApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/instrument/InstrumentApiIntegrationTest.java)：`rejectsEmptyPatchAndInvalidSortWith400` 驗證標的列表拒絕無效排序。
- `loadBook` 的 SQL 參數綁定沒有獨立的注入回歸測試。

### A06: Insecure Design

**措施**

- Redis 以帳號鎖定登入失敗；另以來源 IP 限制登入每分鐘 20 次、公開註冊每小時 10 次，超限回 `429`。
- `(user_id, client_order_id)` 唯一約束與 payload 比對避免同一委託重送造成重複處理。
- 委託、成交、事件與稽核寫入使用資料庫交易維持一致性；啟動時重播事件並比對持久化委託及序號。
- 稽核工作台可檢查 hash chain，並核對委託、成交、事件帳本；監控案件採提報者與審核者分離的四眼原則。

**已知限制**

- 登入鎖定以正規化帳號為 key，預設 5 次失敗鎖 15 分鐘；知道帳號的人可故意輸錯密碼，造成該帳號短暫無法登入。
- IP 限流仰賴可信任反向代理正確提供來源位址；分散式來源仍可能規避單一 IP 的限制。尚無正式威脅模型、完整濫用案例覆蓋或服務容量設計證據。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`failedLoginLocksOutAndDoesNotRevealUnknownAccounts`。
- 同檔的 `rateLimitsLoginRequestsBySourceAddress`、`rateLimitsRegistrationRequestsBySourceAddress`。
- `rateLimitFailsClosedWhenRedisIsUnavailable`：限流 Redis 不可用時回 `503`。

### A07: Authentication Failures

**措施**

- [AuthService](../backend-java/src/main/java/com/tradeguard/security/AuthService.java) 對已存在與不存在的帳號都執行密碼雜湊比對，並以通用錯誤訊息回應錯誤帳密。
- 登入失敗計數、每 IP 限流與 token 撤銷狀態存放於 Redis；`JwtCookieAuthenticationFilter` 驗證 JWT 簽章、有效期及撤銷狀態。
- 登出會撤銷 token，Cookie 限定 HttpOnly、SameSite=Lax 與 `/api` path。

**已知限制**

- JWT 的 role claim 在簽發時寫入；[JwtCookieAuthenticationFilter](../backend-java/src/main/java/com/tradeguard/security/JwtCookieAuthenticationFilter.java) 驗簽後直接使用，不會每次查資料庫。若帳號角色或啟用狀態變更，既有 token 最多可能繼續有效至到期；預設最長 15 分鐘。目前未提供帳號停用管理功能。
- Redis 重啟或資料清空可能重置登入失敗計數並清除仍有效 token 的撤銷紀錄。Redis 不可用時認證與限流 fail-closed，避免略過狀態檢查放行。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`knownAndUnknownAccountsReceiveTheSameInvalidCredentialResponse`、`loginSetsHttpOnlyCookieAndCookieAuthenticatesCurrentUser`、`logoutRevokesTokenAndExpiresTheCookie`、`rejectsUntrustedOriginAndFailsClosedWhenRedisIsUnavailable`。

### A08: Software or Data Integrity Failures

**措施**

- 下單、成交及取消事件與相應業務狀態在同一資料庫交易中處理。
- [OrderEventReplayer](../backend-java/src/main/java/com/tradeguard/order/OrderEventReplayer.java) 在啟動時驗證事件重播結果與 PostgreSQL 委託投影、商品序號一致。
- 稽核鏈由 `AuditService` 串接前一筆 hash；`ReconciliationService` 核對委託、成交和事件帳本。
- CI 在變更後執行測試、依賴稽核與建置。

**已知限制**

- 事件與稽核資料的不可變性目前主要由應用程式存取方式維持，尚未加上 PostgreSQL 權限或 trigger 層級的防 UPDATE／DELETE 保護；CI 也未建立已簽署部署產物或可驗證建置證明。

**對應測試**

- [OrderApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/order/OrderApiIntegrationTest.java)：`replayFailsClosedWhenAnAcceptedOrderEventIsMissing`。
- 稽核與對帳各自有測試；這些測試不證明資料庫管理者無法竄改資料。

### A09: Security Logging and Alerting Failures

**措施**

- 業務層重要操作寫入可驗證的稽核鏈，並提供唯讀驗證 API。
- [AuthService](../backend-java/src/main/java/com/tradeguard/security/AuthService.java) 記錄登入失敗及帳號鎖定；[SecurityErrorWriter](../backend-java/src/main/java/com/tradeguard/security/SecurityErrorWriter.java) 記錄 401、403 及限流拒絕；[AuthController](../backend-java/src/main/java/com/tradeguard/auth/AuthController.java) 記錄註冊成功。這些事件不記錄密碼。
- `GlobalExceptionHandler` 會記錄非預期錯誤及部分認證狀態／資料庫完整性故障。

**已知限制**

- 上述安全事件寫入應用程式日誌，不會自動進入稽核 hash chain；沒有集中式安全監控、SIEM 整合、保留政策或告警值班流程。註冊失敗沒有獨立安全事件紀錄。部署平台或代理可能另有存取日誌，但不視為應用程式安全告警已完成。

**對應測試**

- 沒有直接驗證安全日誌內容或集中告警的自動化測試。

### A10: Mishandling of Exceptional Conditions

**措施**

- [GlobalExceptionHandler](../backend-java/src/main/java/com/tradeguard/common/GlobalExceptionHandler.java) 將多種驗證、衝突及伺服器錯誤轉換為共用 JSON 格式；非預期錯誤細節記錄在伺服器端，API 僅回一般訊息。
- Redis 無法檢查登入鎖定、token 撤銷或限流時，相關流程 fail-closed，不略過檢查放行。
- `/api/health/readiness` 能回報 API、PostgreSQL 及 Redis 的依賴狀態；依賴未就緒時 JSON 回應為 `503`。

**已知限制**

- 尚未涵蓋所有資源耗盡、外部服務長時間逾時、重試風暴、部分提交或故障復原情境；需依實際部署拓樸增加故障注入與復原測試。

**對應測試**

- [AuthApiIntegrationTest](../backend-java/src/test/java/com/tradeguard/auth/AuthApiIntegrationTest.java)：`rejectsUntrustedOriginAndFailsClosedWhenRedisIsUnavailable` 驗證登入狀態 Redis 不可用時回 `503`。
- 同檔的 `rateLimitsLoginRequestsBySourceAddress`、`rateLimitsRegistrationRequestsBySourceAddress` 驗證超限回 `429`；`rateLimitFailsClosedWhenRedisIsUnavailable` 驗證限流狀態不可用時回 `503`。

## 優先處理項目

| 優先 | 項目 | 狀態 |
|---|---|---|
| 高 | 補跨使用者讀取／取消委託授權測試（A01） | 已完成：`traderCannotReadOrCancelAnotherUsersOrder`。 |
| 高 | 分頁 `sort` 改欄位白名單，至少保護管理員使用者列表（A05） | 已完成：所有接受分頁排序的 API 都有欄位白名單。 |
| 中 | 前端加入 CSP、frame 與其他安全標頭（A02） | 已完成：設定於 Next.js 全站回應標頭。 |
| 中 | 記錄登入失敗、鎖定、401／403、註冊事件（A09） | 已完成：加入應用程式日誌；尚未串接集中告警或稽核鏈。 |
| 中 | 登入與公開註冊加入通用流量限制（A06／A07） | 已完成：每來源 IP 登入 20 次／分鐘、註冊 10 次／小時；Redis 不可用時回 `503`。 |
| 低 | Actions 固定 commit SHA、Docker image 固定 digest（A03） | 尚未處理。 |
| 低 | 事件／稽核表加 DB 層級防竄改（A08） | 尚未處理。 |

## 如何驗證

- 後端測試：`cd backend-java; .\mvnw.cmd test`。
- 前端檢查：`cd frontend-next; npm run lint`、`npm run build`。
- CI、依賴稽核及 Dependency Review：參閱 [測試與 CI](testing-and-ci.md)。

自動化測試僅證明已涵蓋的測試案例通過，不代表完成 OWASP Top 10 全面驗證、滲透測試或正式環境認證。

## 參考資料

- [OWASP Top 10:2025](https://owasp.org/Top10/2025/)
- [OWASP Top 10 官方專案](https://owasp.org/www-project-top-ten/)
