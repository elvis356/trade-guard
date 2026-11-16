# 目前限制與後續工作

← [回 README](../README.md)

## 後端與資料

本機 PostgreSQL／Redis 主要流程已透過隔離的測試服務驗證，但完整 Compose 堆疊仍需另行驗收。監控目前僅實作 `WASH_TRADE` 與 `CLOSING_MARKUP`；量價異常規則需要 20 日每日 K 線資料來源。事件與稽核資料表的資料庫層級寫入保護，以及 Redis 冪等性加速仍屬後續強化工作。

## 前端

前端已提供登入、角色導覽、商品與帳號管理、委託交易、監控案件、稽核及日終報表工作區；尚未完成真實雲端端到端驗證。管理介面目前以第一頁最多 100 筆呈現帳號／商品、委託、警示與案件，完整跨頁瀏覽仍可後續擴充。

## 尚未提供的功能

密碼重設與帳號停用管理尚未提供。

公開註冊已實作：`/register` 與 `POST /api/auth/register` 可建立 `TRADER` 帳號；建立其他角色仍需管理員權限。

## 安全強化與後續工作

本節延伸 [OWASP Top 10 對照說明](owasp-mapping.md)（採 OWASP Top 10:2025 分類），評估各類別哪些可交由 Spring Boot 生態系套件建置、哪些必須自行實作，並列出建議的後續順序。

> **範圍與限制**：本節依 README 與 `owasp-mapping.md` 推斷，**未逐行檢視原始碼**；實際類別名稱與行為以程式碼為準。套件版本與 API 請以當前穩定版官方文件為準。「可用套件建置」不等於「風險已消除」，每項控制仍需測試驗證。這是技術評估與建議，不是安全稽核或 OWASP 認證。

### 圖例

| 符號 | 意義 |
|---|---|
| ✅ | 套件／框架可直接建置 |
| 🟡 | 套件提供骨架或掛鉤，業務邏輯仍需自寫 |
| 🔧 | 需親自實作（無現成套件，或屬業務規則／流程） |

### 總覽

| 類別（OWASP Top 10:2025） | 可用套件／框架 | 必須自己實作 |
|---|---|---|
| **A01** Broken Access Control | ✅ Spring Security（路由授權、`@PreAuthorize`／`@PostAuthorize`） | 🔧 物件層級擁有者檢查、四眼原則 |
| **A02** Security Misconfiguration | ✅ Spring Security（`headers()`、CORS）、Actuator 設定 | 🔧 部署設定核對；Origin 檢查（除非改用內建 CSRF） |
| **A03** Software Supply Chain Failures | ✅ Dependabot、OWASP Dependency-Check、CycloneDX、cosign／GitHub attestations | 🔧 Actions 版本審查流程 |
| **A04** Cryptographic Failures | ✅ Spring Security Crypto、Nimbus JOSE、JDK `MessageDigest` | 🔧 金鑰輪替、稽核鏈設計 |
| **A05** Injection | ✅ Bean Validation、Spring Data JPA、`StrictHttpFirewall` | 🔧 業務範圍規則、動態排序白名單 |
| **A06** Insecure Design | 🟡 Bucket4j／Resilience4j（流量限制）、資料庫約束 | 🔧 冪等、事件重播、對帳、鎖定策略、威脅建模 |
| **A07** Authentication Failures | ✅ Spring Security、OAuth2 Resource Server、Spring Session | 🔧 Redis 失敗計數、撤銷清單邏輯 |
| **A08** Software or Data Integrity Failures | 🟡 Flyway（已用）、PostgreSQL 權限／trigger、cosign | 🔧 SHA-256 稽核鏈、事件重播、三方對帳 |
| **A09** Security Logging and Alerting Failures | ✅ Actuator `auditevents`、Spring Security 事件、Micrometer、logstash-logback-encoder | 🔧 告警規則、保留政策、值班流程 |
| **A10** Mishandling of Exceptional Conditions | ✅ `ProblemDetail`、Resilience4j、Actuator health groups | 🔧 fail-closed 策略、故障演練 |

### 逐類說明

#### A01 存取控制失效

