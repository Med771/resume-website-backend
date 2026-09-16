# Паспорт проекта `resume-web-site-backend`

Документ описывает **фактическое состояние** репозитория: назначение, стек, архитектуру, данные, безопасность, HTTP/WebSocket контракты, сквозные сценарии и нефункциональные аспекты. Точные поля DTO и коды ответов — в **Swagger** (`/swagger-ui.html`). Дополнение к [backend.md](./backend.md), [frontend.md](./frontend.md), [roles-product-journeys.md](./roles-product-journeys.md), [testing.md](./testing.md), [roadmap.md](./roadmap.md).

---

## 1. Идентификация и назначение

| Поле | Значение |
|------|----------|
| **Maven** | `ru.sin:resume-web-site-backend:0.0.1-SNAPSHOT` |
| **Корневой пакет** | `ru.ai.sin` |
| **Точка входа** | `ru.ai.sin.ResumeWebSiteBackendApplication` |
| **Назначение** | Backend платформы «резюме / витрина студентов»: каталог карточек, заявки рекрутеров, чат с поэтапной видимостью, модерация, справочники, саморегистрация, публичная витрина без входа, лента проектов, first-party аналитика в PostgreSQL. |

Контекстный путь приложения **не** задан (`server.servlet.context-path` отсутствует): пути API начинаются с `/` (перед деплоем за прокси часто добавляют префикс вроде `/api` на стороне шлюза — это вне кода данного репозитория).

---

## 2. Стек технологий и зависимости

| Технология | Роль в проекте | Примечание |
|------------|----------------|------------|
| **Java 21** | Язык | |
| **Spring Boot 3.5.7** | BOM, автоконфигурация | Parent POM |
| **Spring Web / MVC** | REST | |
| **Spring Data JPA** | Репозитории, `Specification`, пагинация | `ddl-auto: validate` |
| **Hibernate** | ORM | через Boot |
| **PostgreSQL** + **JDBC 42.7.8** | БД | |
| **Flyway 10.20.1** | Миграции | `out-of-order: true` |
| **Spring Security 7.0.0** | JWT в cookie, `@PreAuthorize` | Override версии в `pom.xml` |
| **JJWT 0.12.6** | Парсинг/сборка JWT | |
| **springdoc-openapi 2.8.15** | OpenAPI + Swagger UI | Линейка 2.8 для Boot 3.5; springdoc 3.x тянет Boot 4 — не используется |
| **MapStruct 1.5.5** | Маппинг DTO ↔ сущности | |
| **Lombok** | Шаблонный код | |
| **WebSocket + STOMP + SockJS** | Real-time чат | Endpoint `/ws` |
| **Testcontainers** | Интеграционные тесты с PostgreSQL | JUnit 5 |
| **JaCoCo** | Отчёт покрытия | без обязательного `check` в lifecycle |

**Не используется в репозитории:** Spring Actuator/Micrometer как готовая аналитика посещений (аналитика реализована отдельной таблицей и REST).

---

## 3. Архитектура кода (слои и пакеты)

```
ru.ai.sin
├── ResumeWebSiteBackendApplication.java
├── config/                 # Security, WebSocket, свойства (@ConfigurationProperties)
├── filter/                 # JwtCookieAuthenticationFilter
├── helper/                 # SecurityHelper, FileHelper, JwtHelper, …
├── tools/                  # *Tools — загрузка сущностей, маппинг в DTO для сервисов
├── models/                 # Общие модели: PageResponse, embeddables, enums
├── exception/              # GlobalExceptionHandler, ApiException, …
└── logic/                  # Домен по областям (пакет ≈ bounded context)
    ├── auth/
    ├── student/            # Карточки, фильтры, спецификации, скоринг
    ├── publicapi/student/  # Публичная витрина студентов
    ├── request/
    ├── chat/
    ├── recruiter/
    ├── user/
    ├── company|skill|speciality|education|experience|institution|portfolio/
    ├── registration/       # Саморегистрация студента + публичные справочники
    ├── recruiter/registration/  # Заявки на регистрацию работодателя (админ)
    ├── siteproject/
    ├── analytics/
    └── main/               # Статус, отдача файлов с диска
```

