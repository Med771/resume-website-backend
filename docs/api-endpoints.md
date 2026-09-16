# Справочник HTTP API

Актуально для кода в репозитории. **`server.servlet.context-path` не задан** — пути от корня хоста (например `https://api.example.com/auth/login`).

- **Аутентификация:** JWT в **HttpOnly cookie** (имена из `application.yaml`, обычно `ACCESS_TOKEN`, `REFRESH_TOKEN`). Для браузера: `credentials: 'include'`.
- **Роли в `@PreAuthorize`:** `GUEST`, `USER`, `STUDENT`, `ADMIN` (в токене/Principal — префикс `ROLE_`).
- **Пагинация Spring Data:** query `page`, `size` (0-based). Параметр **`sort` в query для эндпоинтов с `Pageable` не используется** там, где в Swagger указано «без параметра sort» или для списков студентов (сортировка — в теле `FilterStudentReq`).
- **Детали DTO, коды ошибок:** [Swagger UI](http://localhost:8080/swagger-ui.html) (`/swagger-ui.html`, `/v3/api-docs`).

---

## Публичные пути (`permitAll`, без JWT)

| Метод | Путь | Назначение |
|-------|------|------------|
| POST | `/auth/register-recruiter` | Заявка на регистрацию работодателя; **204**, cookie не выдаются |
| POST | `/auth/register-student` | Саморегистрация студента + черновик карточки; **204** + Set-Cookie (см. `app.registration`) |
| POST | `/auth/login` | Вход; **204** + Set-Cookie |
| POST | `/auth/refresh` | Новый access; **204** + Set-Cookie |
| POST | `/auth/logout` | Очистка cookie; **204** |
| GET | `/public/vitrina/home` | Витрина главной: `{ students: StudentCardDTO[], projects: SiteProjectDTO[] }` |
| POST | `/public/analytics/events` | Запись события аналитики; **204**; при лимите IP — **429** |
| GET | `/main/status` | Liveness; **204** |
| GET | `/main/photo/{image_path}` | Байты файла изображения из хранилища |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | OpenAPI / UI |
| OPTIONS | `/**` | CORS preflight |

WebSocket handshake: **`/ws/**`** также `permitAll` на уровне HTTP (см. [backend.md](./backend.md)).

---

## `/auth` — сессия и регистрация

Все методы выше в таблице публичных. Кратко:

| Метод | Путь | Ответ | Примечание |
|-------|------|-------|------------|
| POST | `/auth/register-recruiter` | 204 | Тело `RecruiterSelfRegistrationReq` |
| POST | `/auth/register-student` | 204 | Тело `StudentAccountRegistrationReq`; создаёт черновик карточки (`catalogVisible=false`); cookie как после login |
| POST | `/auth/login` | 204 | `LoginRequest` |
| POST | `/auth/refresh` | 204 | Refresh из cookie |
| POST | `/auth/logout` | 204 | Очистка обеих cookie |

---

## `/main` — служебное и файлы

| Метод | Путь | Доступ | Описание |
|-------|------|--------|----------|
| GET | `/main/status` | публично | **204** — сервис жив |
| GET | `/main/photo/{image_path}` | публично | Тело: байты изображения, заголовок `Content-Type` |

---

## `/public/vitrina` — витрина главной (без входа)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/public/vitrina/home` | `PublicHomeVitrinaDTO`: топ карточек студентов (`publicProfileConsent`, `catalogVisible`) и проектов (`visibleToAnonymous` + окно публикации); лимиты — `app.vitrina.home` |

---

## `/public/students`, `/public/projects` — устаревший публичный доступ

Эндпоинты сохранены в коде, но **не** в `permitAll` — анонимный запрос вернёт **401**. Используйте `GET /public/vitrina/home` на главной и authenticated API после входа.

---

## `/projects` — Projects (авторизованные)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/projects` | **STUDENT**, **GUEST**, **USER** | Витрина: все проекты в окне публикации, включая `visibleToAnonymous=false` |

---

## `/vacancies` — Vacancies / VacancyApplications

Витрина **только для авторизованных** (**STUDENT**, **GUEST**, **USER**). Публикация после модерации админом. Отдельного `/public/vacancies` нет.

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/vacancies` | **STUDENT**, **GUEST**, **USER** | `PageResponse<VacancyCardDTO>` — только **PUBLISHED** в окне дат; query-фильтры `FilterVacancyReq` |
| GET | `/vacancies/{id}` | те же | `VacancyDTO`; **404** для чужой неопубликованной |
| GET | `/vacancies/mine` | **GUEST**, **USER** | Все вакансии текущего рекрутёра (включая модерацию) |
| POST | `/vacancies` | **GUEST**, **USER** | Черновик **DRAFT**; нужен профиль рекрутёра |
| PUT | `/vacancies/{id}` | владелец | Только **DRAFT** / **REJECTED** |
| POST | `/vacancies/{id}/submit-for-review` | владелец | → **PENDING_REVIEW** |
| POST | `/vacancies/{id}/close` | владелец | **PUBLISHED** → **CLOSED** |
| DELETE | `/vacancies/{id}` | владелец / **ADMIN** | **ARCHIVED** или удаление пустого **DRAFT** |
| POST | `/vacancies/{id}/applications` | **STUDENT** | Отклик; `catalogVisible=false` запрещён; **201** |
| GET | `/vacancies/applications/mine` | **STUDENT** | Мои отклики |
| POST | `/vacancies/applications/{applicationId}/withdraw` | **STUDENT** | **204**; только **SUBMITTED** |
| GET | `/vacancies/{id}/applications` | владелец | Отклики на вакансию |
| POST | `/vacancies/{id}/applications/{applicationId}/accept` | владелец | **ACCEPTED**, чат, system `VACANCY_APPLICATION_ACCEPTED` |
| POST | `/vacancies/{id}/applications/{applicationId}/reject` | владелец | **REJECTED**; опционально `rejectionReason` |

Переписка по отклику открывается **после accept** (как у заявок после `STUDENT_CONFIRMED`), через общий `MessagingGate` (заявка **или** принятый отклик).

---

## `/admin/vacancies` — VacancyModerationAdmin

`@PreAuthorize("hasRole('ADMIN')")`.

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/admin/vacancies/filter` | Очередь; тело `FilterVacancyModerationReq` (по умолчанию **PENDING_REVIEW**) |
| GET | `/admin/vacancies/{id}` | Полная карточка |
| POST | `/admin/vacancies/{id}/approve` | → **PUBLISHED** |
| POST | `/admin/vacancies/{id}/reject` | → **REJECTED**; тело `VacancyModerationRejectReq`; **204** |

---

## `/public/analytics`

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/public/analytics/events` | **204**; тело `AnalyticsEventInReq`; rate limit по IP (`app.analytics`) |

---

## `/student` — карточки (вход обязателен)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/student/me` | **STUDENT** | Своя карточка `StudentDTO`; **404** если нет привязки |
| PATCH | `/student/me` | **STUDENT** | Частичное обновление анкеты (`PatchStudentMeReq`); `null` — не менять; `catalogVisible` студенту недоступен |
| GET | `/student/{id}` | **STUDENT**, **GUEST**, **USER**, **ADMIN** | Полный `StudentDTO`; `catalogVisible=false` для не-админа → **404**; требуется **APPROVED** (кроме ADMIN) |
| POST | `/student/cardsFilter` | **STUDENT**, **GUEST**, **USER**, **ADMIN** | `PageResponse<StudentCardDTO>`; тело `FilterStudentReq`; скрытые карточки только у админа; **APPROVED** обязателен (кроме ADMIN) |
| POST | `/student/filter` | **STUDENT**, **GUEST**, **USER**, **ADMIN** | `PageResponse<StudentDTO>`; те же правила |
| POST | `/student/photo/{id}` | **ADMIN** | `multipart/form-data`, часть **`avatarFile`**; **204** |
| POST | `/student` | **ADMIN** | Создание; **201**; опционально **`publicProfileConsent`**, **`manualSortOrder`** в теле `AddStudentReq` |
| POST | `/student/extended` | **ADMIN** | Создание с вложенными сущностями; **201**; те же опциональные поля, что и в `AddStudentReq` |
| PUT | `/student/{id}` | **ADMIN** | Полное обновление `UpdateStudentReq`; **200**; ручной порядок: `manualSortOrder` / `clearManualSortOrder` |
| PATCH | `/student/{id}` | **ADMIN** | Частичное `PatchStudentReq`; **200**; то же для `manualSortOrder` |
| DELETE | `/student/{id}` | **ADMIN** | Каскадное удаление связанных данных; **204** |

Роль **STUDENT** с **APPROVED** может читать каталог через `GET /student/{id}`, `POST /student/cardsFilter`, `POST /student/filter`. Пользователи **PENDING_APPROVAL** получают **403**.

---

## `/request` — заявки

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/request/{id}` | **ADMIN** | `RequestDTO` |
| POST | `/request/filter` | **ADMIN** | Страница заявок по `FilterRequestReq` |
| POST | `/request` | **GUEST**, **USER**, **ADMIN** | Создание заявки рекрутером; **STUDENT** — **403**; **201**; студент с `catalogVisible=false` для не-админа — **404** |
| POST | `/request/{id}/student-decision` | **STUDENT** | Тело `StudentRequestDecisionReq`; **204** |
| DELETE | `/request/{id}` | **ADMIN** | **204** |

---

## `/chat` — REST чата

Все требуют **`isAuthenticated()`**, кроме удаления сообщения.

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/chat` | любой аутентифицированный | Список чатов с превью (`PageResponse<ChatSummaryDTO>`) |
| GET | `/chat/{chatId}/summary` | любой | Сводка чата |
| GET | `/chat/{chatId}/messages` | любой | Страница сообщений (видимость USER/SYSTEM по правилам домена) |
| POST | `/chat/{chatId}/messages` | любой | Текст; **201** `ChatMessageDTO`; тело `PostChatMessageReq` |
| POST | `/chat/{chatId}/messages/attachment` | любой | `multipart`: часть **`file`**, опционально **`body`**; **201** |
| PATCH | `/chat/{chatId}/messages/{messageId}` | любой | Редактирование; `PatchChatMessageReq` |
| POST | `/chat/{chatId}/read` | любой | Отметка прочитанного; `MarkChatReadReq`; **204** |
| DELETE | `/chat/{chatId}/messages/{messageId}` | **ADMIN** | Мягкое удаление; **204** |
| GET | `/chat/{chatId}/context` | **ADMIN** | Связанные заявки и отклики на вакансии (`ChatContextDTO`); **200** |
| DELETE | `/chat/{chatId}` | **ADMIN** | Полное удаление чата, заявок и откликов по `appChatId`; **204** |

Подробности видимости сообщений: [backend.md](./backend.md).

---

## `/recruiter`

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| GET | `/recruiter/me` | **GUEST**, **USER**, **ADMIN** | Свой профиль или **404** (до первой заявки с данными) |
| GET | `/recruiter/{id}` | **GUEST**, **USER**, **ADMIN** | Карточка по UUID |
| POST | `/recruiter` | **ADMIN** | Создание; **201** |
| POST | `/recruiter/filter` | **ADMIN** | Фильтр страниц |
| PUT | `/recruiter/{id}` | **ADMIN** | Полное обновление |
| PATCH | `/recruiter/{id}` | **ADMIN** | Частичное обновление |
| DELETE | `/recruiter/{id}` | **ADMIN** | **204** |

---

## `/user` — пользователи (админ)

| Метод | Путь | Роли | Описание |
|-------|------|------|----------|
| POST | `/user/filter` | **ADMIN** | `FilterUserReq` |
| POST | `/user` | **ADMIN** | Создание; для ЛК студента: роль **STUDENT** + `studentId`; **201** |
| DELETE | `/user/{id}` | **ADMIN** | Удаление **USER** / **STUDENT** по UUID; **204** |

---

## `/admin/projects` — Projects (админ)

Класс контроллера: `@PreAuthorize("hasRole('ADMIN')")` на все методы.

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/admin/projects` | Все проекты в порядке `sortOrder` |
| POST | `/admin/projects` | Создание; **201** `CreateSiteProjectReq` |
| PUT | `/admin/projects/{id}` | Обновление `UpdateSiteProjectReq` |
| DELETE | `/admin/projects/{id}` | **204** |
| POST | `/admin/projects/reorder` | Порядок `orderedIds`; **204** |
| GET | `/admin/projects/{id}/students` | UUID привязанных студентов |
| POST | `/admin/projects/{id}/students` | Привязка студентов; тело `SiteProjectStudentsReq`; **204** |
| DELETE | `/admin/projects/{id}/students` | Отвязка студентов; **204** |

---

## `/admin/analytics` — отчёты (админ)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/admin/analytics/summary` | Тело `AnalyticsSummaryReq`; ответ `AnalyticsSummaryDTO` (события `PAGE_VIEW` по `path`) |
| POST | `/admin/analytics/entity-population` | Тело опционально `EntityPopulationSummaryReq` (`from`/`to` вместе или оба `null`); ответ `EntityPopulationSummaryDTO` — пользователи по ролям, всего студентов/рекрутеров, при окне — новые по `created_at` |

---

## `/admin/recruiter-registration-requests` — модерация регистрации работодателей

Класс: `@PreAuthorize("hasRole('ADMIN')")`.

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/admin/recruiter-registration-requests/filter` | Тело опционально `FilterRecruiterRegistrationReq` |
| POST | `/admin/recruiter-registration-requests/{id}/approve` | Одобрение; ответ `RecruiterRegistrationApproveResultDTO` |
| POST | `/admin/recruiter-registration-requests/{id}/reject` | Отклонение; опционально тело `RecruiterRegistrationRejectReq`; **204** |

---

## Справочники: `/company`, `/skill`, `/speciality`, `/education`

Чтение после входа (студент выбирает id для резюме). CUD только админ.

| Метод | Путь | Роли |
|-------|------|------|
| GET | `/{resource}/{id}` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/{resource}/filter` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/{resource}` | **ADMIN** |
| PUT | `/{resource}/{id}` | **ADMIN** |
| DELETE | `/{resource}/{id}` | **ADMIN** |

`{resource}` ∈ `company`, `skill`, `speciality`, `education`. Тела — DTO из пакета `…dto` соответствующего модуля.

---

## `/experience` — опыт работы

Своё всегда; чужое GET — только при `catalogVisible`. Запись: студент — только своя карточка (`studentId` в теле игнорируется), админ — любой `studentId`.

| Метод | Путь | Роли |
|-------|------|------|
| GET | `/experience/{id}` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/experience/filter` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/experience` | **STUDENT**, **ADMIN** |
| PUT | `/experience/{id}` | **STUDENT**, **ADMIN** |
| DELETE | `/experience/{id}` | **STUDENT**, **ADMIN** |

---

## `/portfolio` — портфолио

Те же правила доступа, что у `/experience`.

| Метод | Путь | Роли |
|-------|------|------|
| GET | `/portfolio/{id}` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/portfolio/filter` | **STUDENT**, **RECRUITER**, **ADMIN** |
| POST | `/portfolio` | **STUDENT**, **ADMIN** |
| PUT | `/portfolio/{id}` | **STUDENT**, **ADMIN** |
| DELETE | `/portfolio/{id}` | **STUDENT**, **ADMIN** |

---

## `/institution` — учёба студента (связь студент–образование)

Те же правила доступа, что у `/experience`.

| Метод | Путь | Роли | Примечание |
|-------|------|------|------------|
| GET | `/institution/{id}` | **STUDENT**, **RECRUITER**, **ADMIN** | |
| POST | `/institution/filter` | **STUDENT**, **RECRUITER**, **ADMIN** | Если в фильтре передан `educationId`, внутри вызывается проверка **админа** (`SecurityHelper.checkAdminRoleForFilter`) |
| POST | `/institution` | **STUDENT**, **ADMIN** | |
| PUT | `/institution/{id}` | **STUDENT**, **ADMIN** | |
| DELETE | `/institution/{id}` | **STUDENT**, **ADMIN** | |

---

## Сводка по доступу к «фильтрам» справочников

| Ресурс | `POST …/filter` |
|--------|-----------------|
| company, skill, speciality, education | **STUDENT**, **RECRUITER**, **ADMIN** (CUD только **ADMIN**) |
| experience, portfolio, institution | **STUDENT**, **RECRUITER**, **ADMIN**; CUD — **STUDENT** (своя карточка) и **ADMIN** |

---

## WebSocket (не HTTP REST)

| Назначение | Путь / префикс |
|------------|----------------|
| SockJS | `/ws` |
| STOMP broker | подписки на `/topic/...` |
| Основной топик чата | `/topic/chats/{chatId}` |
| Сообщения до принятия заявки (для админского UI) | `/topic/chats/{chatId}/staff` |
| Inbox-уведомления пользователя (badge, toast, список чатов) | `/topic/users/{userId}/inbox` — payload `UserInboxNotificationDTO` |

`{chatId}` — UUID прикладного чата (`appChatId` в `RequestDTO`).

---

## Связанные документы

- [backend.md](./backend.md) — домен, чаты, файлы, релизный чеклист  
- [frontend.md](./frontend.md) — CORS, cookie, типичные ошибки  
- [roles-product-journeys.md](./roles-product-journeys.md) — продуктовые сценарии  
- [project-passport.md](./project-passport.md) — общий паспорт проекта  

При изменении контроллеров обновляйте этот файл и при необходимости аннотации `@Operation` в коде.
