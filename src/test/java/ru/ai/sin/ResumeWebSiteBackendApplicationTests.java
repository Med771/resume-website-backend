package ru.ai.sin;

import org.junit.jupiter.api.Test;
import ru.ai.sin.integration.AbstractPostgresIntegrationTest;

/**
 * Поднимает контекст Spring с реальным PostgreSQL в Docker (Flyway + JPA validate).
 * <p>С запущенным Docker тест выполняется. Без Docker — класс помечается как <em>Disabled</em>,
 * {@code mvn test} остаётся зелёным (удобно для машин без Docker / агентов без сокета).
 * В CI с Docker этот тест должен реально выполняться.
 */
class ResumeWebSiteBackendApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
