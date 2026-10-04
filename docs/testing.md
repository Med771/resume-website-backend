# Тесты

Запуск: `.\mvnw.cmd test` (или `mvn test`).

Отчёт покрытия JaCoCo: `target/site/jacoco/index.html`. Порог покрытия сборку не валит.

## Два вида

**Без базы.** Mockito и `@WebMvcTest`: сервис или один контроллер, безопасность метода через тестовый `SecurityFilterChain`. Так проверяют заявки, чат, вакансии, регистрацию, доступ к опыту и портфолио.

**С PostgreSQL.** `AbstractPostgresIntegrationTest` поднимает контейнер `postgres:16-alpine`, гоняет Flyway и ходит в API с cookie. Класс помечен `disabledWithoutDocker = true`: нет Docker — тест пропускается, `mvn test` не краснеет.

Интеграционные сценарии: путь ролей (`FullRoleJourneysIntegrationTest`), вакансии и отклик, проекты и студенты, порядок навыков, публичная главная и закрытый каталог без cookie (`PublicCatalogSecurityMvcTest`).

Первый прогон с Docker скачивает образ Postgres.