- **目前實作**：`SecurityConfiguration` 路由與角色規則、`.anyRequest().authenticated()`；`OrderService` 本人委託限制；`SurveillanceService` 四眼原則。
- **套件可做**：`@EnableMethodSecurity` 搭配 `@PreAuthorize`、`@PostAuthorize`，以及 Spring Data 的 SpEL principal 查詢，把擁有者檢查改為宣告式。
- **自己做**：「只能操作本人委託」與「提報者 ≠ 審核者」是業務規則，套件只提供表達式入口，判斷邏輯仍要自寫。

#### A02 安全設定錯誤

- **目前實作**：CSP、frame、Referrer-Policy 標頭；CORS 白名單；`OriginValidationFilter`；環境變數注入密鑰；Docker 非 root。
- **套件可做**：`headers()` 可補 HSTS、`X-Content-Type-Options`、Permissions-Policy；Actuator 可限制暴露端點。
- **可替換**：`OriginValidationFilter` 可改用內建 CSRF（`CookieCsrfTokenRepository`），或繼續保留 Origin 檢查，兩者擇一。
- **自己做**：HTTPS、Cookie `Secure`、精確 CORS origins、資料庫 TLS 等部署核對屬於流程。

#### A03 軟體供應鏈失效

- **目前實作**：前端 lockfile、`npm audit`、Dependency Review、Maven `clean verify`。
- **缺口**：後端 Maven 缺少專屬弱點掃描，也沒有 SBOM 或建置證明。
- **套件可做**：`dependency-check-maven`、Dependabot、CycloneDX Maven plugin（SBOM）、`actions/attest-build-provenance`、cosign（簽章）。
- **自己做**：定期審查 GitHub Actions 版本與來源。

#### A04 加密失效

- **目前實作**：BCrypt cost 12、JWT 金鑰至少 32 bytes 與短效 token、`MessageDigest.isEqual` 比對 EOD token、SHA-256 稽核鏈。這些本來就走標準函式庫。
- **套件可做**：`DelegatingPasswordEncoder`（日後升級雜湊演算法）；欄位加密可用 Spring Security Crypto 的 `Encryptors`（AES-GCM）；金鑰託管可考慮 Spring Vault。
- **自己做**：金鑰儲存與輪替、TLS 依平台設定；稽核鏈的資料結構與驗證邏輯是專案特有設計。

#### A05 注入

- **目前實作**：DTO + Jakarta Bean Validation、Spring Data／JPA repository、分頁上限 100、資料庫約束。
- **套件可做**：`StrictHttpFirewall`（Spring Security 預設啟用）擋異常路徑與標頭；若日後輸出 HTML，再引入 OWASP Java HTML Sanitizer／Encoder。
- **自己做**：價格區間、升降單位、1,000 股整股單位等業務驗證；動態 `sort` 參數的屬性白名單。

#### A06 不安全設計

這一類最偏設計層面，套件能幫的最少。

- **目前實作**：Redis 登入鎖定、`(user_id, client_order_id)` 冪等、交易一致性、事件重播、對帳、四眼原則。
- **自己做**：以上核心流程皆為專案特有邏輯。
- **缺口**：流量限制。Spring Security 沒有，可用 Bucket4j 或 Resilience4j `RateLimiter`。目前尚無正式威脅模型，可用 OWASP Threat Dragon（工具）做 STRIDE 分析。

#### A07 驗證失效

- **目前實作**：`AuthService`（dummy hash、通用錯誤訊息）、Redis 失敗計數、`JwtCookieAuthenticationFilter`、登出撤銷。
- **可替換現有自訂程式**：

  | 現有 | 可換成 |
  |---|---|
  | `JwtCookieAuthenticationFilter` | OAuth2 Resource Server（自訂 `BearerTokenResolver` 讀 Cookie） |
  | `AuthService` 的未知帳號 dummy hash | `DaoAuthenticationProvider`（內建防計時攻擊） |
  | 通用錯誤訊息 | 內建 `hideUserNotFoundExceptions` |

