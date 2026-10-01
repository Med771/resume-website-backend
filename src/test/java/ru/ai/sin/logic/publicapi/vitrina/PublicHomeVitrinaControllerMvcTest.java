package ru.ai.sin.logic.publicapi.vitrina;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.logic.publicapi.vitrina.dto.PublicHomeVitrinaDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.enums.CourseEnum;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicHomeVitrinaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class PublicHomeVitrinaControllerMvcTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicHomeVitrinaService publicHomeVitrinaService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void getHome_ok() throws Exception {
        when(publicHomeVitrinaService.getHome()).thenReturn(new PublicHomeVitrinaDTO(
                List.of(new StudentCardDTO(
                        STUDENT_ID,
                        "Bio",
                        "Ann",
                        "Smith",
                        null,
                        null,
                        CourseEnum.FIRST,
                        "CS",
                        List.of(new SkillDTO(1L, "Go")),
                        null
                )),
                List.of()
        ));

        mockMvc.perform(get("/public/vitrina/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students[0].firstName").value("Ann"))
                .andExpect(jsonPath("$.projects").isArray());
    }
}
