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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountStatusSortIntegrationTest extends AbstractPostgresIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Test
    void filterBySkillAndAccountStatus_pendingComesFirst() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Cookie[] adminCookies = login("admin", "admin");
        long specialityId = createSpeciality(adminCookies, "SortSpec-" + suffix);
        long skillId = createSkill(adminCookies, "SortSkill-" + suffix);

        UUID approvedId = createStudent(adminCookies, "sort_ok_" + suffix, specialityId, skillId);
        UUID pendingId = createStudent(adminCookies, "sort_wait_" + suffix, specialityId, skillId);
        UserEnt pending = userRepo.findByUsername("sort_wait_" + suffix).orElseThrow();
        pending.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        userRepo.saveAndFlush(pending);

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("skillsIds", List.of(skillId));
        filter.put("sortBy", "ACCOUNT_STATUS");
        filter.put("useDefaultRanking", false);

        MvcResult result = mockMvc.perform(post("/student/filter?page=0&size=20")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        List<String> ids = new java.util.ArrayList<>();
        data.forEach(node -> ids.add(node.get("id").asText()));
        assertThat(ids).contains(pendingId.toString(), approvedId.toString());
        assertThat(ids.indexOf(pendingId.toString())).isLessThan(ids.indexOf(approvedId.toString()));
    }

    private Cookie[] login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/admin/login")
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

    private long createSkill(Cookie[] cookies, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/skill")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private UUID createStudent(Cookie[] cookies, String username, long specialityId, long skillId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("city", "Moscow");
        body.put("birthDate", "2000-05-05");
        body.put("bio", "status sort student");
        body.put("course", "FIRST");
        body.put("busyness", "FREE");
        body.put("firstName", "Sort");
        body.put("lastName", username);
        body.put("email", username + "@test.local");
        body.put("phoneNumber", "+79991234567");
        body.put("specialityId", specialityId);
        body.put("skillsIds", List.of(skillId));
        body.put("username", username);
        body.put("password", "StudentPass1");

        MvcResult result = mockMvc.perform(post("/student")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
