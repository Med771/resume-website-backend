package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.user")
public class UserProperties {

    private List<Login> logins;

    @Data
    public static class Login {
        private String username;
        private String password;
        private String role;
    }
}