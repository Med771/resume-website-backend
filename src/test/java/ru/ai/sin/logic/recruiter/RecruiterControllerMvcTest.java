package ru.ai.sin.logic.recruiter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;
import ru.ai.sin.logic.recruiter.dto.PatchRecruiterReq;
import ru.ai.sin.logic.recruiter.dto.RecruiterDTO;
import ru.ai.sin.models.enums.CooperationFormat;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecruiterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class RecruiterControllerMvcTest {

    private static final UUID REC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecruiterService recruiterService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    private static RecruiterDTO sampleRecruiter() {
        return new RecruiterDTO(
                REC_ID,
                "ACME",
                "Moscow",
                "Ann",
                "Bee",
                "ann_user",
                "ann@acme.test",
                "+79990001122",
                "ann_tg",
                null,
                null,
                null,
                null
        );
    }

    @Test
    void getMe_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/recruiter/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMe_forbiddenForStudent() throws Exception {
        mockMvc.perform(get("/recruiter/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getMe_okWhenLinked() throws Exception {
        when(recruiterService.getLinkedForCurrentUser()).thenReturn(Optional.of(sampleRecruiter()));

        mockMvc.perform(get("/recruiter/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cooperationFormat").value(nullValue()))
                .andExpect(jsonPath("$.readyToRequestStudents").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getMe_notFoundWhenUnlinked() throws Exception {
        when(recruiterService.getLinkedForCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/recruiter/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getById_ok() throws Exception {
        when(recruiterService.getById(REC_ID)).thenReturn(sampleRecruiter());

        mockMvc.perform(get("/recruiter/{id}", REC_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_forbiddenForUser() throws Exception {
        mockMvc.perform(post("/recruiter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"X\",\"firstName\":\"a\",\"lastName\":\"b\",\"email\":\"a@b.c\",\"phoneNumber\":\"+1\",\"telegramUsername\":\"t\"}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_created() throws Exception {
        when(recruiterService.create(any())).thenReturn(sampleRecruiter());

        mockMvc.perform(post("/recruiter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"X\",\"firstName\":\"a\",\"lastName\":\"b\",\"email\":\"a@b.c\",\"phoneNumber\":\"+1\",\"telegramUsername\":\"t\"}")
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(recruiterService).create(any());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void filter_forbiddenForUser() throws Exception {
        mockMvc.perform(post("/recruiter/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void patch_oldBodyWithoutNewFields_ok() throws Exception {
        when(recruiterService.patch(eq(REC_ID), any())).thenReturn(sampleRecruiter());

        mockMvc.perform(patch("/recruiter/{id}", REC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"X\"}")
                        .with(csrf()))
                .andExpect(status().isOk());

        ArgumentCaptor<PatchRecruiterReq> captor = ArgumentCaptor.forClass(PatchRecruiterReq.class);
        verify(recruiterService).patch(eq(REC_ID), captor.capture());
        assertThat(captor.getValue().companyName()).isEqualTo("X");
        assertThat(captor.getValue().cooperationFormat()).isNull();
        assertThat(captor.getValue().readyToRequestStudents()).isNull();
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void patch_newFieldsReachService() throws Exception {
        when(recruiterService.patch(eq(REC_ID), any())).thenReturn(sampleRecruiter());

        mockMvc.perform(patch("/recruiter/{id}", REC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cooperationFormat\":\"INTERNSHIP\",\"readyToRequestStudents\":true}")
                        .with(csrf()))
                .andExpect(status().isOk());

        ArgumentCaptor<PatchRecruiterReq> captor = ArgumentCaptor.forClass(PatchRecruiterReq.class);
        verify(recruiterService).patch(eq(REC_ID), captor.capture());
        assertThat(captor.getValue().cooperationFormat()).isEqualTo(CooperationFormat.INTERNSHIP);
        assertThat(captor.getValue().readyToRequestStudents()).isTrue();
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void patch_unknownFormat_badRequest() throws Exception {
        mockMvc.perform(patch("/recruiter/{id}", REC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cooperationFormat\":\"NOPE\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(recruiterService, never()).patch(any(), any());
    }
}
