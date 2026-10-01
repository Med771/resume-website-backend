# HTTP API

Роли: **S** студент, **R** рекрутер, **A** админ. «Вход» — любая из трёх ролей с cookie. «Открыто» — cookie не нужна.

Тела запросов — в Swagger (`/swagger-ui.html`). Списки с фильтром почти везде: `POST .../filter` и query `page`, `size`.

## Открыто

| Метод | Путь | Зачем |
|-------|------|--------|
| POST | `/auth/register-student` | регистрация студента, сразу cookie |
| POST | `/auth/register-recruiter` | регистрация рекрутера, сразу cookie |
| POST | `/auth/login` | вход студента или рекрутера |
| POST | `/auth/refresh` | новый access по refresh-cookie |
| POST | `/auth/logout` | сбросить cookie |
| GET | `/auth/me` | текущая сессия; без cookie — 401 |
| POST | `/auth/admin/login` | вход админа |
| POST | `/auth/admin/refresh` | обновить access админа |
| POST | `/auth/admin/logout` | выход админа |
| POST | `/verification/phone/start` | начать проверку телефона |
| GET | `/verification/phone/{id}/status` | статус проверки |
| POST | `/verification/phone/{id}/confirm-code` | ввести код |
| GET, POST | `/telegram/webhook` | апдейты бота |
| GET | `/public/vitrina/home` | анонимная главная: студенты и проекты |
| POST | `/public/analytics/events` | событие аналитики |
| GET | `/main/status` | 204, сервис жив |
| GET | `/main/photo/{image_path}` | картинка с диска |
| GET | `/swagger-ui.html`, `/v3/api-docs` | документация |

`POST /auth/confirm-email` и `POST /auth/resend-email-confirmation` — только **S** (нужна cookie после регистрации).

`POST /auth/change-password` — любой вошедший. `GET /auth/admin/me` и `POST /auth/admin/change-password` — **A**.

## Студенты

| Метод | Путь | Кто |
|-------|------|-----|
| GET | `/student/me` | S |
| PATCH | `/student/me` | S |
| GET | `/student/{id}` | S, R, A |
| POST | `/student/cardsFilter` | S, R, A |
| POST | `/student/filter` | S, R, A |
| POST | `/student/photo/{id}` | S, A, multipart |
| POST | `/student` | A |
| POST | `/student/extended` | A, карточка сразу с опытом и учёбой |
| PUT | `/student/{id}` | A |
| PATCH | `/student/{id}` | A |
| DELETE | `/student/{id}` | A |
| POST | `/admin/students/reorder` | A |
| POST | `/admin/students/bulk-visibility` | A |
| GET | `/public/students/{id}` | вход |
| POST | `/public/students/cards` | вход |

Карточка с `catalog_visible = false` для студента и рекрутера — 404.

## Заявки и чат

| Метод | Путь | Кто |
|-------|------|-----|
| POST | `/request` | R, A |
| POST | `/request/mine/filter` | S, R |
| GET | `/request/{id}` | A |
| POST | `/request/filter` | A |
| POST | `/request/{id}/student-decision` | S |
| POST | `/request/{id}/tu-decision` | S, R |
| DELETE | `/request/{id}` | A |
| GET | `/chat` | вход |
| GET | `/chat/{chatId}/summary` | вход |
| GET | `/chat/{chatId}/messages` | вход |
| POST | `/chat/{chatId}/messages` | вход |
| POST | `/chat/{chatId}/messages/attachment` | вход, multipart |
| PATCH | `/chat/{chatId}/messages/{messageId}` | вход, своё сообщение |
| POST | `/chat/{chatId}/read` | вход |
| GET | `/chat/{chatId}/context` | A |
| DELETE | `/chat/{chatId}` | A |
| DELETE | `/chat/{chatId}/messages/{messageId}` | A |
| GET | `/profile/communication-readiness` | вход |

Пока переписка не открыта, студент и рекрутер получают только системные сообщения. Правила — в [backend.md](./backend.md).

## Вакансии

