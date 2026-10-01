package ru.ai.sin.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Для {@code @WebMvcTest} контроллеров с публичными путями в проде ({@code /auth/**}, {@code /main/**}).
 */
@TestConfiguration
@EnableWebSecurity
public class PermitAllWebSecurityTestConfig {

    @Bean
    SecurityFilterChain permitAllTestChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
