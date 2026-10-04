# Как устроен backend

Краткий паспорт: [project-passport.md](./project-passport.md). Список URL: [api-endpoints.md](./api-endpoints.md). Таблицы: [database.md](./database.md).

## Слои

```
ru.ai.sin
├── config/          Security, WebSocket, свойства
├── filter/          JwtCookieAuthenticationFilter
├── helper/          JWT, файлы, текущий пользователь
├── tools/           загрузка сущностей и сборка DTO
├── models/          страницы, embeddable, enum
├── exception/       общий обработчик ошибок
└── logic/           домен: контроллер → сервис → репозиторий
```

Фильтры списков — классы `*Specifications`. Маппинг — MapStruct (`*Mapper`) и `*Tools`. `open-in-view: false`: ленивые связи читают внутри сервиса, обычно в `@Transactional(readOnly = true)`.

## Безопасность

Сессия stateless, CSRF выключен. Фильтр читает JWT из cookie и кладёт пользователя в `SecurityContext`.

Без cookie открыты:

- `/auth/**`, кроме `POST /auth/confirm-email` и `POST /auth/resend-email-confirmation` (нужен вошедший студент)
- `/verification/phone/**`, `/telegram/webhook`
- `/public/vitrina/**`, `/public/analytics/**`
- `/main/**`, `/ws/**`, Swagger, `/error`, `OPTIONS /**`

`/admin/**` — только **ADMIN**. Всё остальное требует вход, точные роли стоят на методах через `@PreAuthorize`.

`/public/students/**` и `/public/vacancies/**` вход требуют. Анонимная главная — `GET /public/vitrina/home`.

Роли: **STUDENT**, **RECRUITER**, **ADMIN**. В токене authority вида `ROLE_STUDENT`. Админ входит через `/auth/admin/login`, студент и рекрутер — через `/auth/login`.

Статус аккаунта (`users.account_status`): `PENDING_APPROVAL`, `APPROVED`, `REJECTED`.

## Домены

**Студент.** Карточка: ФИО, контакты, курс (1–5), занятость, специальность, навыки, опыт, учёба, портфолио. `catalog_visible = false` скрывает карточку от всех, кроме админа (в том числе `GET /student/{id}` отвечает 404). `public_profile_consent` нужен, чтобы карточка попала в публичные выборки. Порядок в каталоге задаёт тело фильтра (`manualSortOrder`, затем аватар, `profile_text_score`, дата), query-параметр `sort` сервис студентов игнорирует.

**Заявка.** Рекрутер (или админ) пишет студенту: `POST /request`. На пару рекрутер–студент один чат. Студент принимает или отклоняет. Свои заявки: `POST /request/mine/filter`.

**Чат.** Пока нет принятой заявки (`student_confirm`, `success`, `recruiter_conf`) и нет принятого отклика на вакансию, студент и рекрутер в REST видят только системные сообщения. Админ видит всё. После разрешения — обычная переписка. Первое сообщение админа в чате добавляет системное `ADMIN_JOINED`.

**Вакансия.** Рекрутер создаёт черновик, отправляет на модерацию, админ публикует или отклоняет. Студент откликается. Принятый отклик тоже открывает переписку в общем чате пары.

**Регистрация.** Студент: `POST /auth/register-student`, сразу cookie и черновик карточки (`catalog_visible = false`), код из 6 цифр на почту. Рекрутер: подтверждает телефон, `POST /auth/register-recruiter` сразу создаёт профиль и пользователя со статусом `PENDING_APPROVAL` и выдаёт cookie. Оба аккаунта одобряет админ в `/admin/account-approvals`. Одобрение студента включает показ карточки в каталоге. Студента без подтверждённой почты одобрить нельзя.

Таблица `recruiter_registration_requests` и `/admin/recruiter-registration-requests` в коде есть. Текущая саморегистрация новые строки туда не пишет.

**Проекты сайта.** `/projects`: читать могут все три роли, создавать и менять — админ. На анонимной главной те же записи с `visible_to_anonymous` и окном публикации, без списка студентов.

**Аналитика.** `POST /public/analytics/events` без входа, лимит по IP. Сводки — `/admin/analytics/**`.

**Файлы.** Аватары и вложения лежат на диске (`app.file.path`). Картинки отдаёт `GET /main/photo/{image_path}`. Админ управляет файлами через `/admin/storage/files`.

**Уведомления.** Отдельной таблицы нет. Событие уходит в WebSocket `/topic/users/{userId}/inbox`.

## WebSocket

| | |
|--|--|
| Подключение | `/ws` (SockJS) |
| Брокер | `/topic` |
| Префикс приложения | `/app` |
| Чат | `/topic/chats/{chatId}` |
| Сообщения участников до открытия переписки | `/topic/chats/{chatId}/staff` |
| Входящие | `/topic/users/{userId}/inbox` |

Handshake `/ws` на HTTP открыт всем. Токен на connect сервер не проверяет.

Системные коды в `chat_messages.system_event`: `REQUEST_SENT`, `STUDENT_ACCEPTED`, `STUDENT_REJECTED`, `ADMIN_JOINED`, `VACANCY_APPLICATION_ACCEPTED`, `TU_STUDENT_CONFIRMED`, `TU_RECRUITER_CONFIRMED`, `TU_CONFIRMED`, `TU_REJECTED`.

## Ошибки

`GlobalExceptionHandler` отдаёт единое тело ошибки:

| Код | Когда |
|-----|--------|
| 400 | валидация, нечитаемое тело, своё `BadRequestException`, слишком большой файл |
| 401 | нет или плохая аутентификация |
| 403 | роль не подходит |
| 404 | сущность не найдена |
| 409 | конфликт данных, где это обрабатывается |
| 429 | лимит регистрации, почты или аналитики |
| 500 | всё остальное |

## Конфиг

`src/main/resources/application.yaml`: база, Flyway, CORS, JWT, лимиты регистрации и аналитики, Telegram, демо-логин админа (`app.user.logins`), путь к файлам.
