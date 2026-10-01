package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.swagger")
public class SwaggerProperties {

    private String title;
    private String description;
    private String version;
    private List<ServerProperties> servers;

    @Data
    public static class ServerProperties {
        private String url;
        private String description;
    }
}