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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VacancyAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class VacancyAdminControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VacancyModerationAdminService vacancyModerationAdminService;

    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void filter_allowedForAdmin() throws Exception {
        mockMvc.perform(post("/admin/vacancies/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void filter_forbiddenForGuest() throws Exception {
        mockMvc.perform(post("/admin/vacancies/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