**Паттерн:** контроллер → сервис (`*ServiceImpl`) → репозиторий (`*Repo`); для сложных выборок — `*Specifications`; маппинг — MapStruct (`*Mapper`) и/или `*Tools`. Транзакции — `@Transactional` на сервисах; чтение списков с последующим маппингом LAZY-полей — в одной **read-only** транзакции (см. студентов `getAllByFilter`, `getById`).

---

## 4. Конфигурация (`application.yaml`)

Ключевые группы (имена могут уточняться в файле):

- **`spring.datasource`**, **`spring.jpa`** (`ddl-auto: validate`, `open-in-view: false`), **`spring.flyway`**
- **`app.security`** — CORS, JWT TTL, имена cookie, CSP
- **`app.file`** — корень хранилища файлов, лимит размера
- **`app.registration`** — лимиты саморегистрации, rate limit по IP
- **`app.user.logins`** — демо-пользователи при старте (отключать/менять в проде)
- **`app.analytics`** — лимит событий с IP в минуту
- **`app.swagger`** — метаданные OpenAPI
- **`springdoc`** — включение UI и `/v3/api-docs`

---

## 5. Данные и миграции Flyway

Все изменения схемы — SQL в `src/main/resources/db/migration/`. Расширение **pg_trgm** (V0021) используется для нечёткого поиска по ФИО/bio в фильтре студентов.

**Логические группы миграций:**

| Область | Примеры версий / смысл |
|---------|-------------------------|
| Справочники | companies, skills, specialities, education |
| Профиль | students, опыт, institution, portfolio, связь student_skills (M:N) |
| Пользователи и роли | users, связь со студентом/рекрутером |
| Рекрутеры | recruiters, FK, регистрация работодателя (V0032) |
| Заявки и чаты | requests, chats, messages, read_states (V0029 и др.) |
| Студент: публичность и ранг | V0033 — `public_profile_consent`, `profile_text_score` |
| Лента сайта | V0034 — `site_projects` |
| Аналитика | V0035 — `analytics_events` |

**JPA:** сущности в `logic/*/…Ent.java` должны соответствовать схеме после миграций (`validate`).

---

## 6. Безопасность и роли

### 6.1. Роли (`RoleEnum`)

**`GUEST`**, **`USER`**, **`STUDENT`**, **`ADMIN`**. В Spring Security ожидаются authorities **`ROLE_<ИМЯ>`** (согласование при загрузке пользователя / JWT).

### 6.2. Цепочка `SecurityFilterChain` (`SecurityConfig`)

- **CSRF** отключён (stateless API + cookie JWT).
- **Session:** `STATELESS`.
- **CORS** из `app.security.cors`.
- **JWT filter** до `UsernamePasswordAuthenticationFilter`.
- **CSP** заголовки из конфига.
- **Матчеры `permitAll`:** `/auth/**`, `/public/vitrina/**`, `/public/analytics/**`, `/main/**`, `/ws/**`, Swagger, `/error`, `OPTIONS /**`.
- **Остальное:** `authenticated()`; детализация прав — **`@PreAuthorize`** на методах контроллеров.

### 6.3. Аутентификация

- **HttpOnly cookies:** access + refresh (имена из конфига).
- **`JwtCookieAuthenticationFilter`:** извлекает JWT, валидирует, поднимает `SecurityContext` с пользователем из БД.

### 6.4. Методная безопасность

`@EnableMethodSecurity` — на контроллерах широко используются **`hasRole`**, **`hasAnyRole`**.

---

## 7. Карта REST-контроллеров (префиксы)

