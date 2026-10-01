package ru.ai.sin.logic.siteproject;

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
import ru.ai.sin.logic.siteproject.dto.FilterSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.SiteProjectDTO;
import ru.ai.sin.logic.siteproject.dto.SiteProjectStudentsReq;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class SiteProjectControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteProjectService siteProjectService;

    @MockitoBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void filter_unauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post("/projects/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void filter_okForRecruiter() throws Exception {
        UUID id = UUID.randomUUID();
        when(siteProjectService.filter(any())).thenReturn(List.of(
                new SiteProjectDTO(id, "T", null, null, null, List.of(), List.of(), 0, false, null, null, List.of())
        ));
        mockMvc.perform(post("/projects/filter")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        verify(siteProjectService).filter(new FilterSiteProjectReq(null, null, null));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void filter_okForStudent() throws Exception {
        when(siteProjectService.filter(any())).thenReturn(List.of());
        mockMvc.perform(post("/projects/filter")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filter_okForAdmin() throws Exception {
        when(siteProjectService.filter(any())).thenReturn(List.of());
        mockMvc.perform(post("/projects/filter")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getById_okForStudent() throws Exception {
        UUID id = UUID.randomUUID();
        when(siteProjectService.getById(id)).thenReturn(
                new SiteProjectDTO(id, "T", null, null, null, List.of(), List.of(), 0, true, null, null, null)
        );
        mockMvc.perform(get("/projects/" + id).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void create_forbiddenForStudent() throws Exception {
        mockMvc.perform(post("/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"visibleToAnonymous\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void create_forbiddenForRecruiter() throws Exception {
        mockMvc.perform(post("/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"visibleToAnonymous\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_okForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(siteProjectService.create(any())).thenReturn(
                new SiteProjectDTO(id, "T", null, null, null, List.of(), List.of(), 0, true, null, null, null)
        );
        mockMvc.perform(post("/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"visibleToAnonymous\":true}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_okForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(siteProjectService.update(any(), any())).thenReturn(
                new SiteProjectDTO(id, "T2", null, null, null, List.of(), List.of(), 0, true, null, null, null)
        );
        mockMvc.perform(put("/projects/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T2\",\"visibleToAnonymous\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void delete_forbiddenForRecruiter() throws Exception {
        mockMvc.perform(delete("/projects/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_okForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/projects/" + id).with(csrf()))
                .andExpect(status().isNoContent());
        verify(siteProjectService).delete(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reorder_okForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/projects/reorder")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\":[\"" + id + "\"]}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listStudents_ok() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(siteProjectService.listStudentIds(projectId)).thenReturn(List.of(studentId));

        mockMvc.perform(get("/projects/" + projectId + "/students").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(studentId.toString()));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void listStudents_forbiddenForStudent() throws Exception {
        mockMvc.perform(get("/projects/" + UUID.randomUUID() + "/students").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bindStudents_ok() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        mockMvc.perform(post("/projects/" + projectId + "/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isNoContent());

        verify(siteProjectService).bindStudents(projectId, new SiteProjectStudentsReq(List.of(studentId)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unbindStudents_ok() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/" + projectId + "/students")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isNoContent());

        verify(siteProjectService).unbindStudents(projectId, new SiteProjectStudentsReq(List.of(studentId)));
    }
}
