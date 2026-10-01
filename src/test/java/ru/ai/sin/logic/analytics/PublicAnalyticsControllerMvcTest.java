package ru.ai.sin.logic.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicAnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class PublicAnalyticsControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void ingest_noContent() throws Exception {
        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PAGE_VIEW\",\"path\":\"/home\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ingest_badEventType() throws Exception {
        doThrow(new ru.ai.sin.exception.models.BadRequestException("Unknown eventType: X"))
                .when(analyticsService).recordEvent(any(), any());
        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"X\",\"path\":\"/\"}"))
                .andExpect(status().isBadRequest());
    }
}
