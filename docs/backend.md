# Устройство и возможности backend

Сводный **паспорт проекта** (стек, архитектура, аспекты, сценарии): [project-passport.md](./project-passport.md).

**Полный перечень эндпоинтов** (метод, путь, роли): [api-endpoints.md](./api-endpoints.md).

## Стек и инфраструктура

| Компонент | Назначение |
|-----------|------------|
| **Java 21**, **Spring Boot 3.5.x** | REST API, безопасность, JPA |
| **PostgreSQL** | основное хранилище |
| **Flyway** | миграции (`classpath:db/migration`, `out-of-order: true`) |
| **Spring Data JPA** | репозитории, спецификации для фильтров |
| **JJWT** (через `JwtHelper`) | access / refresh токены |
| **Spring WebSocket + STOMP** | простой брокер сообщений, SockJS endpoint |
| **springdoc-openapi** | Swagger UI (`/swagger-ui.html`, `/v3/api-docs`) |

Конфигурация по умолчанию в `application.yaml`: datasource (например `jdbc:postgresql://localhost:5501/resume`), JWT в **HttpOnly cookies** (`ACCESS_TOKEN`, `REFRESH_TOKEN`), CORS с `allow-credentials: true`, путь cookie `/`.

## Доменная модель (упрощённо)

- **Компании, институты, специальности, навыки** — справочники и CRUD (см. соответствующие контроллеры).
- **Студенты** (`students` + связанные сущности: опыт, портфолио, образование и т.д.) — карточки резюме, фильтры, загрузка фото.
- **Рекрутеры** — отдельные сущности; к пользователю привязка через `users.recruiter_id`.
- **Пользователи** (`users`) — роль, опционально связь **1:1** со студентом (`users.student_id`, уникальна).
- **Заявки** (`requests`) — связь рекрутер + студент + **`app_chat_id`** (чат в приложении), результат (`result`), текст ответа студента.
- **Чаты** (`chats`) — один чат на пару **рекрутер–студент** (уникальный индекс по паре).
- **Сообщения** (`chat_messages`) — вид сообщения (`USER` / `SYSTEM`), тело, вложение (имя файла в хранилище), мягкое удаление админом.
- **Прочитанность** (`chat_read_states`) — последнее прочитанное сообщение по паре (чат, пользователь).

Миграция **V0029** вводит чаты/сообщения/read state, `users.student_id`, переносит заявки на `app_chat_id`, убирает старые Telegram-поля с заявок.

## Роли и безопасность

Роли: **`GUEST`**, **`USER`**, **`STUDENT`**, **`ADMIN`**. В Spring Security для `hasRole('X')` в JWT/Principal ожидается authority вида **`ROLE_X`** (формируется в `UserHelper` из `RoleEnum`).

В `SecurityConfig`: кроме явных исключений всё требует **аутентификации**. Исключения: `/auth/**`, **`/public/vitrina/**`**, **`/public/analytics/**`**, `/main/**`, **`/ws/**`** (handshake WebSocket), Swagger, `/error`, `OPTIONS /**`. Справочники company/skill/education/speciality читаются после входа через обычные `GET /{id}` и `POST /filter`.

**`JwtCookieAuthenticationFilter`**: читает access (и при необходимости refresh), валидирует JWT, поднимает `SecurityContext` с `UserDetails` по username из БД.

## Основные возможности по областям

Перечень всех REST-путей с ролями: [api-endpoints.md](./api-endpoints.md). Ниже — поведение домена, а не таблица URL.

### Аутентификация (`/auth`)

Регистрация студента и работодателя, login, refresh, logout; cookie и лимиты — в справочнике и `application.yaml`.

### Публичное / служебное (`/main`)

Статус сервиса и раздача изображений из хранилища приложения.

### Заявки (`/request`)

- Рекрутер (не **`STUDENT`**): создание заявки; чат get-or-create, системное сообщение **`REQUEST_SENT`**, ожидание решения (**`WAITING`** / в проверке решения студента также учитывается **`CREATION`**).
- Студент: решение по заявке — `StudentRequestDecisionReq`: `accept`, опционально `comment`; **`STUDENT_CONFIRMED`** или **`REFUSAL`**, в чат — **`STUDENT_ACCEPTED`** / **`STUDENT_REJECTED`**.
- Админ: просмотр по id, фильтр страницы, удаление.

### Чаты (`/chat`)

- Список «моих» чатов с превью и непрочитанным (админ — все; иначе по привязке рекрутер/студент).
- Сводка, постраничные сообщения, отправка текста и **multipart** с вложением, правка сообщения, отметка прочитанного.
- Админ: мягкое удаление сообщения.

### Бизнес-логика видимости чата

- Пока по паре рекрутер–студент **нет** «разрешённого» результата заявки (**`STUDENT_CONFIRMED`**, **`SUCCESS`**, **`RECRUITER_CONFIRMED`**), **рекрутер и студент** в REST видят **только системные** сообщения. Обычные сообщения участников в этот период **не показываются** в списке и в превью.
- **Админ** видит полную историю.
- После принятия заявки студентом — полная переписка для сторон (при доступе к чату).
- Первое **пользовательское** сообщение **админа** в чате инициирует системное **`ADMIN_JOINED`** (`ChatServiceImpl`).

### WebSocket

- Endpoint: **`/ws`** (SockJS).
- Брокер: префикс **`/topic`**.
- **`/topic/chats/{chatId}`** — системные сообщения и (после «разрешения» заявки) пользовательские для общего топика.
- До принятия заявки **пользовательские** сообщения публикуются в **`/topic/chats/{chatId}/staff`** — для real-time админу; рекрутер/студент подписываются только на основной топик.

