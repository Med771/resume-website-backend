package ru.ai.sin.logic.main;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.ai.sin.config.PermitAllWebSecurityTestConfig;
import ru.ai.sin.filter.JwtCookieAuthenticationFilter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MainController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PermitAllWebSecurityTestConfig.class)
class MainControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MainService mainService;

    @MockBean
    private JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

    @Test
    void status_noContentWithoutAuth() throws Exception {
        mockMvc.perform(get("/main/status"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPhoto_okWithoutAuth() throws Exception {
        when(mainService.getFileContent("avatar.jpg")).thenReturn(new byte[] {1, 2, 3});
        when(mainService.getContentType("avatar.jpg")).thenReturn("image/jpeg");

        mockMvc.perform(get("/main/photo/{image_path}", "avatar.jpg"))
                .andExpect(status().isOk());
    }
}
