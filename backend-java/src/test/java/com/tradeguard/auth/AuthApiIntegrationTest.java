package com.tradeguard.auth;

import com.tradeguard.security.AuthStateStore;
import com.tradeguard.security.AuthStateUnavailableException;
import com.tradeguard.security.JwtService;
import com.tradeguard.user.AppUser;
import com.tradeguard.user.AppUserRepository;
import com.tradeguard.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthStateStore authStateStore;

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("DELETE FROM surveillance_cases");
        jdbcTemplate.execute("DELETE FROM surveillance_alerts");
        jdbcTemplate.execute("DELETE FROM order_events");
        jdbcTemplate.execute("DELETE FROM trades");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM instruments");
        jdbcTemplate.execute("DELETE FROM app_users");
        reset(authStateStore);
        when(authStateStore.isLoginLocked(anyString(), anyInt())).thenReturn(false);
        when(authStateStore.recordLoginFailure(anyString(), anyInt(),
                org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(authStateStore.isTokenRevoked(anyString())).thenReturn(false);
    }

    @Test
    void publicMarketReadsRemainAvailableAndProtectedRoutesUseJson401() throws Exception {
        mockMvc.perform(get("/api/market/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginSetsHttpOnlyCookieAndCookieAuthenticatesCurrentUser() throws Exception {
        createUser("trader1", "correct-horse-battery-12", Role.TRADER);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"TRADER1","password":"correct-horse-battery-12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api")))
                .andExpect(jsonPath("$.user.username").value("trader1"))
                .andExpect(jsonPath("$.user.role").value("TRADER"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();

        var cookie = login.getResponse().getCookie("tradeguard_session");
        String tokenId = jwtService.parse(cookie.getValue()).tokenId();
        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("trader1"))
                .andExpect(jsonPath("$.role").value("TRADER"));
        verify(authStateStore).isTokenRevoked(tokenId);
    }

    @Test
    void publicRegistrationCreatesOnlyTraderAccountsAndAllowsLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newtrader","password":"correct-horse-battery-12",
                                 "role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.username").value("newtrader"))
                .andExpect(jsonPath("$.role").value("TRADER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newtrader","password":"correct-horse-battery-12"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("TRADER"));
    }

    @Test
    void publicRegistrationRejectsDuplicateAndInvalidAccounts() throws Exception {
        createUser("taken", "correct-horse-battery-12", Role.TRADER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"taken","password":"correct-horse-battery-12"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("帳號無法使用，請改用其他帳號。"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bad name","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void logoutRevokesTokenAndExpiresTheCookie() throws Exception {
        createUser("trader1", "correct-horse-battery-12", Role.TRADER);
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"correct-horse-battery-12"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        var cookie = login.getResponse().getCookie("tradeguard_session");
        String tokenId = jwtService.parse(cookie.getValue()).tokenId();

        mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authStateStore).revokeToken(anyString(), org.mockito.ArgumentMatchers.any());
        when(authStateStore.isTokenRevoked(tokenId)).thenReturn(true);
        mockMvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failedLoginLocksOutAndDoesNotRevealUnknownAccounts() throws Exception {
        createUser("trader1", "correct-horse-battery-12", Role.TRADER);
        when(authStateStore.recordLoginFailure(anyString(), anyInt(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"wrong-password"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing","password":"wrong-password"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("登入失敗次數過多，請稍後再試。"));
    }

    @Test
    void knownAndUnknownAccountsReceiveTheSameInvalidCredentialResponse() throws Exception {
        createUser("trader1", "correct-horse-battery-12", Role.TRADER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號或密碼錯誤。"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("帳號或密碼錯誤。"));
    }

    @Test
    void roleAuthorizationAndAdminUserCreationAreEnforced() throws Exception {
        createUser("trader1", "correct-horse-battery-12", Role.TRADER);
        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"correct-horse-battery-12"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(get("/api/audit/verify")
                        .cookie(login.getResponse().getCookie("tradeguard_session")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/admin/users")
                        .cookie(login.getResponse().getCookie("tradeguard_session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auditor1","password":"very-long-demo-password",
                                 "role":"AUDITOR"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"auditor1","password":"very-long-demo-password",
                                 "role":"AUDITOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("auditor1"))
                .andExpect(jsonPath("$.role").value("AUDITOR"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/admin/users?page=0&size=10")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void rejectsUntrustedOriginAndFailsClosedWhenRedisIsUnavailable() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"wrong-password"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register")
                                .header("Origin", "https://evil.example")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"eviltrader","password":"correct-horse-battery-12"}
                                        """))
                .andExpect(status().isForbidden());
        assertFalse(users.existsByUsername("eviltrader"));

        when(authStateStore.isLoginLocked(anyString(), anyInt()))
                .thenThrow(new AuthStateUnavailableException("Redis unavailable."));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader1","password":"wrong-password"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    private void createUser(String username, String password, Role role) {
        users.saveAndFlush(new AppUser(username, passwordEncoder.encode(password), role));
    }
}
