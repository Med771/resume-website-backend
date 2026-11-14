# Сценарии по ролям

Роли: студент, рекрутер, админ. Подробные URL — в [api-endpoints.md](./api-endpoints.md).

## Гость

Шлёт просмотры в аналитику. Может зарегистрироваться студентом или рекрутером. Каталог резюме без входа закрыт.

## Студент

1. `POST /auth/register-student` — аккаунт, cookie, черновик карточки (`catalogVisible = false`, статус `PENDING_APPROVAL`).
2. Вводит код с почты: `POST /auth/confirm-email`.
3. Дозаполняет резюме: `PATCH /student/me`, опыт, учёба, портфолио, фото.
4. Админ одобряет аккаунт. Пока почта не подтверждена, одобрение вернёт 400.
5. Смотрит каталог и проекты.
6. На заявку рекрутера отвечает `POST /request/{id}/student-decision`.
7. После принятия заявки пишет в чат. До этого видит только системные сообщения.
8. Подтверждает или отклоняет ТУ: `POST /request/{id}/tu-decision`.

Свои заявки: `POST /request/mine/filter`.

## Рекрутер

1. `POST /auth/register-recruiter`. Сервер сразу создаёт профиль и пользователя и выдаёт cookie. Обязательны почта, фамилия, имя и компания. Код с почты подтверждают `POST /auth/confirm-email`.
2. В ЛК статус `PENDING_APPROVAL`, пока админ не одобрит аккаунт (`POST /admin/account-approvals/{userId}/approve`).
3. `GET /recruiter/me`, каталог `POST /student/cardsFilter`, карточка `GET /student/{id}`.
4. Пишет студенту: `POST /request`. Чат создаётся сразу.
5. После согласия студента переписывается в `/chat`.

## Админ

Вход: `POST /auth/admin/login`.

- Список студентов: `/admin/account-approvals/students`. В строке есть `hasPortfolio`, `hasExperience`, `hasEducation`. Одобрение студента ставит `catalog_visible = true`.
- Старые заявки из `recruiter_registration_requests`: `/admin/recruiter-registration-requests`. Новая саморегистрация эту таблицу не заполняет.
- Карточки студентов, видимость, ручной порядок.
- Справочники: компании, навыки, специальности, учебные заведения.
- Список вакансий всех статусов: `GET /admin/vacancies`. Одобрение и отказ — `POST /admin/vacancies/{id}/approve` и `reject`, только из `PENDING_REVIEW`.
- Все заявки и чаты, полная история, удаление сообщений и чата.
- Проекты сайта: создание, порядок, участники.
- Сводки аналитики: `/admin/analytics`.
- Файлы: `/admin/storage/files`.

## Как открывается переписка

Чат один на пару рекрутер–студент. Писать друг другу можно, если есть заявка со статусом `student_confirm`, `recruiter_conf` или `success`, либо отклик на вакансию в статусе `accepted`. Админ читает и пишет раньше.
