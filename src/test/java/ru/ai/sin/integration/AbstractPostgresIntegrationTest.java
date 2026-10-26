package ru.ai.sin.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Общая настройка интеграционных тестов с реальным PostgreSQL (Spring Boot Testing + Testcontainers).
 * Подменяет {@code spring.datasource.*} через {@link DynamicPropertySource}, Flyway гоняется на контейнере.
 * Контейнер один на всю JVM: иначе каждый тестовый класс поднимает новый порт, а Spring отдаёт старый контекст.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractPostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("resume_it");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_PORT", () -> "1025");
        registry.add("MAIL_USERNAME", () -> "test@localhost");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("MAIL_FROM", () -> "test@localhost");
        registry.add("TELEGRAM_BOT_TOKEN", () -> "test-bot-token");
        registry.add("TELEGRAM_BOT_USERNAME", () -> "test_bot");
        registry.add("TELEGRAM_WEBHOOK_SECRET", () -> "test-webhook-secret");
        registry.add("ADMIN_USERNAME", () -> "admin");
        registry.add("ADMIN_PASSWORD", () -> "admin");
        registry.add("app.user.logins[0].username", () -> "admin");
        registry.add("app.user.logins[0].password", () -> "admin");
        registry.add("app.user.logins[0].role", () -> "ADMIN");
        registry.add("app.user.logins[1].username", () -> "guest");
        registry.add("app.user.logins[1].password", () -> "guest");
        registry.add("app.user.logins[1].role", () -> "RECRUITER");
        registry.add("JWT_ACCESS_SECRET", () -> "bG9jYWwtdGVzdC1hY2Nlc3Mtc2VjcmV0LWtleS0zMmIh");
        registry.add("JWT_REFRESH_SECRET", () -> "bG9jYWwtdGVzdC1yZWZyZXNoLXNlY3JldC1rZXktMzJiIQ==");
        registry.add("JWT_COOKIE_SECURE", () -> "false");
        registry.add("JWT_COOKIE_SAME_SITE", () -> "Lax");
        registry.add("SWAGGER_VERSION", () -> "1.0.0");
        registry.add("SWAGGER_ON", () -> "false");
    }
}
