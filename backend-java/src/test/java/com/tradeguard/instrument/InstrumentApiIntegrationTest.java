package com.tradeguard.instrument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class InstrumentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM instruments");
    }

    @Test
    void createReturns201AndLocationThenCanBeRead() throws Exception {
        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"TEST01","name":"測試商品","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/market/instruments/TEST01")))
                .andExpect(jsonPath("$.symbol").value("TEST01"))
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/api/market/TEST01/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("測試商品"));
    }

    @Test
    void createRejectsInvalidAndDuplicateSymbolsWithConsistentErrors() throws Exception {
        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"lowercase","name":"","active":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.symbol").exists());

        String request = """
                {"symbol":"TEST01","name":"測試商品","active":true}
                """;
        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/market/instruments"));
    }

    @Test
    void supportsPaginationPutPatchAndDelete() throws Exception {
        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"TEST01","name":"測試商品","active":true}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/market/instruments?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/market/instruments/TEST01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"TEST01","name":"更新名稱","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新名稱"));

        mockMvc.perform(patch("/api/market/instruments/TEST01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.name").value("更新名稱"));

        mockMvc.perform(delete("/api/market/instruments/TEST01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TEST01"));

        mockMvc.perform(get("/api/market/instruments/TEST01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsEmptyPatchAndInvalidSortWith400() throws Exception {
        mockMvc.perform(post("/api/market/instruments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"symbol":"TEST01","name":"測試商品","active":true}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/market/instruments/TEST01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/market/instruments?sort=missingField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
