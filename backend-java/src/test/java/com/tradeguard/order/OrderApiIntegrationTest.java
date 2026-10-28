package com.tradeguard.order;

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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest {

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

    @Autowired
    private OrderEventReplayer eventReplayer;

    @MockitoBean
    private AuthStateStore authStateStore;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("DELETE FROM order_events");
        jdbcTemplate.execute("DELETE FROM trades");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM instruments");
        jdbcTemplate.execute("DELETE FROM app_users");
        when(authStateStore.isTokenRevoked(anyString())).thenReturn(false);
        when(authStateStore.isLoginLocked(anyString(), anyInt())).thenReturn(false);
        when(authStateStore.recordLoginFailure(anyString(), anyInt(), any()))
                .thenReturn(false);
        createUser("trader1");
        createUser("trader2");
        instruments.saveAndFlush(new Instrument("TEST01", "測試商品", true,
                new BigDecimal("50.00")));
    }

    @Test
    void matchesBestPriceThenTimeAndRecordsMakerPriceTrades() throws Exception {
        long firstSell = orderId(createOrder("trader1", "TEST01", "S", "50.00", 1000,
                UUID.randomUUID()));
        long secondSell = orderId(createOrder("trader2", "TEST01", "S", "49.00", 1000,
                UUID.randomUUID()));
        MvcResult tieSellResult = createOrder("trader2", "TEST01", "S", "49.00", 1000,
                UUID.randomUUID());
        long tieSellId = orderId(tieSellResult);

        MvcResult buyResult = createOrder("trader1", "TEST01", "B", "51.00", 3000,
                UUID.randomUUID());
        long buyId = orderId(buyResult);
        org.junit.jupiter.api.Assertions.assertTrue(firstSell < secondSell);
        assertResponseBody(buyResult, "\"status\":\"FILLED\"");

        mockMvc.perform(get("/api/market/TEST01/trades?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].price").value(50.0))
                .andExpect(jsonPath("$.content[2].price").value(49.0))
                .andExpect(jsonPath("$.content[1].sellOrderId").value(tieSellId));

        mockMvc.perform(get("/api/orders/mine").with(user("trader1").roles("TRADER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].order_id").value(buyId))
                .andExpect(jsonPath("$.content[0].status").value("FILLED"))
                .andExpect(jsonPath("$.content[0].filled_qty").value(3000))
                .andExpect(jsonPath("$.content[0].seq_no").value(4));
    }

    @Test
    void supportsPartialFillsCancellationAndIdempotentRetries() throws Exception {
        UUID clientOrderId = UUID.randomUUID();
        MvcResult initialSell = createOrder("trader2", "TEST01", "S", "50.00", 2000,
                UUID.randomUUID());
        long sellId = orderId(initialSell);

        MvcResult firstBuy = createOrder("trader1", "TEST01", "B", "50.00", 5000,
                clientOrderId);
        long buyId = orderId(firstBuy);
        assertResponseBody(firstBuy, "\"status\":\"PARTIALLY_FILLED\"");
        assertResponseBody(firstBuy, "\"filled_qty\":2000");
        assertResponseBody(firstBuy, "\"remaining_qty\":3000");

        MvcResult retry = createOrder("trader1", "TEST01", "B", "50.00", 5000,
                clientOrderId);
        org.junit.jupiter.api.Assertions.assertEquals(buyId, orderId(retry));

        mockMvc.perform(delete("/api/orders/{id}", buyId)
                        .with(user("trader1").roles("TRADER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.filled_qty").value(2000))
                .andExpect(jsonPath("$.remaining_qty").value(3000));

        mockMvc.perform(delete("/api/orders/{id}", sellId)
                        .with(user("trader1").roles("TRADER")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/market/TEST01/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].qty").value(2000));

        mockMvc.perform(delete("/api/orders/{id}", buyId)
                        .with(user("trader1").roles("TRADER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesLotsTicksPriceLimitsAndInstrumentAvailability() throws Exception {
        mockMvc.perform(createRequest("trader1", "TEST01", "B", "50.00", 500,
                        UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("交易單位")));

        mockMvc.perform(createRequest("trader1", "TEST01", "B", "50.05", 1000,
                        UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("升降單位")));

        mockMvc.perform(createRequest("trader1", "TEST01", "B", "55.50", 1000,
                        UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("漲跌幅")));

        mockMvc.perform(createRequest("trader1", "UNKNOWN", "B", "50.00", 1000,
                        UUID.randomUUID()))
                .andExpect(status().isNotFound());

        mockMvc.perform(createRequest("trader1", "TEST01", "B", "55.00", 1000,
                        UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.seq_no").value(1));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_events", Long.class));
    }

    @Test
    void repeatedClientIdWithDifferentPayloadConflictsAndSideMustBeValid() throws Exception {
        UUID clientOrderId = UUID.randomUUID();
        MvcResult initial = createOrder("trader1", "TEST01", "B", "50.00", 1000,
                clientOrderId);
        long orderId = orderId(initial);

        mockMvc.perform(createRequest("trader1", "TEST01", "B", "50.00", 2000,
                        clientOrderId))
                .andExpect(status().isConflict());

        mockMvc.perform(createRequest("trader1", "TEST01", "X", "50.00", 1000,
                        UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/mine").with(user("trader1").roles("TRADER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].order_id").value(orderId));
    }

    @Test
    void replaysAppendOnlyEventsIntoOpenOrderBookAndChecksDatabaseProjection()
            throws Exception {
        createOrder("trader2", "TEST01", "S", "50.00", 2000, UUID.randomUUID());
        createOrder("trader1", "TEST01", "B", "50.00", 1000, UUID.randomUUID());
        long canceledId = orderId(createOrder("trader1", "TEST01", "S", "51.00",
                1000, UUID.randomUUID()));
        mockMvc.perform(delete("/api/orders/{id}", canceledId)
                        .with(user("trader1").roles("TRADER")))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(5,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_events", Long.class));
        var recovered = eventReplayer.replay();
        org.junit.jupiter.api.Assertions.assertEquals(1, recovered.get("TEST01").size());
        var openOrder = recovered.get("TEST01").get(0);
        org.junit.jupiter.api.Assertions.assertEquals(1, openOrder.seqNo());
        org.junit.jupiter.api.Assertions.assertEquals(1000, openOrder.remainingQuantity());
        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PARTIALLY_FILLED,
                openOrder.status());
        org.junit.jupiter.api.Assertions.assertEquals(3,
                jdbcTemplate.queryForObject(
                        "SELECT last_seq_no FROM instruments WHERE symbol = 'TEST01'",
                        Long.class));
    }

    @Test
    void replayFailsClosedWhenAnAcceptedOrderEventIsMissing() throws Exception {
        createOrder("trader1", "TEST01", "B", "50.00", 1000, UUID.randomUUID());
        jdbcTemplate.update("DELETE FROM order_events WHERE event_type = 'ORDER_ACCEPTED'");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                eventReplayer::replay);
    }

    private MvcResult createOrder(String username, String symbol, String side,
                                 String price, long quantity, UUID clientOrderId)
            throws Exception {
        return mockMvc.perform(createRequest(username, symbol, side, price, quantity,
                        clientOrderId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        matchesPattern(".*/api/orders/[0-9]+")))
                .andReturn();
    }

    private org.springframework.test.web.servlet.request
            .MockHttpServletRequestBuilder createRequest(
            String username, String symbol, String side, String price,
            long quantity, UUID clientOrderId) {
        return post("/api/orders")
                .with(user(username).roles("TRADER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"client_order_id":"%s","symbol":"%s","side":"%s",
                         "price":%s,"qty":%d}
                        """.formatted(clientOrderId, symbol, side, price, quantity));
    }

    private long orderId(MvcResult result) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("order_id").asLong();
    }

    private void assertResponseBody(MvcResult result, String expected) throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(result.getResponse()
                .getContentAsString().contains(expected));
    }

    private void createUser(String username) {
        users.saveAndFlush(new AppUser(username,
                passwordEncoder.encode("correct-horse-battery-12"), Role.TRADER));
    }
}
