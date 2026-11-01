package com.tradeguard.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthPathServesBrandedStatusPage() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("API 正常回應")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("style-src 'unsafe-inline'")));
    }

    @Test
    void healthStatusPathRemainsMachineReadable() throws Exception {
        mockMvc.perform(get("/api/health/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"status":"UP"}
                        """));
    }

    @Test
    void readinessPathServesBrandedPageToBrowsers() throws Exception {
        mockMvc.perform(get("/api/health/readiness").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("PostgreSQL")))
                .andExpect(content().string(containsString("/favicon.ico")))
                .andExpect(header().string("Content-Security-Policy",
                        containsString("connect-src 'self'")));
    }

    @Test
    void readinessPathRemainsMachineReadableWhenRequestedAsJson() throws Exception {
        mockMvc.perform(get("/api/health/readiness").accept(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 200
                                || result.getResponse().getStatus() == 503))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", anyOf(is("UP"), is("DOWN"))))
                .andExpect(jsonPath("$.checks.api").value("UP"))
                .andExpect(jsonPath("$.checks.database", anyOf(is("UP"), is("DOWN"))))
                .andExpect(jsonPath("$.checks.redis", anyOf(is("UP"), is("DOWN"))));
    }

    @Test
    void faviconIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/x-icon")));
    }
}
