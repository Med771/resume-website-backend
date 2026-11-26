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
import ru.ai.sin.logic.student.version.StudentProfileVersionEnt;
import ru.ai.sin.logic.student.version.StudentProfileVersionRepo;
import ru.ai.sin.logic.student.version.StudentProfileVersionStatus;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RecruitmentRequestIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentProfileVersionRepo versionRepo;

    @Test
    void studentRespondsOnceAfterApprovedVersion() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] admin = login("admin", "admin");
        long specialityId = createSpeciality(admin, "RecSpec-" + suffix);
        long cityId = createCity(admin, "RecCity-" + suffix);
        UUID requestId = createRequest(admin, specialityId, cityId, "Co-" + suffix);

        MvcResult created = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/recruitment-requests/" + requestId)
                        .cookie(admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode request = objectMapper.readTree(created.getResponse().getContentAsString());
        assertThat(request.get("specialityIds").get(0).asLong()).isEqualTo(specialityId);
        assertThat(request.get("cityIds").get(0).asLong()).isEqualTo(cityId);

        UUID studentId = createStudent(admin, "rec_stu_" + suffix + "@integration.test", specialityId);
        Cookie[] student = login("rec_stu_" + suffix, "StudentPass1");

        mockMvc.perform(post("/recruitment-requests/" + requestId + "/responses").cookie(student))
                .andExpect(status().isForbidden());

        StudentProfileVersionEnt version = new StudentProfileVersionEnt();
        version.setStudentId(studentId);
        version.setVersionNumber(1);
        version.setStatus(StudentProfileVersionStatus.APPROVED);
        version.setSnapshot("{}");
        version.setCreatedAt(LocalDateTime.now());
        versionRepo.saveAndFlush(version);

        mockMvc.perform(post("/recruitment-requests/" + requestId + "/responses").cookie(student))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/recruitment-requests/" + requestId + "/responses").cookie(student))
                .andExpect(status().isConflict());
    }

    private Cookie[] login(String username, String password) throws Exception {
        String path = "admin".equals(username) ? "/auth/admin/login" : "/auth/login";
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn();
        return java.util.Arrays.stream(result.getResponse().getCookies())
                .filter(cookie -> "ACCESS_TOKEN".equals(cookie.getName()) || "REFRESH_TOKEN".equals(cookie.getName()))
                .toArray(Cookie[]::new);
    }

    private long createSpeciality(Cookie[] cookies, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/speciality")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createCity(Cookie[] cookies, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/city")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private UUID createRequest(Cookie[] cookies, long specialityId, long cityId, String company) throws Exception {
        String body = """
                {"tasks":"сверстать","qualificationRequirements":"html","workConditions":"удалённо","studentCount":2,"specialityIds":[%d],"cityIds":[%d],"companyName":"%s"}
                """.formatted(specialityId, cityId, company);
        MvcResult result = mockMvc.perform(post("/recruitment-requests")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createStudent(Cookie[] cookies, String email, long specialityId) throws Exception {
        ensureCity("Moscow");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", "Moscow");
        body.put("birthDate", "2000-05-05");
        body.put("bio", "integration student");
        body.put("course", "FIRST");
        body.put("busyness", "FREE");
        body.put("firstName", "Rec");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("specialityId", specialityId);
        body.put("skillsIds", List.of(createSkill(cookies, "RecSkill-" + email)));
        body.put("username", email.substring(0, email.indexOf('@')));
        body.put("password", "StudentPass1");
        MvcResult result = mockMvc.perform(post("/student")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private long createSkill(Cookie[] cookies, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/skill")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
