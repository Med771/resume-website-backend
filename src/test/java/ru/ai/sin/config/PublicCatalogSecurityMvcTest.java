package ru.ai.sin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.integration.AbstractPostgresIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PublicCatalogSecurityMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicProjects_requiresAuth() throws Exception {
        mockMvc.perform(get("/public/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentMe_requiresAuth() throws Exception {
        mockMvc.perform(get("/student/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mainPhoto_requiresAuth() throws Exception {
        mockMvc.perform(get("/main/photo/avatar.jpg"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mainStatus_permitAll() throws Exception {
        mockMvc.perform(get("/main/status"))
                .andExpect(status().isOk());
    }
}
