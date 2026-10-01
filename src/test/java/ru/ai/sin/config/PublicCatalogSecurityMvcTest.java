package ru.ai.sin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.integration.AbstractPostgresIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PublicCatalogSecurityMvcTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicVacancies_requiresAuth() throws Exception {
        mockMvc.perform(get("/public/vacancies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicStudentsCards_requiresAuth() throws Exception {
        mockMvc.perform(post("/public/students/cards?page=0&size=20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicProjects_requiresAuth() throws Exception {
        mockMvc.perform(get("/public/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicVitrinaHome_permitAll() throws Exception {
        mockMvc.perform(get("/public/vitrina/home"))
                .andExpect(status().isOk());
    }
}
