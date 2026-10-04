# Локальный запуск

Нужны Java 21 и PostgreSQL. Приложение читает базу из `src/main/resources/application.yaml`:

`jdbc:postgresql://localhost:5432/resume`, пользователь `Resume`.

Пароль лежит в том же файле. При старте Flyway сам накатывает миграции.

```powershell
.\mvnw.cmd spring-boot:run
```

Сервер: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

Демо-админ создаётся из `app.user.logins` (сейчас логин `admin`). На проде этот блок нужно сменить или убрать.

## Демо-данные

Скрипты в `scripts/dev/` не входят в Flyway. Их запускают вручную в ту же базу, что в `application.yaml`.

- `demo_seed.sql` — пара справочников, если таблица ещё пустая.
- `site_project_demo_seed.sql` — 10 проектов. Если в `site_projects` уже есть строки, скрипт ничего не пишет.

## Тесты

```powershell
.\mvnw.cmd test
```

Интеграционные тесты поднимают PostgreSQL 16 в Docker. Без Docker они помечаются как пропущенные, сборка остаётся зелёной. Подробнее: [testing.md](./testing.md).

Телефон рекрутера локально можно подтвердить кодом из `app.telegram.dev-confirm-code`, пока `allow-dev-confirm: true`. Как включить бота: [telegram-setup.md](./telegram-setup.md).