| Метод | Путь | Кто |
|-------|------|-----|
| GET | `/vacancies` | S, R, A |
| GET | `/vacancies/mine` | R |
| GET | `/vacancies/{id}` | S, R, A |
| POST | `/vacancies` | R |
| PUT | `/vacancies/{id}` | R |
| POST | `/vacancies/{id}/submit-for-review` | R |
| POST | `/vacancies/{id}/close` | R |
| DELETE | `/vacancies/{id}` | R, A |
| POST | `/vacancies/{id}/applications` | S |
| GET | `/vacancies/applications/mine` | S |
| POST | `/vacancies/applications/{applicationId}/withdraw` | S |
| GET | `/vacancies/{id}/applications` | R |
| POST | `/vacancies/{id}/applications/{applicationId}/accept` | R |
| POST | `/vacancies/{id}/applications/{applicationId}/reject` | R |
| POST | `/vacancies/applications/{applicationId}/tu-decision` | S, R |
| POST | `/admin/vacancies/filter` | A |
| GET | `/admin/vacancies/{id}` | A |
| POST | `/admin/vacancies/{id}/approve` | A |
| POST | `/admin/vacancies/{id}/reject` | A |
| POST | `/admin/vacancies/reorder` | A |
| PATCH | `/admin/vacancies/{id}/vitrina` | A |
| GET | `/public/vacancies` | вход |
| GET | `/public/vacancies/{id}` | вход |
| POST | `/recruiter/onboarding/vacancy` | R |
| GET | `/recruiter/onboarding/status` | R |

## Рекрутер, пользователи, модерация

| Метод | Путь | Кто |
|-------|------|-----|
| GET | `/recruiter/me` | R, A |
| GET | `/recruiter/{id}` | R, A |
| PATCH | `/recruiter/{id}` | R, A |
| POST | `/recruiter` | A |
| POST | `/recruiter/filter` | A |
| PUT | `/recruiter/{id}` | A |
| DELETE | `/recruiter/{id}` | A |
| POST | `/user/filter` | A |
| POST | `/user` | A |
| DELETE | `/user/{id}` | A |
| GET | `/admin/account-approvals` | A |
| POST | `/admin/account-approvals/{userId}/approve` | A |
| POST | `/admin/account-approvals/{userId}/reject` | A |
| POST | `/admin/recruiter-registration-requests/filter` | A |
| POST | `/admin/recruiter-registration-requests/{id}/approve` | A |
| POST | `/admin/recruiter-registration-requests/{id}/reject` | A |

Живая регистрация рекрутера идёт через `POST /auth/register-recruiter` и очередь `/admin/account-approvals`. Методы `/admin/recruiter-registration-requests` работают со строками таблицы `recruiter_registration_requests`; текущая форма новые строки туда не добавляет.

Студента без подтверждённой почты админ одобрить не может (400).

## Проекты

| Метод | Путь | Кто |
|-------|------|-----|
| POST | `/projects/filter` | S, R, A |
| GET | `/projects/{id}` | S, R, A |
| POST | `/projects` | A |
| PUT | `/projects/{id}` | A |
| DELETE | `/projects/{id}` | A |
| POST | `/projects/reorder` | A |
| GET | `/projects/{id}/students` | A |
| POST | `/projects/{id}/students` | A |
| DELETE | `/projects/{id}/students` | A |

Аноним видит проекты внутри `GET /public/vitrina/home`.

## Справочники

Один и тот же набор методов.

Чтение (`GET /{id}`, `POST /filter`) — **S, R, A**. Создание, правка, удаление — **A**, кроме опыта, портфолио и учёбы студента: их меняет ещё и **S** (свою карточку).

| Ресурс | Создание / правка / удаление |
|--------|------------------------------|
| `/company` | A |
| `/skill` | A |
| `/speciality` | A |
| `/education` | A |
| `/experience` | S, A |
| `/portfolio` | S, A |
| `/institution` | S, A |

Фильтр `/institution/filter` с полем `educationId` доступен только админу.

## Аналитика и файлы админа

| Метод | Путь | Кто |
|-------|------|-----|
| POST | `/admin/analytics/summary` | A |
| POST | `/admin/analytics/entity-population` | A |
| POST | `/admin/analytics/funnel` | A |
| GET | `/admin/storage/files` | A |
| POST | `/admin/storage/files` | A, multipart |
| DELETE | `/admin/storage/files/{fileName}` | A |

## WebSocket

Подключение: `/ws`. Топики:

- `/topic/chats/{chatId}`
- `/topic/chats/{chatId}/staff`
- `/topic/users/{userId}/inbox`
