package com.tradeguard.audit;

import com.tradeguard.instrument.Instrument;
import com.tradeguard.instrument.InstrumentRepository;
import com.tradeguard.security.AuthStateStore;
import com.tradeguard.user.AppUser;
import com.tradeguard.user.AppUserRepository;
import com.tradeguard.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditEodApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private InstrumentRepository instruments;

    @MockitoBean
    private AuthStateStore authStateStore;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("DELETE FROM surveillance_cases");
        jdbcTemplate.execute("DELETE FROM surveillance_alerts");
        jdbcTemplate.execute("DELETE FROM order_events");
        jdbcTemplate.execute("DELETE FROM trades");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM instruments");
        jdbcTemplate.execute("DELETE FROM app_users");
        when(authStateStore.isTokenRevoked(anyString())).thenReturn(false);
        when(authStateStore.isLoginLocked(anyString(), anyInt())).thenReturn(false);
        when(authStateStore.recordLoginFailure(anyString(), anyInt(), any()))
                .thenReturn(false);
        createUser("trader", Role.TRADER);
        createUser("auditor", Role.AUDITOR);
        instruments.saveAndFlush(new Instrument("AUDT01", "稽核測試商品", true,
                new BigDecimal("50.00")));
    }

    @Test
    void verifiesHashChainAndReportsTampering() throws Exception {
        placeOrder("S", "50.00");
        placeOrder("B", "50.00");

        var initialVerification = mockMvc.perform(get("/api/audit/verify")
                        .with(user("auditor").roles("AUDITOR")))
                .andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(200,
                initialVerification.getResponse().getStatus(),
                initialVerification.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(initialVerification.getResponse().getContentAsString())
                        .get("valid").asBoolean(),
                initialVerification.getResponse().getContentAsString());

        Long id = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM audit_entries", Long.class);
        String originalDetail = jdbcTemplate.queryForObject(
                "SELECT detail FROM audit_entries WHERE id = ?", String.class, id);
        jdbcTemplate.update("UPDATE audit_entries SET detail = ? WHERE id = ?",
                "tampered", id);

        mockMvc.perform(get("/api/audit/verify")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.brokenEntryId").value(id));

        jdbcTemplate.update("UPDATE audit_entries SET detail = ? WHERE id = ?",
                originalDetail, id);
        mockMvc.perform(get("/api/audit/verify")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void reconcilesThreeLedgersAndProducesProtectedDailyXmlAndEodResult()
            throws Exception {
        placeOrder("S", "50.00");
        placeOrder("B", "50.00");

        mockMvc.perform(get("/api/audit/reconcile")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.ordersChecked").value(2))
                .andExpect(jsonPath("$.tradesChecked").value(1));

        Long tradeId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM trades", Long.class);
        jdbcTemplate.update("UPDATE trades SET quantity = 900 WHERE id = ?", tradeId);
        mockMvc.perform(get("/api/audit/reconcile")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.balanced").value(false))
                .andExpect(jsonPath("$.issues[0].code").exists());
        jdbcTemplate.update("UPDATE trades SET quantity = 1000 WHERE id = ?", tradeId);

        mockMvc.perform(get("/api/reports/daily.xml")
                        .with(user("trader").roles("TRADER")))
                .andExpect(status().isForbidden());

        String today = LocalDate.now(ZoneId.of("Asia/Taipei")).toString();
        mockMvc.perform(get("/api/reports/daily.xml")
                        .param("date", today)
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<dailyTradeReport")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<symbol>AUDT01</symbol>")));

        mockMvc.perform(post("/internal/eod"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/eod")
                        .header("X-EOD-Token",
                                "test-only-eod-token-with-at-least-32-bytes")
                        .param("date", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNumber())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.tradeCount").value(1))
                .andExpect(jsonPath("$.reportSha256").value(
                        org.hamcrest.Matchers.matchesPattern("[0-9a-f]{64}")));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eod_reports", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eod_runs", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject(
                        "SELECT trade_count FROM eod_reports WHERE business_date = ?",
                        Integer.class, java.sql.Date.valueOf(today)));
    }

    private void placeOrder(String side, String price) throws Exception {
        String payload = """
                {"client_order_id":"%s","symbol":"AUDT01","side":"%s",
                 "price":%s,"qty":1000}
                """.formatted(UUID.randomUUID(), side, price);
        mockMvc.perform(post("/api/orders")
                        .with(user("trader").roles("TRADER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void createUser(String username, Role role) {
        users.saveAndFlush(new AppUser(username,
                passwordEncoder.encode("correct-horse-battery-12"), role));
    }
}
