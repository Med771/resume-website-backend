# Документация Resume Singularity

Backend платформы резюме: каталог студентов, заявки, чат, вакансии, лента проектов и публичная главная.

Поля запросов и коды ответов смотрите в Swagger: `/swagger-ui.html`.

| Документ | О чём |
|----------|--------|
| [project-passport.md](./project-passport.md) | Что это за сервис и из чего собран |
| [backend.md](./backend.md) | Как устроен код, роли, чат, файлы |
| [database.md](./database.md) | Таблицы PostgreSQL |
| [api-endpoints.md](./api-endpoints.md) | Методы, пути, кто может вызывать |
| [frontend.md](./frontend.md) | Cookie, CORS, WebSocket, что видит клиент |
| [roles-product-journeys.md](./roles-product-journeys.md) | Сценарии студента, рекрутера и админа |
| [frontend-student-email-registration.md](./frontend-student-email-registration.md) | Регистрация студента по почте |
| [design-ui-brief.md](./design-ui-brief.md) | Экраны и состояния для UI |
| [telegram-setup.md](./telegram-setup.md) | Бот для подтверждения телефона рекрутера |
| [local-dev.md](./local-dev.md) | Как поднять сервер локально |
| [testing.md](./testing.md) | Как устроены тесты |
| [roadmap.md](./roadmap.md) | Что сервер сейчас не закрывает |

Роли в базе и в JWT: **STUDENT**, **RECRUITER**, **ADMIN**.
