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
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.company.CompanyController;
import ru.ai.sin.logic.company.CompanyService;
import ru.ai.sin.logic.company.dto.CompanyDTO;
import ru.ai.sin.logic.education.EducationController;
import ru.ai.sin.logic.education.EducationService;
import ru.ai.sin.logic.education.dto.EducationDTO;
import ru.ai.sin.logic.experience.ExperienceController;
import ru.ai.sin.logic.experience.ExperienceService;
import ru.ai.sin.logic.experience.dto.ExperienceDTO;
import ru.ai.sin.logic.experience.dto.ExperienceRes;
import ru.ai.sin.logic.institution.InstitutionController;
import ru.ai.sin.logic.institution.InstitutionService;
import ru.ai.sin.logic.institution.dto.InstitutionDTO;
import ru.ai.sin.logic.institution.dto.InstitutionRes;
import ru.ai.sin.logic.portfolio.PortfolioController;
import ru.ai.sin.logic.portfolio.PortfolioService;
import ru.ai.sin.logic.portfolio.dto.PortfolioDTO;
import ru.ai.sin.logic.skill.SkillController;
import ru.ai.sin.logic.skill.SkillService;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        ExperienceController.class,
        InstitutionController.class,
        PortfolioController.class,
        SkillController.class,
        CompanyController.class,
        EducationController.class,
        StudentController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class StudentResumeAccessMvcTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExperienceService experienceService;
    @MockitoBean
    private InstitutionService institutionService;
    @MockitoBean
    private PortfolioService portfolioService;
    @MockitoBean
    private SkillService skillService;
    @MockitoBean
    private CompanyService companyService;
    @MockitoBean
    private EducationService educationService;
    @MockitoBean
    private StudentService studentService;
    @MockitoBean
    private SecurityHelper securityHelper;
    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCreateExperience_created() throws Exception {
        when(experienceService.create(any())).thenReturn(new ExperienceDTO(
                1L, STUDENT_ID, new ExperienceRes(1L, "Dev", null, LocalDate.of(2020, 1, 1), null)));

        mockMvc.perform(post("/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":1,"studentId":"22222222-2222-2222-2222-222222222222",\
                                "position":"Dev","startDate":"2020-01-01"}""")
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCreateInstitution_created() throws Exception {
        when(institutionService.create(any())).thenReturn(
                new InstitutionDTO(1L, STUDENT_ID, new InstitutionRes(1L, 2020, 2024)));

        mockMvc.perform(post("/institution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"educationId\":1,\"startYear\":2020,\"endYear\":2024}")
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCreatePortfolio_created() throws Exception {
        when(portfolioService.create(any())).thenReturn(
                new PortfolioDTO(1L, "Site", "https://ex.com", null, STUDENT_ID));

        mockMvc.perform(post("/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Site\",\"link\":\"https://ex.com\"}")
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCreateExperience_forbidden() throws Exception {
        mockMvc.perform(post("/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":1,\"position\":\"Dev\",\"startDate\":\"2020-01-01\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentPostEducation_forbidden() throws Exception {
        mockMvc.perform(post("/education")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institution\":\"MSU\",\"additionalInfo\":\"info\",\"webUrl\":\"https://msu.ru\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentPostCompany_forbidden() throws Exception {
        mockMvc.perform(post("/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentPostSkill_forbidden() throws Exception {
        mockMvc.perform(post("/skill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentFilterSkill_ok() throws Exception {
        when(skillService.getAllByFilter(any(), any()))
                .thenReturn(new PageResponse<>(List.of(new SkillDTO(1L, "Java")), 0, 20, 1, 1));

        mockMvc.perform(post("/skill/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentFilterCompany_ok() throws Exception {
        when(companyService.getAllByFilter(any(), any()))
                .thenReturn(new PageResponse<>(List.of(new CompanyDTO(1L, "Acme", List.of())), 0, 20, 1, 1));

        mockMvc.perform(post("/company/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentFilterEducation_ok() throws Exception {
        when(educationService.getAllByFilter(any(), any()))
                .thenReturn(new PageResponse<>(
                        List.of(new EducationDTO(1L, "MSU", "info", "https://msu.ru")), 0, 20, 1, 1));

        mockMvc.perform(post("/education/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentOnboarding_noMapping() throws Exception {
        mockMvc.perform(post("/student/onboarding/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(NoResourceFoundException.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void patchMe_ok() throws Exception {
        when(studentService.patchMe(any())).thenReturn(new StudentDTO(
                STUDENT_ID, "City", null, null, "bio", null,
                CourseEnum.FIRST, BusynessEnum.FREE, "A", "B", null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "Java")),
                false, false, 0, null));

        mockMvc.perform(patch("/student/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"bio\",\"skillsIds\":[1]}")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(studentService).patchMe(any());
    }
}