| Префикс | Назначение |
|---------|------------|
| `/auth` | login, refresh, logout, регистрация студента/рекрутера (см. `AuthController`) |
| `/main` | health, отдача файлов по пути |
| `/public/vitrina` | Анонимная главная: студенты + проекты |
| `/public/analytics` | Ingest событий аналитики |
| `/student` | Каталог, CRUD карточек (в основном админ), ЛК студента |
| `/request` | Заявки, фильтр, решение студента |
| `/chat` | Чаты и сообщения |
| `/recruiter` | Профиль рекрутера |
| `/user` | Пользователи (админ) |
| `/company`, `/skill`, `/speciality`, `/education`, `/experience`, `/institution`, `/portfolio` | Справочники и связи с карточкой |
| `/admin/recruiter-registration-requests` | Модерация регистраций работодателей |
| `/projects` | Лента проектов: чтение STUDENT/RECRUITER/ADMIN, CUD только ADMIN |
| `/admin/analytics` | Сводки по событиям и по сущностям (пользователи/студенты/рекрутеры) |

Полные пути методов, HTTP-глаголы и матрица ролей: [api-endpoints.md](./api-endpoints.md); детали полей — в Swagger.

---

## 8. Доменные подсистемы (что есть и как устроено)

### 8.1. Студенты (`/student` + публичное `/public/students`)

- **Сущность** `StudentEnt`: вложенные `UserInformation`, `ContactInformation`, `TimeStamped`, связь со специальностью, M:N навыки, курс (`CourseEnum`), флаг **`public_profileConsent`**, денормализованный **`profileTextScore`**, опциональный **`manualSortOrder`** (ручной приоритет в каталоге).
- **Фильтрация:** `StudentSpecifications.byFilters` + `FilterStudentReq`; карточки с **`catalogVisible=false`** скрыты от не-админов в выдачах и в `GET /student/{id}` (404).
- **Сортировка:** только из тела `FilterStudentReq` (`StudentSortResolver`); query `sort` у `Pageable` **не** используется. Режим релевантности: **`manualSortOrder` ASC** (NULL в конце на PostgreSQL), затем `imagePath` ASC, `profileTextScore` DESC, дата создания DESC; можно сортировать только по **`MANUAL_SORT_ORDER`**. **Важно:** нельзя использовать `Sort.Order.nullsLast()` вместе с `findAll(Specification, …)` — Spring Data JPA с Criteria не поддерживает null precedence (на PostgreSQL для ASC NULL по умолчанию в конце).
- **Публичная витрина:** только `publicProfileConsent = true` и `catalogVisible = true`; укороченный `StudentCardDTO` без расширения PII (поле `manualSortOrder` в ответе — для прозрачности порядка).
- **Согласие и ручной порядок:** **`publicProfileConsent`** и **`manualSortOrder`** — при **`POST /student`** и **`POST /student/extended`** (опционально в теле), иначе у **`PUT`/`PATCH /student/{id}`**; сброс номера — `clearManualSortOrder: true`. Если при создании не передать — согласие **false**, ручной номер **не задан** (`NULL`).

### 8.2. Заявки (`/request`)

- Связь рекрутер + студент + чат (`app_chat_id`), результат (`ResultEnum`), этапы согласования.
- Студент **не** создаёт заявки; скрытие студента с `catalogVisible=false` для рекрутера при создании заявки (404).
- Админ: просмотр, фильтр, удаление.

### 8.3. Чаты (`/chat` + WebSocket)

- **Доступ к истории REST:** до «разрешённого» результата заявки рекрутер и студент видят **только системные** сообщения; полная история — после принятия; **админ** — полная история сразу.
- **WS:** `/topic/chats/{chatId}` — основной топик; **`/topic/chats/{chatId}/staff`** — пользовательские сообщения до принятия (для админского UI). Публикация событий при отправке/системных событиях (`ChatSystemEvent`).

### 8.4. Рекрутеры и пользователи

