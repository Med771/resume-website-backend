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
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PlatformUpdateIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Test
    void publicVacancies_anonymousWithoutAuth() throws Exception {
        mockMvc.perform(get("/public/vacancies?page=0&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    void publicStudents_anonymousWithoutAuth() throws Exception {
        mockMvc.perform(post("/public/students/cards?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void accountApproval_pendingStudentBlockedFromApply() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecP-" + suffix);
        long skillId = createSkill(adminCookies, "SkillP-" + suffix);
        UUID studentId = createStudent(adminCookies, "pend_stu_" + suffix + "@test.local", specialityId, skillId);
        String username = "pend_stu_" + suffix;
        createStudentUser(adminCookies, username, "pass-pend-1", studentId);

        UserEnt user = userRepo.findByUsername(username).orElseThrow();
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        userRepo.save(user);

        Cookie[] studentCookies = login(username, "pass-pend-1");
        mockMvc.perform(get("/auth/me").cookie(studentCookies))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("PENDING_APPROVAL"));

        mockMvc.perform(post("/request")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"X\",\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"r@test.local\",\"phoneNumber\":\"+79991112233\",\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountApproval_adminApprovesThenStudentCanPatchMe() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecA-" + suffix);
        long skillId = createSkill(adminCookies, "SkillA-" + suffix);
        UUID studentId = createStudent(adminCookies, "appr_stu_" + suffix + "@test.local", specialityId, skillId);
        String username = "appr_stu_" + suffix;
        createStudentUser(adminCookies, username, "pass-appr-1", studentId);

        UserEnt user = userRepo.findByUsername(username).orElseThrow();
        user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        userRepo.save(user);

        mockMvc.perform(post("/admin/account-approvals/" + user.getId() + "/approve").cookie(adminCookies))
                .andExpect(status().isNoContent());

        Cookie[] studentCookies = login(username, "pass-appr-1");
        mockMvc.perform(patch("/student/me")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"publicProfileConsent\":true}"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("\"publicProfileConsent\":true"));
    }

    @Test
    void adminStudentsReorder() throws Exception {
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecR-" + UUID.randomUUID().toString().substring(0, 6));
        long skillId = createSkill(adminCookies, "SkillR-" + UUID.randomUUID().toString().substring(0, 6));
        UUID id1 = createStudent(adminCookies, "reorder1_" + UUID.randomUUID().toString().substring(0, 6) + "@t.local", specialityId, skillId);
        UUID id2 = createStudent(adminCookies, "reorder2_" + UUID.randomUUID().toString().substring(0, 6) + "@t.local", specialityId, skillId);

        mockMvc.perform(post("/admin/students/reorder")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\":[\"" + id2 + "\",\"" + id1 + "\"]}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void recruiterDelete_cascadeIncludingChats() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecD-" + suffix);
        long skillId = createSkill(adminCookies, "SkillD-" + suffix);
        UUID studentId = createStudent(adminCookies, "del_stu_" + suffix + "@test.local", specialityId, skillId);

        MvcResult recruiterResult = mockMvc.perform(post("/recruiter")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"DelCorp\",\"firstName\":\"Del\",\"lastName\":\"Rec\",\"email\":\"delrec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990001122\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID recruiterId = UUID.fromString(objectMapper.readTree(recruiterResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/user")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Del Rec\",\"username\":\"delrec_" + suffix + "\",\"password\":\"pass-del-1\",\"role\":\"RECRUITER\",\"recruiterId\":\"" + recruiterId + "\"}"))
                .andExpect(status().isCreated());

        Cookie[] recruiterCookies = login("delrec_" + suffix, "pass-del-1");

        MvcResult requestResult = mockMvc.perform(post("/request")
                        .cookie(recruiterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"DelCorp\",\"firstName\":\"Del\",\"lastName\":\"Rec\",\"email\":\"delrec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990001122\",\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(objectMapper.readTree(requestResult.getResponse().getContentAsString()).get("appChatId").asText()).isNotBlank();

        mockMvc.perform(delete("/recruiter/" + recruiterId).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    @Test
    void tuDecision_fullSuccessFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecT-" + suffix);
        long skillId = createSkill(adminCookies, "SkillT-" + suffix);
        UUID studentId = createStudent(adminCookies, "tu_stu_" + suffix + "@test.local", specialityId, skillId);
        createStudentUser(adminCookies, "tu_stu_" + suffix, "pass-tu-1", studentId);
        Cookie[] studentCookies = login("tu_stu_" + suffix, "pass-tu-1");

        MvcResult recruiterResult = mockMvc.perform(post("/recruiter")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"TuCorp\",\"firstName\":\"Tu\",\"lastName\":\"Rec\",\"email\":\"turec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990003344\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID recruiterId = UUID.fromString(objectMapper.readTree(recruiterResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/user")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tu Rec\",\"username\":\"turec_" + suffix + "\",\"password\":\"pass-tu-r\",\"role\":\"RECRUITER\",\"recruiterId\":\"" + recruiterId + "\"}"))
                .andExpect(status().isCreated());
        Cookie[] recruiterCookies = login("turec_" + suffix, "pass-tu-r");

        MvcResult requestResult = mockMvc.perform(post("/request")
                        .cookie(recruiterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"TuCorp\",\"firstName\":\"Tu\",\"lastName\":\"Rec\",\"email\":\"turec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990003344\",\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/request/" + requestId + "/student-decision")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true,\"comment\":\"ok\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/request/" + requestId + "/tu-decision")
                        .cookie(studentCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/request/" + requestId + "/tu-decision")
                        .cookie(recruiterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/request/" + requestId).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(r -> {
                    JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
                    assertThat(body.get("result").asText()).isEqualTo("SUCCESS");
                    assertThat(body.get("tuPhase").asText()).isEqualTo("COMPLETED");
                    assertThat(body.get("studentTuConfirmedAt").isNull()).isFalse();
                    assertThat(body.get("recruiterTuConfirmedAt").isNull()).isFalse();
                    assertThat(body.get("recruiterDisplayName").asText()).isNotBlank();
                    assertThat(body.get("studentDisplayName").asText()).isNotBlank();
                });
    }

    @Test
    void adminDeleteChat_cascadesRequestAndMessages() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SpecC-" + suffix);
        long skillId = createSkill(adminCookies, "SkillC-" + suffix);
        UUID studentId = createStudent(adminCookies, "chatdel_" + suffix + "@test.local", specialityId, skillId);

        MvcResult recruiterResult = mockMvc.perform(post("/recruiter")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"DelChatCorp\",\"firstName\":\"Chat\",\"lastName\":\"Del\",\"email\":\"chatdelrec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990005566\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID recruiterId = UUID.fromString(objectMapper.readTree(recruiterResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/user")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chat Del\",\"username\":\"chatdelrec_" + suffix + "\",\"password\":\"pass-chatdel\",\"role\":\"RECRUITER\",\"recruiterId\":\"" + recruiterId + "\"}"))
                .andExpect(status().isCreated());

        Cookie[] recruiterCookies = login("chatdelrec_" + suffix, "pass-chatdel");

        MvcResult requestResult = mockMvc.perform(post("/request")
                        .cookie(recruiterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"DelChatCorp\",\"firstName\":\"Chat\",\"lastName\":\"Del\",\"email\":\"chatdelrec_" + suffix + "@test.local\",\"phoneNumber\":\"+79990005566\",\"studentId\":\"" + studentId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode requestBody = objectMapper.readTree(requestResult.getResponse().getContentAsString());
        long requestId = requestBody.get("id").asLong();
        UUID chatId = UUID.fromString(requestBody.get("appChatId").asText());

        mockMvc.perform(get("/chat/" + chatId + "/messages").cookie(adminCookies))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/chat/" + chatId).cookie(adminCookies))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/request/" + requestId).cookie(adminCookies))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/chat/" + chatId + "/messages").cookie(adminCookies))
                .andExpect(status().isNotFound());
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
        body.put("firstName", "Flow");
        body.put("lastName", "Student");
        body.put("email", email);
        body.put("phoneNumber", "+79991234567");
        body.put("telegramUsername", "flowstu");
        body.put("specialityId", specialityId);
        body.put("skillsIds", List.of(skillId));
        body.put("publicProfileConsent", true);

        MvcResult r = mockMvc.perform(post("/student")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private void createStudentUser(Cookie[] cookies, String username, String password, UUID studentId) throws Exception {
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
}
