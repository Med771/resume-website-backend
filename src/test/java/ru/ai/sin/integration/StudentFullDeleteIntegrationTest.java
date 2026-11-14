package ru.ai.sin.integration;

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
class StudentFullDeleteIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deleteStudent_freesLoginAndEmail() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "dstu_" + suffix + "@integration.test";
        String username = "dstu_" + suffix;
        String password = "DelStuPass12";

        Cookie[] adminCookies = login("admin", "admin");

        long specialityId = createSpeciality(adminCookies, "DelSpec-" + suffix);
        long skillId = createSkill(adminCookies, "DelSkill-" + suffix);
        UUID studentId = createStudent(adminCookies, email, username, password, specialityId, skillId);

        mockMvc.perform(delete("/student/" + studentId).cookie(adminCookies))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/student/" + studentId).cookie(adminCookies))
                .andExpect(status().isNotFound());

        UUID recreatedId = createStudent(adminCookies, email, username, password, specialityId, skillId);
        assertThat(recreatedId).isNotEqualTo(studentId);

        Cookie[] recreatedCookies = login(username, password);
        mockMvc.perform(get("/student/me").cookie(recreatedCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains(recreatedId.toString()));
    }

    private Cookie[] login(String username, String password) throws Exception {
        String path = "admin".equals(username) ? "/auth/admin/login" : "/auth/login";
        MvcResult r = mockMvc.perform(post(path)
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

    private UUID createStudent(
            Cookie[] cookies,
            String email,
            String username,
            String password,
            long specialityId,
            long skillId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", "Moscow");
        body.put("hhLink", "https://hh.ru/member");
        body.put("birthDate", "2000-05-05");
        body.put("bio", "full delete student");
        body.put("course", "FIRST");
        body.put("busyness", "FREE");
        body.put("firstName", "Del");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("phoneNumber", "+7901" + String.format("%07d", Math.abs(email.hashCode()) % 10_000_000));
        body.put("telegramUsername", "dstu" + username.substring(Math.max(0, username.length() - 8)));
        body.put("specialityId", specialityId);
        body.put("skillsIds", List.of(skillId));
        body.put("username", username);
        body.put("password", password);

        MvcResult r = mockMvc.perform(post("/student")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }
}
