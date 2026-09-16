# Продуктовые сценарии по ролям: от регистрации до повседневного использования

Документ описывает **фактическое поведение backend** репозитория `resume-web-site-backend` по состоянию кода и внутренней документации (`docs/backend.md`, `docs/frontend.md`, `docs/roadmap.md`). Для точных полей DTO и кодов ответов используйте **Swagger UI** (`/swagger-ui.html`).

---

## 1. Назначение продукта (кратко)

Сервис связывает **студентов** (карточки резюме) и **работодателей / рекрутеров**: каталог, заявка на контакт, **внутренний чат** с поэтапным раскрытием переписки до принятия заявки студентом, модерация и администрирование справочников и пользователей.

---

## 2. Версии стека и документация зависимостей

| Компонент | Версия в проекте | Официальная документация |
|-----------|------------------|---------------------------|
| Java | 21 | [Oracle / OpenJDK](https://docs.oracle.com/en/java/javase/21/) |
| Spring Boot (parent) | 3.5.7 | [Spring Boot 3.5 Reference](https://docs.spring.io/spring-boot/docs/3.5.7/reference/htmlsingle/) |
| Spring Security (BOM override в `pom.xml`) | 7.0.0 | [Spring Security Reference](https://docs.spring.io/spring-security/reference/) |
| Spring Data JPA / Web / WebSocket | через Boot BOM | см. Spring Boot Reference, разделы *Data Access*, *Web*, *WebSocket* |
| PostgreSQL JDBC Driver | 42.7.8 | [pgJDBC Documentation](https://jdbc.postgresql.org/documentation/) |
| Flyway | 10.20.1 | [Flyway Documentation](https://documentation.red-gate.com/flyway) |
| springdoc-openapi (WebMVC UI) | 2.8.15 | [springdoc-openapi](https://springdoc.org/) |
| JJWT | 0.12.6 | [jjwt README / wiki](https://github.com/jwtk/jjwt) |
| MapStruct | 1.5.5.Final | [MapStruct Reference](https://mapstruct.org/documentation/stable/reference/html/) |
| Lombok | 1.18.42 | [Project Lombok](https://projectlombok.org/features/all) |
| Testcontainers (JUnit Jupiter, PostgreSQL) | через Boot BOM | [Testcontainers for Java](https://java.testcontainers.org/) |
| JaCoCo (отчёт покрытия) | 0.8.12 | [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/) |

Артефакт Maven: `resume-web-site-backend` **0.0.1-SNAPSHOT** (см. `pom.xml`).

---

## 3. Модель ролей

| Роль | Где живёт | Описание |
|------|-----------|----------|
| **GUEST** | Не в БД | Неаутентифицированный пользователь: `permitAll` (`/public/*`, `/auth/*`, `/main/*`, …) |
| **STUDENT** | `RoleEnum` + JWT | Студент: ЛК, онбординг резюме, отклики на вакансии, чаты |
| **RECRUITER** | `RoleEnum` + JWT | Рекрутер: каталог, заявки, вакансии, чаты |
| **ADMIN** | `RoleEnum` + JWT | Модерация, справочники, пользователи, аналитика |

В JWT и Spring Security authorities: **`ROLE_STUDENT`**, **`ROLE_RECRUITER`**, **`ROLE_ADMIN`**.

Роли **`USER`** в коде **нет** — используйте **RECRUITER**.

**Учётная запись admin** создаётся при старте (`application.yaml` → `app.user.logins`): `admin` / `admin`.

---

## 4. Общий каркас: сессия, публичные точки, файлы

### 4.1. Аутентификация без роли (гость сети)

Доступно без входа (`permitAll` в `SecurityConfig`):

| Действие | HTTP | Назначение |
|----------|------|------------|
| Регистрация студента | `POST /auth/register-student` | Создание пользователя `STUDENT` и черновика карточки (`catalogVisible=false`), выдача **HttpOnly** cookie; почта подтверждается отдельно |
| Заявка на регистрацию работодателя | `POST /auth/register-recruiter` | Запись со статусом `PENDING`, **без** cookie и без возможности входа до одобрения админом |
| Вход | `POST /auth/login` | Установка пары JWT-cookie |
| Обновление access | `POST /auth/refresh` | Новый access по refresh-cookie |
| Выход | `POST /auth/logout` | Очистка cookie |
| Публичная витрина главной | `GET /public/vitrina/home` | Карточки студентов и проекты (`visible_to_anonymous` + окно публикации) |
| Аналитика (first-party) | `POST /public/analytics/events` | Лимит `app.analytics.rate-limit-per-ip-per-minute`; без cookie |
| Статус API / картинки | `GET /main/status`, `GET /main/photo/{image_path}` | Живость, отдача файлов из `app.file.path` |
| Документация | Swagger, OpenAPI | См. `app.swagger` в конфиге |

Клиент с cookie должен вызывать API с **`credentials: 'include'`** (или axios `withCredentials: true`) и origin из `app.security.cors.allowed-origins` — иначе браузер отрежет CORS или не отправит cookie.

### 4.1.1. Анонимный посетитель сайта (витрина без регистрации)

Сценарий «лендинг / каталог до логина»:

1. **Карточки:** `POST /public/students/cards` с тем же телом фильтра, что и у рекрутера (`FilterStudentReq` + пагинация `page`/`size`), но сервер дополнительно требует **`public_profile_consent = true`** и **`catalog_visible = true`**. Параметры сортировки — поля **`sortBy`**, **`sortDirection`**, **`useDefaultRanking`** в теле (произвольный `sort=` из query для этих методов не используется).
2. **Деталь:** `GET /public/students/{id}` — **404**, если нет согласия, `catalog_visible = false` или запись скрыта по тем же правилам, что и для не-админов на `GET /student/{id}`.
3. **Проекты:** `GET /public/vitrina/home` — только опубликованные и с `visible_to_anonymous` (поле `projects`).
4. **Аналитика:** при согласии на cookies/трекинг (продуктово) — `POST /public/analytics/events` с типом **`PAGE_VIEW`** и т.д.; при превышении лимита с одного IP — **429**.

### 4.2. После входа (любая роль)

Повседневный цикл: работа под cookie → при 401 — `POST /auth/refresh` → повтор запроса → при необходимости `logout`.

**WebSocket:** точка `/ws` (SockJS + STOMP), топики `/topic/chats/{chatId}` и `/topic/chats/{chatId}/staff` — см. `docs/frontend.md` и раздел 7 ниже.

---

## 5. Роль STUDENT (студент)

### 5.1. Как появляется аккаунт

**Путь A — саморегистрация**

1. `POST /auth/register-student` с телом `StudentAccountRegistrationReq`: логин, пароль, подтверждение пароля, **email**, телефон; опционально имя (`firstName`), фамилия (`lastName`), отчество (`middleName`), город. Telegram и `phoneVerificationId` не нужны.
2. На указанную почту уходит **6-значный** код. Подтверждение: `POST /auth/confirm-email` с cookie сессии (роль **STUDENT**). Повторная отправка: `POST /auth/resend-email-confirmation`. Лимиты — `app.registration.email-confirm-max-attempts-per-hour` / `email-resend-max-per-hour`.
3. Ограничения регистрации: лимит попыток с одного IP (`app.registration.rate-limit-per-ip-per-hour`), политика пароля (`min-password-length`, `require-letter-and-digit`).
4. Создаётся пользователь **`STUDENT`** (`PENDING_APPROVAL`, `emailVerified=false`) и **черновик** карточки (`catalogVisible=false`); сразу выдаются cookie. `GET /auth/me` отдаёт `emailVerified`. Дозаполнение анкеты — `PATCH /student/me` (курс **1–5**, пол `gender`) и CRUD `/experience`, `/institution`, `/portfolio`. Справочники студент только читает.
5. Админ одобряет аккаунт (`POST /admin/account-approvals/{id}/approve`) **только после** подтверждения почты; иначе **400**. `PENDING_APPROVAL` не подменяется флагом почты.

**Путь B — администратор готовит витрину**

1. Админ создаёт карточку: `POST /student`, `POST /student/extended`, фото `POST /student/photo/{id}` и т.д.
2. Админ создаёт пользователя: `POST /user` с `role: STUDENT` и обязательным `studentId` (связь 1:1 пользователь ↔ студент).

### 5.2. Модерация «на витрине»

Студенты с **`catalogVisible=false`** (черновик после регистрации или до одобрения аккаунта):

- в выдачах **`POST /student/cardsFilter`** и **`POST /student/filter`** видны **только администратору**;
- для **`GET /student/{id}`** не-админ получает **404** (как при отсутствии id);
- рекрутер при попытке заявки на такого студента также получает **404**.

Продуктовый смысл: студент после саморегистрации **может войти в ЛК** и заполнять анкету, но **не виден рекрутерам**, пока админ не одобрит аккаунт (`catalogVisible=true` при approve) и не настроит публичное согласие при необходимости.

### 5.3. Повседневное использование (ЛК и отклики)

| Возможность | Эндпоинт / механизм |
|-------------|---------------------|
| Просмотр своей карточки | `GET /student/me` |
| Редактирование резюме | `PATCH /student/me`; CRUD `/experience`, `/institution`, `/portfolio` (только своя карточка; `catalogVisible` студенту недоступен) |
| Справочники для анкеты | `POST /skill/filter`, `/company/filter`, `/education/filter`, `/speciality/filter` и GET по id (CUD справочников — только админ) |
| Лента проектов | `POST /projects/filter`, `GET /projects/{id}` (окно публикации, без `students`) |
| Решение по заявке | `POST /request/{id}/student-decision` с `accept` и опциональным `comment` |
| Чаты | `GET /chat`, `GET /chat/{chatId}/summary`, `GET /chat/{chatId}/messages`, отправка текста/вложений, отметка прочитанного, правка **своих** сообщений — по тем же правилам, что и у других ролей (кроме админского удаления) |
| WebSocket | Подписка на `/topic/chats/{chatId}` для событий и переписки после «разрешения» заявки (см. раздел 7) |

**Ограничения текущей версии API (важно для UI):**

- **`POST /request`** студенту **запрещён**.
- Каталог **`POST /student/cardsFilter`**, **`POST /student/filter`**, **`GET /student/{id}`** для роли `STUDENT` в `@PreAuthorize` **не открыты** — студент **не** листает чужие анкеты через эти пути (см. `docs/roadmap.md`).
- Отдельного **`GET /request/my`** или фильтра заявок для студента **нет**: UI должен получать `id` заявки известным способом (например, из пуша, deeplink, админа или будущего эндпоинта из бэклога).

### 5.4. Типичный сюжет «дня из жизни»

Студент заходит в приложение → смотрит **только свой** профиль → видит входящие контакты (если фронт их откуда-то подставляет) → **принимает или отклоняет** заявку → после принятия ведёт **полноценную переписку** с рекрутером в чате и отмечает прочитанное; параллельно может получать системные сообщения (`REQUEST_SENT`, `STUDENT_ACCEPTED` / `STUDENT_REJECTED`, `ADMIN_JOINED`).

---

## 6. Роли GUEST и USER (работодатель / рекрутер в широком смысле)

В API права на ключевые сценарии работодателя **совпадают** (обе роли входят в одни и те же `hasAnyRole('GUEST', 'USER', 'ADMIN')` там, где разрешён каталог и заявки). Различие — **продуктовое / организационное** (например, демо-гость vs подтверждённый работодатель).

### 6.1. Как появляется аккаунт

**Путь A — саморегистрация с модерацией**

1. `POST /auth/register-recruiter` — заявка `RecruiterSelfRegistrationReq` (логин, пароли, компания, контакты). Пароль проверяется политикой; лимит IP для рекрутера — `app.registration.rate-limit-recruiter-per-ip-per-hour`.
2. Статус заявки **`PENDING`**. Войти по этим учётным данным **нельзя**, пока админ не одобрит.
3. Админ: `POST /admin/recruiter-registration-requests/filter` → `POST /admin/recruiter-registration-requests/{id}/approve` или `.../reject`.
4. При **approve** создаются сущности **Recruiter** и **User** с ролью **`USER`** (не `GUEST`), пароль берётся из сохранённого хеша заявки, пользователь связывается с рекрутером. После этого работодатель использует **`POST /auth/login`** с выбранным при регистрации логином.

**Путь B — создал только админ**

- `POST /recruiter` + привязка пользователя вручную (или иной внутренний процесс) — по факту см. админский сценарий; в коде явно: админ может создать рекрутера и управлять им через `PUT`/`PATCH`/`DELETE /recruiter/{id}`.

**Путь C — демо `guest` / `user` из конфига**

- Готовые учётки для стенда: роли **`GUEST`** и **`USER`** без прохождения формы регистрации.

### 6.2. Первый контакт с платформой

1. **Логин** → cookie.
2. **Каталог**: `POST /student/cardsFilter`, `POST /student/filter`, детальная карточка `GET /student/{id}` — карточки с **`catalogVisible=false`** в выдаче **только у админа**; для рекрутера они отфильтрованы / недоступны по id.
3. **Проекты:** `POST /projects/filter`, `GET /projects/{id}` — все в окне публикации, включая `visibleToAnonymous=false`; в DTO есть `students`.
4. **Профиль рекрутера «я»**: `GET /recruiter/me` — если ещё **нет** привязки рекрутера к пользователю, будет **404** до первой успешной заявки с полным набором полей компании (логика описана в Swagger у `POST /request`).

### 6.3. Заявка и чат

1. **`POST /request`** — создаётся заявка, результат в духе **`WAITING`**, создаётся или подбирается чат, в чат пишется системное событие **`REQUEST_SENT`**.
2. До «разрешённого» состояния заявки (принятие студентом — **`STUDENT_CONFIRMED`** и др. по бизнес-правилам сервиса) рекрутер и студент в **REST** видят **только системные** сообщения; пользовательские сообщения **не отображаются** (попытка отправить текст может завершиться ошибкой до принятия — см. интеграционный тест `FullRoleJourneysIntegrationTest`).
3. После положительного решения студента (`POST /request/{id}/student-decision` с `accept: true`) открывается полноценная переписка в REST и в основном WS-топике.
4. **Нет** отдельного списка «моих заявок рекрутера» одним вызовом — только обход через админский `POST /request/filter` или будущий эндпоинт (roadmap).

### 6.4. Справочники (чтение)

Для подготовки заявки и карточек доступны фильтры и `GET …/{id}` для: company, experience, portfolio, education, skill, speciality, institution — в рамках `@PreAuthorize` на контроллерах (для работодателя — как минимум чтение там, где разрешено; точная матрица — Swagger).

### 6.5. Типичный сюжет «дня из жизни»

Рекрутер логинится → ищет кандидатов в каталоге → открывает карточку → отправляет **заявку** → ждёт решения студента, видя в чате системную ленту → после согласия **общается**, правит свои сообщения, крепит файлы → при необходимости синхронно смотрит обновления через **WebSocket** на `/topic/chats/{appChatId}`.

---

## 7. Роль ADMIN (администратор)

### 7.1. Как появляется аккаунт

Обычно **начальные данные** через `app.user.logins` (пользователь `admin`) или отдельный процесс наката БД. Создание пользователя с ролью **`ADMIN`** через `POST /user` **запрещено** кодом (`UserServiceImpl` разрешает только `USER` и `STUDENT`).

### 7.2. Модерация и наполнение системы

| Зона | Примеры эндпоинтов |
|------|-------------------|
| Заявки на регистрацию работодателей | `POST /admin/recruiter-registration-requests/filter`, `POST .../{id}/approve`, `POST .../{id}/reject` |
| Пользователи | `POST /user/filter`, `POST /user`, `DELETE /user/{id}` (не админов) |
| Очередь аккаунтов | `GET /admin/account-approvals`, approve/reject; студенту approve требует `emailVerified` |
| Студенты | полный CRUD, расширенное создание, фото, `catalogVisible` / `publicProfileConsent`, `middleName` / `gender` |
| Рекрутеры | создание/фильтр/изменение/удаление |
| Заявки на контакт | `GET /request/{id}`, `POST /request/filter`, `DELETE /request/{id}` плюс то же создание, что у рекрутера |
| Справочники | POST/PUT/PATCH/DELETE по корням `/company`, `/institution`, … |
| Проекты | `POST /projects`, `PUT/DELETE /projects/{id}`, `POST /projects/reorder`, `GET/POST/DELETE /projects/{id}/students` |
| Чат | полная история в REST в любой фазе; **`DELETE /chat/{chatId}/messages/{messageId}`** — мягкое удаление сообщения |

### 7.3. Особенности чата для админа

- Видит **все** чаты в списке и **полную** историю, включая пользовательские сообщения «до принятия» студентом.
- Первое **пользовательское** сообщение админа в чате порождает системное **`ADMIN_JOINED`**.
- Для real-time «скрытых» от рекрутера сообщений до принятия заявки админский UI может подписываться на **`/topic/chats/{chatId}/staff`**.

### 7.4. Типичный сюжет «дня из жизни»

Админ обрабатывает **очередь регистраций** (работодатели и студенты `PENDING_APPROVAL`) → одобряет студентов (`catalogVisible=true`) → правит справочники → при спорных диалогах **входит в чат** и при необходимости **удаляет** сообщение или **удаляет** заявку → смотрит заявки фильтром `POST /request/filter`.

---

## 8. Вакансии и отклики (отдельно от заявок)

| Роль | Сценарий |
|------|----------|
| **GUEST** / **USER** | Создаёт вакансию (**DRAFT**), редактирует, **`POST …/submit-for-review`**, видит статусы и причину отклонения в **`GET /vacancies/mine`**, обрабатывает отклики (**accept** / **reject**) |
| **ADMIN** | Очередь **`POST /admin/vacancies/filter`**, **approve** → **PUBLISHED**, **reject** → **REJECTED** |
| **STUDENT** | Лента **`GET /vacancies`**, отклик, **«Мои отклики»**, withdraw; `catalogVisible=false` — отклик запрещён |
| Чат | После **accept** отклика — тот же чат рекрутер↔студент; переписка разрешена через `MessagingGate` (заявка в разрешённом статусе **или** принятый отклик) |

Жизненный цикл вакансии: **DRAFT** → **PENDING_REVIEW** → **PUBLISHED** / **REJECTED** → при необходимости **CLOSED** / **ARCHIVED**.

---

## 9. Сквозной сценарий «рекрутер ↔ студент» (склейка ролей)

1. **Админ** (или процесс) готовит справочники и карточку студента **или** студент сам регистрируется и ждёт модерации (`PENDING_APPROVAL`, `catalogVisible=false`).
2. **Рекрутер** (`GUEST`/`USER`) создаёт **`POST /request`** — появляется `appChatId`, статус заявки, системное сообщение в чате.
3. **Студент** вызывает **`POST /request/{id}/student-decision`** (нужно знать числовой `id`).
4. При **accept: true** — обе стороны получают полную переписку в REST и общий WS-топик; при **false** — отказ, соответствующие системные события.
5. **Админ** при необходимости подключается к переписке и модерирует.

Жизненный цикл поля **`result`** заявки включает значения `ResultEnum` (`WAITING`, `STUDENT_CONFIRMED`, `REFUSAL`, `SUCCESS`, устаревшие `CREATION`/`SYNC` и др.) — детали переходов см. в сервисе заявок и в `docs/roadmap.md` (там отмечено желание упростить/задокументировать legacy-коды).

---

## 10. Известные ограничения и риски (для честного описания «как в жизни»)

Сводка из `docs/roadmap.md` и кода:

- **WebSocket `/ws`** на уровне HTTP сейчас **permitAll**; для продакшена рекомендована привязка JWT к STOMP `CONNECT` и запрет подписки на чужие `chatId`.
- **Cookie `secure: false`** в примере конфига — для HTTPS в проде нужно **`secure: true`** и согласование **SameSite** с доменами фронта и API.
- Нет **списка заявок** «для студента» и «для рекрутера» без админских методов — планируется в бэклоге.
- Студент **не** имеет доступа к каталогу других студентов через текущие `@PreAuthorize`.

---

## 11. Где читать дальше

- [README.md](./README.md) — оглавление документации.
- [backend.md](./backend.md) — архитектура, домен, роли, чаты.
- [frontend.md](./frontend.md) — контракт для клиента (cookie, CORS, WS).
- [roadmap.md](./roadmap.md) — полные пути по ролям и бэклог.
- [testing.md](./testing.md) — как гоняются тесты и Testcontainers.

Документ можно дополнять по мере изменения API; дата составления: **2026-05-19**.
