package ru.ai.sin.logic.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.exception.GlobalExceptionHandler;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecruitmentRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({MethodSecurityTestConfig.class, GlobalExceptionHandler.class})
class RecruitmentRequestControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecruitmentRequestService recruitmentRequestService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void create_unauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post("/recruitment-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void create_forbiddenForStudent() throws Exception {
        mockMvc.perform(post("/recruitment-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getById_notFoundWhenServiceHidesForeignRequest() throws Exception {
        UUID id = UUID.randomUUID();
        when(recruitmentRequestService.getById(id)).thenThrow(new NotFoundException("Заявка не найдена"));

        mockMvc.perform(get("/recruitment-requests/" + id).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private static String body() {
        return """
                {"tasks":"t","qualificationRequirements":"q","workConditions":"w","studentCount":1,"specialityIds":[1],"cityIds":[1]}
                """;
    }
}
