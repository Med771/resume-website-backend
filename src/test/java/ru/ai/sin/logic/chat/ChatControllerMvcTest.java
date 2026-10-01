package ru.ai.sin.logic.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.MethodSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityTestConfig.class)
class ChatControllerMvcTest {

    private static final UUID CHAT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MSG_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void listChats_unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "u1")
    void listChats_authenticatedReturns200() throws Exception {
        when(chatService.listMyChats(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "u1")
    void sendText_returns201() throws Exception {
        when(chatService.sendTextMessage(any(), any())).thenReturn(null);

        mockMvc.perform(post("/chat/{chatId}/messages", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hi\"}")
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "u1", roles = "RECRUITER")
    void adminDelete_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(delete("/chat/{chatId}/messages/{messageId}", CHAT_ID, MSG_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adm", roles = "ADMIN")
    void adminDelete_noContent() throws Exception {
        mockMvc.perform(delete("/chat/{chatId}/messages/{messageId}", CHAT_ID, MSG_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(chatService).adminSoftDeleteMessage(CHAT_ID, MSG_ID);
    }

    @Test
    @WithMockUser(username = "u1")
    void markRead_noContent() throws Exception {
        mockMvc.perform(post("/chat/{chatId}/read", CHAT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageId\":\"" + MSG_ID + "\"}")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(chatService).markRead(any(), any());
    }

    @Test
    @WithMockUser(username = "u1")
    void editMessage_ok() throws Exception {
        when(chatService.editMessage(any(), any(), any())).thenReturn(null);

        mockMvc.perform(patch("/chat/{chatId}/messages/{messageId}", CHAT_ID, MSG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"updated\"}")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
