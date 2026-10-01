package ru.ai.sin.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Минимальная цепочка для {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}:
 * {@code @PreAuthorize} на контроллерах без JWT-фильтра (см. {@code addFilters = false}).
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class MethodSecurityTestConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }
}
