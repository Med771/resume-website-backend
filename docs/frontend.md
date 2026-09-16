# Руководство для frontend-разработчика

Полный перечень HTTP-эндпоинтов с ролями и публичными путями: [api-endpoints.md](./api-endpoints.md).

Саморегистрация студента (почта, 6-значный код, без Telegram): [frontend-student-email-registration.md](./frontend-student-email-registration.md).

## Базовый URL и CORS

- API должен быть в **`app.security.cors.allowed-origins`** (см. `application.yaml`), иначе браузер заблокирует запросы с **credentials**.
- Для сессии на cookie используйте **`credentials: 'include'`** (fetch) или **`withCredentials: true`** (axios).

## Вход в систему

1. `POST /auth/login` — JSON с учётными данными (точная схема — в Swagger, `LoginRequest`).
2. Ответ **204** + **Set-Cookie** — дальше браузер сам отправляет cookies на тот же origin API.
3. При **401** или истечении access: `POST /auth/refresh`, затем повтор запроса.
4. Выход: `POST /auth/logout`.

Имена cookie задаются в конфиге (часто **`ACCESS_TOKEN`**, **`REFRESH_TOKEN`**). Cookie **HttpOnly** — из JavaScript не читаются.

## Публичная витрина без входа

Без cookie (`permitAll`) — **только главная страница**:

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | `/public/vitrina/home` | Агрегат витрины: `{ students, projects }` с лимитами из `app.vitrina.home` |
| POST | `/public/analytics/events` | Событие аналитики (лимит `app.analytics.rate-limit-per-ip-per-minute`) |

Пути `/public/students/**`, `/public/projects/**`, `/public/vacancies/**` **требуют JWT** (каталоги и вакансии — после регистрации и одобрения аккаунта).

Для **`POST /student/cardsFilter`** и **`POST /student/filter`** порядок выдачи задаётся полями **`sortBy`**, **`sortDirection`**, **`useDefaultRanking`** в JSON (`FilterStudentReq`), а не произвольным `sort=` в query.

Поля **`public_profile_consent`**, **`manual_sort_order`** и **`profile_text_score`** приходят в DTO студента при ответах API; при **создании админом** первые два можно задать в теле `POST /student` / `POST /student/extended` (см. Swagger).

## Роли и экраны

| Роль | Типичные сценарии |
|------|-------------------|
| **GUEST** / **USER** | Каталог студентов, `POST /request`, `GET /recruiter/me`, чаты `/chat/...`. |
| **STUDENT** | `GET /student/me`, каталог `POST /student/cardsFilter`, `GET /student/{id}` (после **APPROVED**), чаты, `POST /request/{id}/student-decision`. **Создавать заявку нельзя.** |
| **ADMIN** | Фильтр/удаление заявок, мягкое удаление сообщений, полная история чатов в REST, **`/admin/projects`**, **`POST /admin/analytics/summary`**. |

Spring ожидает authorities вида **`ROLE_*`**; это согласовано с данными пользователя в БД.

## Сценарий «рекрутер → студент»

1. Логин рекрутера (**USER** / **GUEST** / …).
2. Первая заявка — с полями компании и контактов (**`AddRequestReq`** в Swagger); далее чаще только **`studentId`**, если **`GET /recruiter/me`** уже возвращает профиль.
3. В **`RequestDTO`** есть **`appChatId`** и **`result`** — можно открыть чат по этому UUID.
4. До ответа студента в ленте REST — только **системные** сообщения; ориентир для UI: `messageKind`, `systemEvent` (`REQUEST_SENT`, `STUDENT_ACCEPTED`, `STUDENT_REJECTED`, `ADMIN_JOINED` — см. backend-док).
5. Чтобы вызвать **`POST /request/{id}/student-decision`**, нужен числовой **`id` заявки**. Пока отдельного списка заявок для студента в API может не быть — см. [roadmap.md](./roadmap.md); временно не полагайтесь только на парсинг текста системного сообщения.

## REST чата

Базовый префикс: **`/chat`**.

| Метод | Путь | Назначение |
|-------|------|------------|
| GET | `/chat` | Список чатов (`PageResponse<ChatSummaryDTO>`) |
| GET | `/chat/{chatId}/summary` | Сводка |
| GET | `/chat/{chatId}/messages` | Страница сообщений |
| POST | `/chat/{chatId}/messages` | Текст: `{ "body": "..." }` |
| POST | `/chat/{chatId}/messages/attachment` | `multipart/form-data`: часть **`file`**, опционально **`body`** |
| PATCH | `/chat/{chatId}/messages/{messageId}` | Новое тело |
| POST | `/chat/{chatId}/read` | `{ "messageId": "<uuid>" }` |
| DELETE | `/chat/{chatId}/messages/{messageId}` | Только админ, мягкое удаление |
| GET | `/chat/{chatId}/context` | Только **ADMIN**: связанные заявки и отклики на вакансии |
| DELETE | `/chat/{chatId}` | Только **ADMIN**: полное удаление чата, заявок и откликов по `appChatId` |

