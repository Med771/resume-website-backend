# Документация Resume Singularity

| Документ | Содержание |
|----------|------------|
| [api-endpoints.md](./api-endpoints.md) | Справочник HTTP: пути, методы, роли |
| [project-passport.md](./project-passport.md) | Паспорт проекта: стек, БД, безопасность |
| [backend.md](./backend.md) | Архитектура backend |
| [frontend.md](./frontend.md) | Руководство для frontend-разработчиков |
| [frontend-student-email-registration.md](./frontend-student-email-registration.md) | Саморегистрация студента: почта, код, анкета |
| [testing.md](./testing.md) | Тесты, Testcontainers, JaCoCo |
| [roles-product-journeys.md](./roles-product-journeys.md) | Сценарии по ролям |
| [design-ui-brief.md](./design-ui-brief.md) | UI/UX: чаты, проекты, вакансии |
| [telegram-setup.md](./telegram-setup.md) | Telegram-бот для верификации телефона |
| [local-dev.md](./local-dev.md) | Локальный запуск (Docker DB + native apps) |
| [roadmap.md](./roadmap.md) | Открытые задачи |

Конфигурация: `src/main/resources/application.yaml`. Swagger UI: `/swagger-ui.html`.

## Роли

| Роль | Описание |
|------|----------|
| **GUEST** | Неаутентифицированный пользователь (`permitAll` эндпоинты) |
| **STUDENT** | Студент с JWT |
| **RECRUITER** | Рекрутер с JWT |
| **ADMIN** | Администратор |

В БД (`RoleEnum`): только `STUDENT`, `RECRUITER`, `ADMIN`.
