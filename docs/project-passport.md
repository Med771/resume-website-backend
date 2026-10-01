# Паспорт проекта

Backend «Resume Singularity»: карточки студентов, заявки рекрутеров, чат, вакансии, модерация, лента проектов и своя аналитика в PostgreSQL.

| | |
|--|--|
| Maven | `ru.sin:resume-web-site-backend:0.0.1-SNAPSHOT` |
| Пакет | `ru.ai.sin` |
| Старт | `ru.ai.sin.ResumeWebSiteBackendApplication` |
| Java | 21 |
| Spring Boot | 3.5.7 |
| База | PostgreSQL, схема через Flyway (`ddl-auto: validate`) |
| API | REST, префикса вроде `/api` в приложении нет |
| Вход | JWT в HttpOnly-cookie `ACCESS_TOKEN` и `REFRESH_TOKEN` |
| Документация HTTP | Swagger `/swagger-ui.html`, OpenAPI `/v3/api-docs` |

Контекстный путь не задан: URL начинаются с `/`. Если перед сервисом стоит шлюз, префикс добавляет он.

## Стек

Spring Web, Spring Data JPA, Hibernate, Flyway 10.20.1, Spring Security (метод `@PreAuthorize`), JJWT, MapStruct, Lombok, WebSocket STOMP + SockJS (`/ws`), springdoc 2.8.15. Тесты: JUnit 5, Mockito, Testcontainers.

Аналитика посещений — таблица `analytics_events` и REST.

## Где читать дальше

Устройство кода и правила доступа — [backend.md](./backend.md). Таблицы — [database.md](./database.md). Список URL — [api-endpoints.md](./api-endpoints.md).
