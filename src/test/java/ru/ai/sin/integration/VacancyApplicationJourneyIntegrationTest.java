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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class VacancyApplicationJourneyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void draft_submit_approve_apply_accept_chat() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        Cookie[] guestCookies = login("guest", "guest");

        long specialityId = createSpeciality(adminCookies, "VacSpec-" + suffix);
        long skillId = createSkill(adminCookies, "VacSkill-" + suffix);
        UUID studentId = createStudent(adminCookies, "vac_stu_" + suffix + "@integration.test", specialityId, skillId);
        createStudentUser(adminCookies, "vac_stu_" + suffix, "vac-stu-pass", studentId);
        Cookie[] studentCookies = login("vac_stu_" + suffix, "vac-stu-pass");

        mockMvc.perform(post("/request")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestCreateJson("vac_rec_" + suffix + "@integration.test", studentId)))
                .andExpect(status().isCreated());

        String vacancyBody = """
                {
                  "title": "Java Intern",
                  "description": "We are looking for a motivated intern with basic Java skills and teamwork.",
                  "city": "Moscow",
                  "workFormat": "REMOTE",
                  "employmentType": "INTERNSHIP",
                  "specialityId": %d,
                  "skillIds": [%d]
                }
                """.formatted(specialityId, skillId);

        MvcResult createResult = mockMvc.perform(post("/vacancies")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vacancyBody))
                .andExpect(status().isCreated())
                .andReturn();
        UUID vacancyId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        assertThat(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("status").asText())
                .isEqualTo("DRAFT");

        mockMvc.perform(post("/vacancies/" + vacancyId + "/submit-for-review").cookie(guestCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("PENDING_REVIEW"));

        mockMvc.perform(post("/admin/vacancies/" + vacancyId + "/approve").cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("PUBLISHED"));

        mockMvc.perform(get("/vacancies?page=0&size=20").cookie(studentCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains(vacancyId.toString()));

        MvcResult applyResult = mockMvc.perform(post("/vacancies/" + vacancyId + "/applications")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverLetter\":\"Interested\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID applicationId = UUID.fromString(
                objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult acceptResult = mockMvc.perform(
                        post("/vacancies/" + vacancyId + "/applications/" + applicationId + "/accept")
                                .cookie(guestCookies))
                .andExpect(status().isOk())
                .andReturn();
        UUID chatId = UUID.fromString(
                objectMapper.readTree(acceptResult.getResponse().getContentAsString()).get("appChatId").asText());

        mockMvc.perform(post("/chat/" + chatId + "/messages")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Welcome aboard\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/chat/" + chatId + "/messages")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Thank you\"}"))
                .andExpect(status().isCreated());
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
        body.put("bio", "vacancy journey student");
        body.put("course", "FIRST");
        body.put("busyness", "FREE");
        body.put("firstName", "Vac");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("phoneNumber", "+79991234568");
        body.put("telegramUsername", "vacstu");
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

    private void createStudentUser(Cookie[] cookies, String username, String password, UUID studentId)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Vac User");
        body.put("username", username);
        body.put("password", password);
        body.put("role", "STUDENT");
        body.put("studentId", studentId.toString());

        mockMvc.perform(post("/user")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private String requestCreateJson(String recruiterEmail, UUID studentId) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("companyName", "VacCorp");
        m.put("firstName", "Rec");
        m.put("lastName", "Ruiter");
        m.put("email", recruiterEmail);
        m.put("phoneNumber", "+78881112234");
        m.put("telegramUsername", "vacrec");
        m.put("studentId", studentId.toString());
        return objectMapper.writeValueAsString(m);
    }
}