Для **ADMIN** в `ChatSummaryDTO` дополнительно: `recruiterName`, `studentName`, `activeRequestId`, `activeRequestResult`, `tuPhase`, `messageCount`.

В **`RequestDTO`** и **`VacancyApplicationDTO`** для списков и деталей доступны поля ТУ: `studentTuConfirmedAt`, `recruiterTuConfirmedAt`, `rejectionReasonCode`, `rejectionComment`, `recruiterDisplayName`, `studentDisplayName`, вычисляемое **`tuPhase`** (`WAITING_STUDENT` | `WAITING_RECRUITER` | `COMPLETED` | `REJECTED` | `NOT_APPLICABLE`). **`page`**, **`size`**. Для каталога студентов сортировка — в теле **`FilterStudentReq`**, не через query `sort`.

## WebSocket (STOMP + SockJS)

- Подключение к **`/ws`** через SockJS (см. документацию Spring WebSocket).
- Подписки:
  - **`/topic/chats/{appChatId}`** — обязательна для рекрутера/студента/админа: системные события и полная переписка **после** принятия заявки.
  - **`/topic/chats/{appChatId}/staff`** — для **админского** UI, если нужен real-time по **пользовательским** сообщениям **до** принятия заявки (они туда не попадают в общий топик намеренно).
  - **`/topic/users/{myUserId}/inbox`** — персональные уведомления, когда пользователь **не** на экране конкретного чата: новая заявка, решение студента, частичное/полное ТУ, отказ по ТУ, новое сообщение в чате. Payload — **`UserInboxNotificationDTO`** (`type`, `chatId`, `requestId?`, `applicationId?`, `preview`, `systemEvent?`, `occurredAt`, `counterpartyName?`). Типы: `NEW_REQUEST`, `REQUEST_DECISION`, `TU_PARTIAL`, `TU_CONFIRMED`, `TU_REJECTED`, `CHAT_MESSAGE`. Подписывайтесь только на **свой** `userId` (из `GET /auth/me`).

Системные события ТУ в чате: `TU_STUDENT_CONFIRMED`, `TU_RECRUITER_CONFIRMED`, `TU_CONFIRMED`, `TU_REJECTED` (наряду с `REQUEST_SENT`, `STUDENT_ACCEPTED` и др.).

**Важно:** в текущей конфигурации handshake **`/ws`** может быть доступен без JWT на уровне Spring Security; в продакшене контракт может усилиться — закладывайте передачу токена при connect, если появится требование.

## Вакансии (рекрутер ↔ студент)

Отдельный домен от **`POST /request`**. Витрина: **`GET /vacancies`** (JWT, роли **STUDENT** / **GUEST** / **USER**).

| Кто | Действия |
|-----|----------|
| Рекрутер | `POST /vacancies` → `submit-for-review` → ждёт админа → `GET /vacancies/mine`, отклики `GET/POST .../applications/...` |
| Админ | `POST /admin/vacancies/filter`, `approve` / `reject` |
| Студент | `GET /vacancies`, `POST /vacancies/{id}/applications`, `GET /vacancies/applications/mine` |

Статусы вакансии: **DRAFT** → **PENDING_REVIEW** → **PUBLISHED** (или **REJECTED** с `moderationRejectionReason`). Чат по отклику — после **`accept`** рекрутёром (`appChatId` в `VacancyApplicationDTO`). Системное событие: **`VACANCY_APPLICATION_ACCEPTED`**.

## Решение студента по заявке

`POST /request/{id}/student-decision`

```json
{
  "accept": true,
  "comment": "опционально"
}
```

или `"accept": false`. Успех — обновить заявку/чат и обработать новые WS-сообщения.

## Swagger

Точные поля DTO, коды ответов и **`@PreAuthorize`** — в **Swagger UI** (`/swagger-ui.html`).

## Частые ошибки

- Запросы без **credentials** — постоянные 401.
- Origin не из CORS — ошибка в консоли браузера без нормального тела ответа.
- Студент вызывает **`POST /request`** — отказ в доступе.
- Ожидание **пользовательских** сообщений у рекрутера/студента **до** accept: в REST их нет; в WS до принятия они уходят в **`/staff`**, не в общий топик.