- **可選**：`CompromisedPasswordChecker`（Spring Security 6.3+，擋外洩密碼）；或改用 **Spring Session Data Redis**，登出與撤銷由框架處理，就不必維護 jti 黑名單。
- **自己做**：Redis 登入失敗計數（可掛在 `AuthenticationFailureBadCredentialsEvent`，鎖定時拋 `LockedException`）、撤銷檢查（接在 `OAuth2TokenValidator`）。
- **已知限制**：Redis 重啟或清空會使鎖定與撤銷狀態消失，見[部署說明](deployment.md#render)。

#### A08 軟體或資料完整性失效

- **目前實作**：事件與業務資料同交易寫入、`OrderEventReplayer` 啟動重播、`AuditService` SHA-256 稽核鏈、`ReconciliationService` 三方對帳。
- **自己做**：以上沒有現成套件，屬於專案核心。
- **套件／資料庫可補的缺口**：事件與稽核資料表的資料庫層級防竄改尚未完成，可透過 Flyway migration 撰寫 PostgreSQL `REVOKE UPDATE, DELETE` 或 trigger，使其成為 append-only；建置產物簽署可用 cosign。

#### A09 安全記錄與警示失效

- **目前實作**：業務稽核鏈、`GlobalExceptionHandler` 記錄非預期錯誤。
- **套件可做**：Spring Boot Actuator 搭配 Spring Security 會自動發布認證成功／失敗、授權拒絕的 `AuditEvent`（`auditevents` 端點），可補上「登入異常、權限拒絕」的缺口；Micrometer + Prometheus／Grafana 做指標與告警；`logstash-logback-encoder` 輸出結構化 JSON 日誌。
- **自己做**：哪些事件要告警、閾值、保留期限、誰值班。業務稽核鏈與安全事件監控是兩件事，不能互相取代。

#### A10 例外狀況處理不當

- **目前實作**：`GlobalExceptionHandler` 統一 JSON 格式、Redis 認證狀態不可用時 fail-closed、`/api/health/readiness`。
- **可替換**：共用錯誤格式可改用 Spring 6 的 `ProblemDetail`（RFC 9457）；安全層的 401／403 以 `AuthenticationEntryPoint`、`AccessDeniedHandler` 對齊同一格式。
- **套件可做**：Resilience4j 的 `TimeLimiter`、`CircuitBreaker`、`Bulkhead` 處理逾時與依賴故障；Actuator health groups 處理 readiness。
- **自己做**：哪些依賴 fail-closed、哪些可降級（例如 Redis 報價快取可降級到 PostgreSQL，但認證狀態必須 fail-closed），以及故障注入與復原測試。

### 補充：標頭、CORS、CSRF 的預設行為

以 Spring Security 6.x 為準：

| 項目 | 實際行為 |
|---|---|
| `X-Content-Type-Options`、`X-Frame-Options`、`Cache-Control` | 預設啟用 |
| `Strict-Transport-Security` | 預設啟用，但只在 HTTPS 請求時送出 |
| CSP | 非預設，須自己撰寫 policy |
| Referrer-Policy、Permissions-Policy | 非預設，須明確設定 |
| CORS | 預設不啟用，需呼叫 `http.cors(...)` 並提供 `CorsConfigurationSource` |
| CSRF | 預設啟用，機制為 token 式；TradeGuard 目前使用自訂 Origin 檢查 |

- Spring Security 的標頭只作用於後端 API 回應。Next.js（Vercel）前端頁面的標頭需在 `next.config.js` 的 `headers()` 設定。
- Security Misconfiguration 在 OWASP Top 10:2021 是 A05，在本專案採用的 2025 版是 A02；2025 版的 A05 是 Injection。引用時請先確認版本。

### 可新增的依賴（示意）

```xml
<!-- 已有：spring-boot-starter-security / validation / data-jpa / data-redis -->

<!-- A07：取代自訂 JWT filter -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- A09：安全事件稽核與指標 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version><!-- 依當前穩定版 --></version>
</dependency>

<!-- A06／A10：流量限制、熔斷、逾時 -->
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-spring-boot3</artifactId>
  <version><!-- 依當前穩定版 --></version>
</dependency>

<!-- A03：Maven 端依賴掃描與 SBOM（build plugins） -->
<!-- org.owasp:dependency-check-maven、org.cyclonedx:cyclonedx-maven-plugin -->
```

<details>
<summary>SecurityFilterChain 草稿（A02／A07，示意，需依實際程式調整）</summary>

```java
@Configuration
@EnableMethodSecurity
class SecurityConfig {

  // 內部 EOD 端點：獨立 chain
  @Bean @Order(1)
  SecurityFilterChain internal(HttpSecurity http, EodTokenFilter eodFilter) throws Exception {
    http.securityMatcher("/internal/**")
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(eodFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("EOD"));
    return http.build();
  }

  // 主要 API
  @Bean @Order(2)
  SecurityFilterChain api(HttpSecurity http, CorsConfigurationSource cors,
                          JwtDecoder decoder, Converter<Jwt, AbstractAuthenticationToken> roles) throws Exception {
    http.cors(c -> c.configurationSource(cors))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(h -> h
            .httpStrictTransportSecurity(x -> x.includeSubDomains(true).maxAgeInSeconds(31536000))
            .contentSecurityPolicy(x -> x.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            .referrerPolicy(x -> x.policy(ReferrerPolicy.NO_REFERRER)))
        .oauth2ResourceServer(o -> o
            .bearerTokenResolver(req -> {
              Cookie c = WebUtils.getCookie(req, "token");   // Cookie 名稱依實際為準
              return c == null ? null : c.getValue();
            })
            .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(roles)))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/health", "/api/market/**").permitAll()
            .requestMatchers("/api/admin/**", "/api/market/instruments/**").hasRole("ADMIN")
            .requestMatchers("/api/orders/**").hasRole("TRADER")
            .requestMatchers("/api/alerts/**", "/api/cases/**").hasAnyRole("SURVEILLANCE", "AUDITOR", "ADMIN")
            .requestMatchers("/api/audit/**", "/api/reports/**").hasAnyRole("AUDITOR", "ADMIN")
            .anyRequest().authenticated());
    return http.build();
  }

  // JWT 撤銷：接上 Redis 黑名單
  @Bean
  JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret, StringRedisTemplate redis) {
    NimbusJwtDecoder d = NimbusJwtDecoder
        .withSecretKey(new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")).build();
    OAuth2TokenValidator<Jwt> revoked = jwt ->
        Boolean.TRUE.equals(redis.hasKey("revoked:" + jwt.getId()))
            ? OAuth2TokenValidatorResult.failure(new OAuth2Error("token_revoked"))
            : OAuth2TokenValidatorResult.success();
    d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), revoked));
    return d;
  }
}
```

路由、Cookie 名稱、角色對應與 secret 格式需與現有程式一致；Redis 不可用時的 fail-closed 行為也需保留（`redis.hasKey` 例外應導致驗證失敗，而非放行）。

</details>

### 建議優先順序

| 順序 | 項目 | 對應類別 | 理由 |
|---|---|---|---|
| 1 | 啟用 Actuator `auditevents` | A09 | 成本最低，一次補上登入失敗與權限拒絕紀錄 |
| 2 | 登入與下單端點加流量限制 | A06 | 目前沒有 |
| 3 | 事件與稽核表加 append-only 保護 | A08 | 與上方「後端與資料」的待強化項目相同 |
| 4 | 補後端 Maven 依賴掃描與 SBOM | A03 | 目前僅前端有 `npm audit` |
| 5 | 評估以 Resource Server／`ProblemDetail` 取代自訂 filter 與錯誤格式 | A07／A10 | 降低維護面積 |

### 設計取捨

通用安全控制（認證、安全標頭、加密、輸入驗證）優先採用成熟套件，並理解其預設值；密碼學與認證機制不自行發明。業務規則（擁有者檢查、四眼原則、冪等、稽核鏈、對帳、告警策略）本來就屬自行實作範圍，需搭配測試。若專案定位為教學展示，保留自訂 JWT filter 也有價值，前提是有測試覆蓋並說明設計理由；若要貼近生產環境，則建議改用框架內建機制。

## 部署環境

Render 免費方案的休眠與 Redis 重啟清空限制，見 [部署說明](deployment.md#render)。

更完整的階段估算與待辦清單見 [HANDOFF.md](../HANDOFF.md)。
