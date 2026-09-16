package ru.ai.sin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные проверки новых публичных эндпоинтов на PostgreSQL (Testcontainers).
 */
@AutoConfigureMockMvc
class NewFeaturesIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAnalytics_event_noAuth_returns204() throws Exception {
        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PAGE_VIEW\",\"path\":\"/it-test\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void publicVitrinaHome_list_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/public/vitrina/home"))
                .andExpect(status().isOk());
    }
}
