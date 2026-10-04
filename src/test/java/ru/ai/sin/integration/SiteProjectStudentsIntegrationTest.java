package ru.ai.sin.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SiteProjectStudentsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void bindListAndUnbindStudents_onRealPostgres() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");

        long specialityId = createSpeciality(adminCookies, "ProjSpec-" + suffix);
        long skillId = createSkill(adminCookies, "ProjSkill-" + suffix);
        UUID studentId = createStudent(adminCookies, "proj_stu_" + suffix + "@integration.test", specialityId, skillId);

        UUID projectId = createSiteProject(adminCookies, "Project-" + suffix);

        mockMvc.perform(get("/projects/" + projectId + "/students").cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).isEqualTo("[]"));

        mockMvc.perform(post("/projects/" + projectId + "/students")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isNoContent());

        MvcResult listResult = mockMvc.perform(get("/projects/" + projectId + "/students")
                        .cookie(adminCookies))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ids = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(ids).hasSize(1);
        assertThat(ids.get(0).asText()).isEqualTo(studentId.toString());

        mockMvc.perform(post("/projects/" + projectId + "/students")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/projects/" + projectId + "/students")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + studentId + "\"]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/" + projectId + "/students").cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).isEqualTo("[]"));
    }

    private Cookie[] login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn();
        return java.util.Arrays.stream(r.getResponse().getCookies())
                .filter(c -> "ACCESS_TOKEN".equals(c.getName()) || "REFRESH_TOKEN".equals(c.getName()))
                .toArray(Cookie[]::new);
    }

    private long createSpeciality(Cookie[] cookies, String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/speciality")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createSkill(Cookie[] cookies, String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/skill")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private UUID createStudent(Cookie[] cookies, String email, long specialityId, long skillId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", "Moscow");
        body.put("hhLink", "https://hh.ru/member");
        body.put("birthDate", "2000-05-05");
        body.put("bio", "integration student");
        body.put("course", "FIRST");
        body.put("busyness", "FREE");
        body.put("firstName", "Proj");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("phoneNumber", "+79991234567");
        body.put("telegramUsername", "projstu");
        body.put("specialityId", specialityId);
        body.put("skillsIds", List.of(skillId));

        MvcResult r = mockMvc.perform(post("/student")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createSiteProject(Cookie[] cookies, String title) throws Exception {
        MvcResult r = mockMvc.perform(post("/projects")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"visibleToAnonymous\":false}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }
}
