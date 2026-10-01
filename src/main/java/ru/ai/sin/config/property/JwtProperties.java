package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String accessSecret;
    private String refreshSecret;

    private String issuer;
    private String audience;

    private long accessTokenTtl;
    private long refreshTokenTtl;
    private CookieProperties cookie = new CookieProperties();

    @Data
    public static class CookieProperties {
        private String accessTokenName;
        private String refreshTokenName;

        private boolean httpOnly;
        private boolean secure;

        private String sameSite;
        private String path;
        /** Optional, e.g. .singularity-resume.ru for shared cookies across subdomains */
        private String domain;
    }
}