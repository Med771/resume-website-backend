# Регистрация студента по почте

Студент регистрируется без Telegram. Сразу после формы он уже залогинен, затем подтверждает почту кодом из 6 цифр. Рекрутер по-прежнему подтверждает телефон, см. [telegram-setup.md](./telegram-setup.md).

Cookie и CORS: [frontend.md](./frontend.md).

## Шаги

1. Форма → `POST /auth/register-student` с `credentials: 'include'`.
2. Ответ **204**, в ответе cookie `ACCESS_TOKEN` и `REFRESH_TOKEN`.
3. На email уходит 6 цифр.
4. `POST /auth/confirm-email` с телом `{ "code": "123456" }`.
5. Если письма нет — `POST /auth/resend-email-confirmation`.
6. Анкету заполняют после: `PATCH /student/me`, `/experience`, `/institution`, `/portfolio`.
7. Админ одобряет аккаунт. Пока `emailVerified` ложь, approve студента вернёт **400**.

Если письмо не ушло, аккаунт всё равно создан (кроме случая, когда отправка обязательна и `app.mail.allow-dev-confirm` выключен). Покажите кнопку «отправить код снова».

## `POST /auth/register-student`

| Поле | Обязательно |
|------|-------------|
| `username` | да, 3–64 символа: латиница, цифры, `_` |
| `password`, `passwordConfirm` | да, должны совпасть. Длина и «буква + цифра» — `app.registration` |
| `email` | да, сюда уйдёт код |
| `phoneNumber` | да, контакт без бота: `+` и 7–15 цифр |
| `firstName`, `lastName`, `middleName`, `city` | нет. `lastName` — фамилия, `middleName` — отчество |

**400** — пароли, занятый логин, занятая почта, слабый пароль. **429** — слишком много регистраций с одного IP.

## Код

Оба вызова только для роли `STUDENT` и только с cookie. Без сессии — **401**.

`POST /auth/confirm-email`: ровно 6 цифр, ответ **204**. Повторный верный вызов, если почта уже подтверждена, тоже **204**.

`POST /auth/resend-email-confirmation`: тела нет, **204**. Если почта уже подтверждена — **400**.

Код живёт около 15 минут. Неверный или просроченный — **400**. Слишком частые попытки и повторные письма — **429**.

## `GET /auth/me`

Поля: `id`, `username`, `role`, `accountStatus`, `emailVerified`, `hintsDisabled`.

После регистрации: cookie есть, `emailVerified: false`, `accountStatus: PENDING_APPROVAL`. После кода почта подтверждена, статус остаётся `PENDING_APPROVAL`, пока не одобрит админ.

Пока `role === "STUDENT"` и `emailVerified === false`, покажите экран ввода кода.

## Анкета

`PATCH /student/me` принимает `middleName` и `gender` (`MALE`, `FEMALE` или `null`). `null` в PATCH значит «не менять». Курс: `FIRST` … `FIFTH` (1–5).

Фото: `POST /student/photo/{id}`. Отдельного `/student/me/photo` нет.

Очередь админа: `GET /admin/account-approvals`. В карточке пользователя есть `emailVerified`.
