# Для фронтенда

Сервер отдаёт JSON. Вход — две HttpOnly-cookie: `ACCESS_TOKEN` и `REFRESH_TOKEN`. Запросы с другого origin идут с `credentials: 'include'`. Имена cookie и список origin лежат в `app.security` и `app.jwt` в `application.yaml`.

Админ логинится на `/auth/admin/login`. Студент и рекрутер — на `/auth/login`.

## Что можно без cookie

- `GET /public/vitrina/home` — главная
- `POST /public/analytics/events`
- `GET /main/status`, `GET /main/photo/{image_path}`
- регистрация, логин, refresh, logout
- проверка телефона `/verification/phone/**`

`/public/students/**` и `/public/vacancies/**` без cookie отвечают **401**. Каталог после входа: `POST /student/cardsFilter` и `GET /vacancies`.

## Сессия

`GET /auth/me` возвращает `id`, `username`, `role`, `accountStatus`, `emailVerified`, `hintsDisabled`.

Роль: `STUDENT`, `RECRUITER`, `ADMIN`. Статус аккаунта: `PENDING_APPROVAL`, `APPROVED`, `REJECTED`.

Access кончается — `POST /auth/refresh` (нужна refresh-cookie). Выход — `POST /auth/logout`.

Регистрация студента: [frontend-student-email-registration.md](./frontend-student-email-registration.md).

## Списки

Пагинация: `page` и `size` в query. У студентов порядок задаёт тело `FilterStudentReq` (`sortBy`, `sortDirection`, `useDefaultRanking`), параметр `sort` сервер не использует.

Карточка с `catalogVisible: false` для студента и рекрутера не находится.

## Чат

Список: `GET /chat`. История: `GET /chat/{chatId}/messages`. Текст: `POST .../messages`. Файл: `POST .../messages/attachment` (multipart). Прочитано: `POST .../read`.

Пока заявка не принята и нет принятого отклика на вакансию, в истории только системные сообщения. Обычный текст в этот момент уходит в топик для админа.

SockJS: `/ws`. Подписки:

| Топик | Что приходит |
|-------|----------------|
| `/topic/chats/{chatId}` | системные события; после открытия переписки — и сообщения людей |
| `/topic/chats/{chatId}/staff` | сообщения людей до открытия переписки |
| `/topic/users/{userId}/inbox` | короткое уведомление (новая заявка, решение, ТУ, сообщение) |

Handshake не требует cookie. Подписывайтесь на чаты, которые уже отдал `GET /chat`.

## Картинки

Аватар: `POST /student/photo/{id}` (студент или админ). Показ: `GET /main/photo/{image_path}`.
