package ru.ai.sin.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий по HTTP с реальной БД (Testcontainers), Flyway и JWT в cookie:
 * админ готовит справочники и карточку студента, работодатель (GUEST) — каталог и заявка,
 * студент — ЛК и решение по заявке, снова работодатель и админ — чат и модерация.
 */
@AutoConfigureMockMvc
class FullRoleJourneysIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void recruiter_student_admin_fullHttpPath() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String studentEmail = "flow_student_" + suffix + "@integration.test";
        String recruiterEmail = "flow_recruiter_" + suffix + "@integration.test";

        Cookie[] adminCookies = login("admin", "admin");

        long specialityId = createSpeciality(adminCookies, "Spec-" + suffix);
        long skillId = createSkill(adminCookies, "Skill-" + suffix);

        UUID studentId = createStudent(adminCookies, studentEmail, specialityId, skillId);
        createStudentUser(adminCookies, "flow_stu_" + suffix, "flow-stu-pass", studentId);

        Cookie[] studentCookies = login("flow_stu_" + suffix, "flow-stu-pass");
        mockMvc.perform(post("/request")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestCreateJson(recruiterEmail, studentId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/student/me").cookie(studentCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains(studentId.toString()));

        Cookie[] guestCookies = login("guest", "guest");

        mockMvc.perform(post("/student/cardsFilter?page=0&size=20")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/student/" + studentId).cookie(guestCookies))
                .andExpect(status().isOk());

        mockMvc.perform(post("/student/filter?page=0&size=200")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains(studentId.toString()));

        mockMvc.perform(post("/student/filter?page=0&size=20")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains(studentId.toString()));

        MvcResult requestResult = mockMvc.perform(post("/request")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestCreateJson(recruiterEmail, studentId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode requestJson = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        long requestId = requestJson.get("id").asLong();
        UUID chatId = UUID.fromString(requestJson.get("appChatId").asText());

        assertThat(requestJson.get("result").asText()).isEqualTo("WAITING");

        mockMvc.perform(get("/recruiter/me").cookie(guestCookies))
                .andExpect(status().isOk());

        int recruiterMsgCountBefore = countMessages(guestCookies, chatId);
        assertThat(recruiterMsgCountBefore).isEqualTo(1);

        mockMvc.perform(post("/chat/" + chatId + "/messages")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hello before accept\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/chat/" + chatId + "/messages")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"admin visible later\"}"))
                .andExpect(status().isCreated());

        assertThat(countMessages(guestCookies, chatId)).isEqualTo(1);

        mockMvc.perform(get("/request/" + requestId).cookie(guestCookies))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/request/" + requestId).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("\"id\":" + requestId));

        MvcResult filterResult = mockMvc.perform(post("/request/filter?page=0&size=20")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode filterRoot = objectMapper.readTree(filterResult.getResponse().getContentAsString());
        assertThat(filterRoot.get("data").isArray()).isTrue();
        assertThat(filterRoot.get("data").size()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(post("/request/" + requestId + "/student-decision")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true,\"comment\":\"ok\"}"))
                .andExpect(status().isNoContent());

        MvcResult studentChatList = mockMvc.perform(get("/chat?page=0&size=20")
                        .cookie(studentCookies))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(studentChatList.getResponse().getContentAsString()).get("data").toString())
                .contains(chatId.toString());

        int afterAccept = countMessages(guestCookies, chatId);
        assertThat(afterAccept).isGreaterThanOrEqualTo(3);

        MvcResult recruiterMsg = mockMvc.perform(post("/chat/" + chatId + "/messages")
                        .cookie(guestCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"thanks, when can we call?\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID recruiterMessageId = UUID.fromString(
                objectMapper.readTree(recruiterMsg.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(delete("/chat/" + chatId + "/messages/" + recruiterMessageId).cookie(guestCookies))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/chat/" + chatId + "/messages/" + recruiterMessageId).cookie(adminCookies))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/request/" + requestId).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    private Cookie[] login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie[] raw = r.getResponse().getCookies();
        assertThat(raw).isNotEmpty();
        return java.util.Arrays.stream(raw)
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
        body.put("firstName", "Flow");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("phoneNumber", "+79991234567");
        body.put("telegramUsername", "flowstu");
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
        body.put("name", "Flow User");
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
        m.put("companyName", "FlowCorp");
        m.put("firstName", "Rec");
        m.put("lastName", "Ruiter");
        m.put("email", recruiterEmail);
        m.put("phoneNumber", "+78881112233");
        m.put("telegramUsername", "flowrec");
        m.put("studentId", studentId.toString());
        return objectMapper.writeValueAsString(m);
    }

    private int countMessages(Cookie[] cookies, UUID chatId) throws Exception {
        MvcResult r = mockMvc.perform(get("/chat/" + chatId + "/messages?page=0&size=100")
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(r.getResponse().getContentAsString());
        return root.get("data").size();
    }
}
