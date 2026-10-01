package ru.ai.sin.logic.vacancy;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VacancyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class VacancyControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VacancyService vacancyService;

    @MockBean
    private VacancyApplicationService vacancyApplicationService;

    @MockBean
    private VacancyApplicationTuDecisionService vacancyApplicationTuDecisionService;

    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    @WithMockUser(roles = "STUDENT")
    void list_allowedForStudent() throws Exception {
        mockMvc.perform(get("/vacancies").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/vacancies").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void submitForReview_forbiddenForStudent() throws Exception {
        mockMvc.perform(post("/vacancies/11111111-1111-1111-1111-111111111111/submit-for-review")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_allowedForGuest() throws Exception {
        mockMvc.perform(post("/vacancies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","description":"long enough description here"}
                                """)
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void apply_allowedForStudent() throws Exception {
        mockMvc.perform(post("/vacancies/11111111-1111-1111-1111-111111111111/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isCreated());
    }
}
