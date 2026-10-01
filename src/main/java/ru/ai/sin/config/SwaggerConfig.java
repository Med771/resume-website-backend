package ru.ai.sin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.ai.sin.config.property.SwaggerProperties;


@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(swaggerProperties.getTitle())
                        .description(swaggerProperties.getDescription())
                        .version(swaggerProperties.getVersion()))

                .servers(swaggerProperties.getServers().stream()
                        .map(serv -> new Server()
                                .url(serv.getUrl())
                                .description(serv.getDescription()))
                        .toList()
                );
    }
}
