package ru.ai.sin.logic.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class RequestControllerMvcTest {

    private static final String JSON_STUDENT_ID = "{\"studentId\":\"22222222-2222-2222-2222-222222222222\"}";
    private static final String JSON_DECISION = "{\"accept\":true}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @MockBean
    private TuDecisionService tuDecisionService;

    /** Не поднимаем реальный JWT-фильтр и JwtHelper в срезе WebMvc. */
    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    @WithMockUser(roles = "STUDENT")
    void create_forbiddenForStudentRole() throws Exception {
        mockMvc.perform(post("/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_STUDENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_allowedForUserRole() throws Exception {
        mockMvc.perform(post("/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_STUDENT_ID)
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_allowedForGuestRole() throws Exception {
        mockMvc.perform(post("/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_STUDENT_ID)
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentDecision_noContentForStudent() throws Exception {
        mockMvc.perform(post("/request/1/student-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_DECISION)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(requestService).studentRespond(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void studentDecision_forbiddenForRecruiterUserRole() throws Exception {
        mockMvc.perform(post("/request/1/student-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_DECISION)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
