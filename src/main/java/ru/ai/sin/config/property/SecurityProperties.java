package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {
    
    private CorsProperties cors = new CorsProperties();
    private CspProperties csp = new CspProperties();

    @Data
    public static class CorsProperties {
        private List<String> allowedOrigins;
        private List<String> allowedMethods;
        private List<String> allowedHeaders;
        private boolean allowCredentials;
        private Long maxAge;
    }

    @Data
    public static class CspProperties {
        private String policy;
    }
}