- Пользователь `users`: роль, опционально `student_id`, `recruiter_id`.
- `GET /recruiter/me` — профиль текущего рекрутера; первая заявка может обогащать данные.

### 8.5. Регистрация

- **Студент:** `POST /auth/register-student` + лимиты, пароль; сразу создаётся черновик карточки (`catalogVisible=false`, `PENDING_APPROVAL`) до модерации.
- **Работодатель:** заявка в БД, одобрение админом (`/admin/recruiter-registration-requests`).

### 8.6. Лента проектов (`site_projects`)

- Один ресурс `/projects`: `POST /filter` и `GET /{id}` для **STUDENT** / **RECRUITER** / **ADMIN**; CUD, `POST /reorder`, `…/{id}/students` — только **ADMIN**.
- Видимость в сервисе: админ — все записи + `students`; рекрутер — окно публикации + `students`; студент — окно публикации, `students = null`.
- Анонимная главная: `GET /public/vitrina/home` (тот же list: `visible_to_anonymous` + окно, без участников). Отдельного `/public/projects` нет.

### 8.7. Аналитика (first-party)

- Таблица событий; **`POST /public/analytics/events`** — `permitAll`, rate limit по IP (`app.analytics`).
- **`POST /admin/analytics/summary`** — агрегация по `path` за интервал.
- **`POST /admin/analytics/entity-population`** — срезы по `users` (в т.ч. по ролям), `students`, `recruiters`; опционально новые студенты/рекрутеры за **[from, to)** по `created_at`.

### 8.8. Файлы

- Загрузка аватара студента, вложения чата — на диск под `app.file.path`; раздача через `/main/photo/{image_path}`.

---

## 9. WebSocket (аспекты)

| Параметр | Значение |
|----------|----------|
| SockJS endpoint | `/ws` |
| Брокер (простой) | префикс `/topic` |
| Application prefix | `/app` |
| CORS для WS | из `app.security.cors.allowed-origins` как `setAllowedOriginPatterns` |

**Продакшен-аспект:** handshake `/ws` в конфиге разрешён широко (`permitAll` на HTTP); усиление (JWT при connect) — продуктовое решение на будущее.

---

## 10. Обработка ошибок и контракт API

`GlobalExceptionHandler` (централизованно):

- **401** — `AuthenticationException`
- **403** — `AccessDeniedException`
- **400** — валидация (`MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`), `BadRequestException`, превышение размера загрузки
- **404** — `NotFoundException`
- **409** и др. — по `ApiException` / `DataIntegrityViolationException` где применимо
- **500** — прочие исключения как `INTERNAL_ERROR` / «Unexpected error»

Тело ошибок — см. `ErrorResponse` в коде.

---

## 11. Нефункциональные и сквозные аспекты

| Аспект | Реализация / риск |
|--------|-------------------|
| **Транзакции** | Сервисы помечены `@Transactional` где нужна атомарность; списки студентов с маппингом полного DTO — в `@Transactional(readOnly = true)` чтобы избежать `LazyInitializationException`. |
| **Open-in-view** | `false` — нельзя рассчитывать на ленивую подгрузку вне сервисного слоя. |
| **Пагинация** | Spring `Pageable` (`page`, `size`); для студентов сортировка из тела, не из query `sort`. |
| **Поиск студентов** | PostgreSQL `pg_trgm` / `similarity` / `word_similarity` в спецификации. |
| **Идемпотентность** | Не гарантируется глобально для POST (заявки, сообщения, аналитика). |
| **Rate limit** | Аналитика — в приложении по IP; регистрация — отдельные лимиты в конфиге. |
| **CORS + cookies** | Нужны `allowed-origins` и `credentials: include` на фронте. |
| **Логирование** | Logback из Boot; паттерн и файлы из `app.logging`. |

---

## 12. Тестирование