Подробнее для клиентов: [frontend.md](./frontend.md).

### Пользователи (`/user`, админ)

Фильтр, создание (в т.ч. **`STUDENT`** + `studentId`), удаление **`USER`** / **`STUDENT`**.

### Студент (`/student`)

Каталог для рекрутера (**RECRUITER**/**ADMIN**), админские CRUD и фото; ЛК — **`GET /student/me`** и **`PATCH /student/me`** только для **`STUDENT`**. Дозаполнение резюме — CRUD `/experience`, `/institution`, `/portfolio`. Публичная витрина без входа — **`/public/vitrina/home`**.

### Рекрутер (`/recruiter`)

Профиль «я» и чтение по id; CRUD справочника рекрутеров — админ.

### Справочники и связанные сущности

**company, skill, speciality, education:** `GET /{id}` и **`POST …/filter`** — **STUDENT**/**RECRUITER**/**ADMIN**; CUD — только **ADMIN**.  
**experience, portfolio, institution:** чтение — **STUDENT**/**RECRUITER**/**ADMIN** (чужое — только при `catalogVisible`); CUD — **STUDENT** (своя карточка) и **ADMIN**.  
**institution:** при фильтре с **`educationId`** требуется роль **ADMIN** (см. `SecurityHelper`).

## Хранение файлов

`app.file.path` (по умолчанию `file`) и лимит размера — вложения чата и фото студентов на диске; картинки — также через `/main/photo/...`.

## Документация API

- Сводная таблица эндпоинтов: [api-endpoints.md](./api-endpoints.md).
- Swagger UI и OpenAPI — `springdoc` и `app.swagger` в `application.yaml` (`/swagger-ui.html`, `/v3/api-docs`).

## Системные события чата (строки `systemEvent`)

Константы в `ru.ai.sin.logic.chat.ChatSystemEvent`:

- `REQUEST_SENT`
- `STUDENT_ACCEPTED`
- `STUDENT_REJECTED`
- `ADMIN_JOINED`

## Публичная витрина и сортировка студентов (без входа)

- Колонки `students.public_profile_consent`, `students.profile_text_score` (Flyway `V0033`). Score пересчитывается при создании/обновлении карточки и саморегистрации.
- **Сортировка:** `POST /student/cardsFilter` и `POST /student/filter` **игнорируют** произвольный `sort` из query; порядок задаётся полями в `FilterStudentReq`: `sortBy` (`StudentSortField`, в т.ч. `MANUAL_SORT_ORDER`), `sortDirection`, `useDefaultRanking` (по умолчанию: ручной номер `manualSortOrder` → аватар → `profileTextScore` → дата создания).
- **Публичные студенты:** `GET /public/students/{id}`, `POST /public/students/cards` — только записи с `public_profile_consent = true` и `catalog_visible = true`; в `SecurityConfig` — `permitAll`.

## Лента проектов

- Таблица `site_projects` (Flyway `V0034`).
- Один ресурс `/projects`: чтение **STUDENT** / **RECRUITER** / **ADMIN** (`POST /projects/filter`, `GET /projects/{id}`); CUD, `POST /projects/reorder`, `…/{id}/students` — только **ADMIN**.
- Видимость: админ — все записи и `students` в DTO; рекрутер — окно публикации и `students`; студент — окно публикации, `students = null`. Вне окна / чужой id для не-админа — **404**.
- Анонимная главная: `GET /public/vitrina/home` вызывает тот же сервисный list (`visibleToAnonymous` + окно, без участников, `limit` из `app.vitrina.home`). Отдельного `/public/projects` нет.

## Аналитика посещений (first-party)

- Таблица `analytics_events` (Flyway `V0035`).
- Приём: `POST /public/analytics/events` (`permitAll`), лимит `app.analytics.rate-limit-per-ip-per-minute`, тип события пока **`PAGE_VIEW`**.
- Отчёт админа: `POST /admin/analytics/summary` с телом `from` / `to` — агрегация `COUNT` по `path`.
- Сводка по сущностям: `POST /admin/analytics/entity-population` — число пользователей по ролям, всего студентов и рекрутеров; опционально окно **`from`/`to`** для подсчёта **новых** студентов и рекрутеров по `created_at` (у таблицы `users` нет даты создания).

## Зависимости OpenAPI

- В `pom.xml` свойство **`swagger.version` = `2.8.15`** (`springdoc-openapi-starter-webmvc-ui`). Линейка **springdoc 3.x** ориентирована на **Spring Boot 4+** и подтягивает артефакты `spring-boot-webmvc:4.x`, что ломает запуск на Boot 3.5 (смешанный classpath, `NoClassDefFoundError` для классов error handling). После перехода на Boot 4 можно снова поднять springdoc до 3.x по [матрице совместимости](https://springdoc.org/).

## Чеклист релиза / деплоя

- Прогнать миграции Flyway на стейдже/проде: **`V0033`**, **`V0034`**, **`V0035`**, **`V0036`** (новые колонки и таблицы).
- После наката проверить индексы (создаются миграциями): по `students` для публичной выдачи; по `analytics_events(occurred_at)` для отчётов; по `site_projects` для фильтра `visible_to_anonymous` и сортировки.
- Убедиться, что **`app.analytics.rate-limit-per-ip-per-minute`** задан под ожидаемый трафик.
- Версия OpenAPI в UI: **`app.swagger.version`** (сейчас **0.2.0**).
- После обновления springdoc: открыть **`/swagger-ui.html`** и **`/v3/api-docs`**.
