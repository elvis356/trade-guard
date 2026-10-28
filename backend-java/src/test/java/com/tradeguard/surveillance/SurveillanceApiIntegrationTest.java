package com.tradeguard.surveillance;

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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SurveillanceApiIntegrationTest {

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
        createUser("surv1", Role.SURVEILLANCE);
        createUser("surv2", Role.SURVEILLANCE);
        instruments.saveAndFlush(new Instrument("WASH01", "監視測試商品", true,
                new BigDecimal("50.00")));
    }

    @Test
    void washAlertRequiresDifferentPeopleForProposalAndReview() throws Exception {
        UUID clientOrderId = UUID.randomUUID();
        placeOrder("S", "50.00", clientOrderId);
        placeOrder("B", "50.00", UUID.randomUUID());

        String alertResponse = mockMvc.perform(get("/api/alerts")
                        .with(user("surv1").roles("SURVEILLANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ruleCode").value("WASH_TRADE"))
                .andExpect(jsonPath("$.content[0].severity").value("HIGH"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/alerts").with(user("trader").roles("TRADER")))
                .andExpect(status().isForbidden());

        long alertId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(alertResponse).get("content").get(0).get("id").asLong();
        String caseResponse = mockMvc.perform(post("/api/alerts/{id}/case", alertId)
                        .with(user("surv1").roles("SURVEILLANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSED"))
                .andReturn().getResponse().getContentAsString();
        long caseId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(caseResponse).get("id").asLong();

        mockMvc.perform(post("/api/cases/{id}/review", caseId)
                        .with(user("surv1").roles("SURVEILLANCE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true,\"note\":\"same user\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cases/{id}/review", caseId)
                        .with(user("surv2").roles("SURVEILLANCE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approve\":true,\"note\":\"Reviewed by another person\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("surv2"));

        mockMvc.perform(get("/api/cases")
                        .with(user("surv1").roles("SURVEILLANCE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private void placeOrder(String side, String price, UUID clientOrderId) throws Exception {
        String payload = """
                {"client_order_id":"%s","symbol":"WASH01","side":"%s",
                 "price":%s,"qty":1000}
                """.formatted(clientOrderId, side, price);
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