- **Unit / Mockito:** сервисы (заявки, чат, пользователи и т.д.), резолвер сортировки студентов.
- **`@WebMvcTest`:** контроллеры с `MethodSecurityTestConfig`, `@MockBean`, `@WithMockUser`.
- **Интеграция:** `AbstractPostgresIntegrationTest` + Testcontainers PostgreSQL 16, Flyway; пример — `FullRoleJourneysIntegrationTest` (сквозной HTTP с cookie), `NewFeaturesIntegrationTest`.
- Без Docker интеграционные тесты с контейнером **отключены** (`disabledWithoutDocker`), сборка остаётся зелёной.

Подробности: [testing.md](./testing.md).

---

## 13. Документация в репозитории

| Файл | Содержание |
|------|------------|
| [api-endpoints.md](./api-endpoints.md) | Полный справочник REST: метод, путь, `@PreAuthorize`, публичные пути, исключения (например `institution/filter`) |
| [backend.md](./backend.md) | Устройство backend, домен, чат, файлы, новые фичи |
| [frontend.md](./frontend.md) | Контракт для фронта, CORS, роли, публичные пути |
| [roles-product-journeys.md](./roles-product-journeys.md) | Продуктовые сценарии по ролям |
| [roadmap.md](./roadmap.md) | Пути по ролям, бэклог |
| [testing.md](./testing.md) | Стратегия тестов, Docker |
| **project-passport.md** (этот файл) | Сводный паспорт |

---

## 14. Сквозные сценарии (кому что доступно)

### 14.1. Аноним (без cookie)

- Регистрация студента / заявка на регистрацию рекрутера.
- **`GET /public/students/{id}`**, **`POST /public/students/cards`** — только согласие + `catalogVisible`.
- **`GET /public/vitrina/home`**, **`POST /public/analytics/events`**.
- Swagger, `/main/status`, фото по пути.

### 14.2. Рекрутер (`GUEST` / `USER`)

- Логин, refresh, logout.
- Каталог: **`POST /student/cardsFilter`**, **`POST /student/filter`**, **`GET /student/{id}`** (с правилами `catalogVisible`).
- Заявка **`POST /request`**, чаты, **`GET /recruiter/me`**, WebSocket `/topic/chats/{id}` после принятия — полная переписка в REST/WS по правилам gated-логики.

### 14.3. Студент (`STUDENT`)

- **`GET /student/me`**, **`PATCH /student/me`**, CRUD своего опыта/учёбы/портфолио; чтение справочников через `/filter`; решение по заявке **`POST /request/{id}/student-decision`**, чаты своей пары; **нельзя** создавать заявку и сущности company/education/skill.

### 14.4. Админ (`ADMIN`)

- Всё вышеперечисленное по правилам метода + модерация пользователей/справочников/заявок, удаление сообщений, полный чат, **`/projects`** (CUD), **`/admin/analytics/summary`**, **`/admin/analytics/entity-population`**, заявки на регистрацию рекрутеров.

### 14.5. Сквозной «happy path» (интеграционный тест)

`FullRoleJourneysIntegrationTest`: админ создаёт справочники и студента → пользователь студента → гость видит каталог и создаёт заявку → студент принимает → переписка и WS-сценарии → админ модерирует.

---

## 15. Ограничения и бэклог (кратко)

- Отдельного списка заявок для студента в REST может не быть — UI опирается на другие каналы или будущий эндпоинт (см. [roadmap.md](./roadmap.md)).
- Усиление безопасности WebSocket handshake при необходимости.
- Аналитика: хранение и правовые требования (минимизация PII) — на стороне продукта.

---

## 16. Версионирование API

Версия для OpenAPI в **`app.swagger.version`** (`application.yaml`). Обратная совместимость REST не формализована отдельным URL versioning — клиенты ориентируются на Swagger и деплой-ноты.

---

*Документ можно дополнять при крупных изменениях домена или контракта. Источник истины по HTTP — код контроллеров + Swagger.*
