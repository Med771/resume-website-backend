package ru.ai.sin.logic.student;

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
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class StudentControllerMvcTest {

    private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String FILTER_JSON = "{}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    private static StudentDTO sampleStudentDto() {
        return new StudentDTO(
                STUDENT_ID,
                "City",
                "https://hh.ru",
                LocalDate.of(2000, 1, 1),
                "Bio",
                null,
                CourseEnum.FIRST,
                BusynessEnum.EMPLOYED,
                "Ivan",
                "Petrov",
                null,
                null,
                "ivan@test.ru",
                null,
                null,
                1L,
                "Spec",
                List.of(new SkillDTO(1L, "Java")),
                false,
                true,
                0,
                null
        );
    }

    @Test
    void getMe_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/student/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getMe_forbiddenForRecruiterRole() throws Exception {
        mockMvc.perform(get("/student/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMe_okWhenLinked() throws Exception {
        when(studentService.getLinkedForCurrentUser()).thenReturn(Optional.of(sampleStudentDto()));

        mockMvc.perform(get("/student/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getById_okForStudent() throws Exception {
        when(studentService.getById(STUDENT_ID)).thenReturn(sampleStudentDto());

        mockMvc.perform(get("/student/{id}", STUDENT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getById_okForRecruiter() throws Exception {
        when(studentService.getById(STUDENT_ID)).thenReturn(sampleStudentDto());

        mockMvc.perform(get("/student/{id}", STUDENT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void cardsFilter_okForGuest() throws Exception {
        when(studentService.getAllCardsByFilter(any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(post("/student/cardsFilter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FILTER_JSON)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void cardsFilter_okForStudent() throws Exception {
        when(studentService.getAllCardsByFilter(any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(post("/student/cardsFilter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FILTER_JSON)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void filter_okForUser() throws Exception {
        when(studentService.getAllByFilter(any(), any()))
                .thenReturn(new PageResponse<>(List.of(sampleStudentDto()), 0, 20, 1, 1));

        mockMvc.perform(post("/student/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FILTER_JSON)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_forbiddenForNonAdmin() throws Exception {
        String body = """
                {"city":"C","hhLink":"https://h","birthDate":"2000-01-01","bio":null,"course":"FIRST","busyness":"EMPLOYED",\
                "firstName":"A","lastName":"B","email":"a@b.c","phoneNumber":"+79001234567","telegramUsername":"tg",\
                "specialityId":1,"skillsIds":[1],"username":"stu_user","password":"pass1234"}""";

        mockMvc.perform(post("/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_created() throws Exception {
        when(studentService.create(any())).thenReturn(sampleStudentDto());

        String body = """
                {"city":"C","hhLink":"https://h","birthDate":"2000-01-01","bio":null,"course":"FIRST","busyness":"EMPLOYED",\
                "firstName":"A","lastName":"B","email":"a@b.c","phoneNumber":"+79001234567","telegramUsername":"tg",\
                "specialityId":1,"skillsIds":[1],"username":"stu_user","password":"pass1234"}""";

        mockMvc.perform(post("/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void setPhoto_okForStudent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatarFile", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/student/photo/{id}", STUDENT_ID).file(file).with(csrf()))
                .andExpect(status().isNoContent());

        verify(studentService).setPhoto(STUDENT_ID, file);
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void setPhoto_forbiddenForRecruiter() throws Exception {
        MockMultipartFile file = new MockMultipartFile("avatarFile", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/student/photo/{id}", STUDENT_ID).file(file).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void delete_forbiddenForUser() throws Exception {
        mockMvc.perform(delete("/student/{id}", STUDENT_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_noContent() throws Exception {
        mockMvc.perform(delete("/student/{id}", STUDENT_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(studentService).deleteById(STUDENT_ID);
    }
}
