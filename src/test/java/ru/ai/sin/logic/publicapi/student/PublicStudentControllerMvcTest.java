package ru.ai.sin.logic.publicapi.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.CourseEnum;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicStudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class PublicStudentControllerMvcTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicCatalogStudentService publicCatalogStudentService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void getById_ok() throws Exception {
        when(publicCatalogStudentService.getCardById(ID)).thenReturn(sampleCard());
        mockMvc.perform(get("/public/students/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void listCards_okWithoutBody() throws Exception {
        when(publicCatalogStudentService.listCards(any(), any())).thenReturn(
                new PageResponse<>(List.of(sampleCard()), 0, 20, 1, 1));
        mockMvc.perform(post("/public/students/cards?page=0&size=20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].firstName").value("Ann"));
    }

    private static StudentCardDTO sampleCard() {
        return new StudentCardDTO(
                ID,
                "Bio",
                "Ann",
                "Smith",
                null,
                null,
                CourseEnum.FIRST,
                "CS",
                List.of(new SkillDTO(1L, "Go")),
                null
        );
    }
}
