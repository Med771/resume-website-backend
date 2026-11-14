package ru.ai.sin.logic.vacancy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VacancyAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class VacancyAdminControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VacancyModerationAdminService vacancyModerationAdminService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/admin/vacancies?page=0&size=20"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void list_forbiddenForRecruiter() throws Exception {
        mockMvc.perform(get("/admin/vacancies?page=0&size=20"))
                .andExpect(status().isForbidden());
    }
}
