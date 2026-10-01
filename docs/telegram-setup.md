# Telegram-бот для телефона рекрутера

Телефон подтверждает работодатель перед `POST /auth/register-recruiter`. Студент бота не использует: ему на почту уходит код из 6 цифр.

## Бот

1. В Telegram откройте [@BotFather](https://t.me/BotFather), команда `/newbot`.
2. Сохраните token и username бота без `@`.

Переменные, которые читает `application.yaml`:

| Переменная | Куда |
|------------|------|
| `APP_TELEGRAM_ENABLED` | `app.telegram.enabled` |
| `TELEGRAM_BOT_TOKEN` | `app.telegram.bot-token` |
| `TELEGRAM_BOT_USERNAME` | `app.telegram.bot-username` |
| `TELEGRAM_WEBHOOK_SECRET` | `app.telegram.webhook-secret` |

Пока `enabled` выключен и выключен dev-код, `POST /verification/phone/start` отвечает ошибкой: бот не настроен.

## Webhook

Telegram шлёт обновления на backend:

`POST https://<хост-api>/telegram/webhook`

Заголовок: `X-Telegram-Bot-Api-Secret-Token: <TELEGRAM_WEBHOOK_SECRET>`.

Без верного секрета ответ **403**. Webhook должен попадать в это приложение, а не в статику сайта.

Поставить webhook (с машины, где открыт `api.telegram.org`):

```bash
curl "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -d "url=https://<хост-api>/telegram/webhook" \
  -d "secret_token=<TELEGRAM_WEBHOOK_SECRET>"
```

Проверка: `curl "https://api.telegram.org/bot<TOKEN>/getWebhookInfo"`.

Локально Telegram нужен публичный HTTPS. Туннель (ngrok, cloudflared) на порт 8080 или уже развёрнутый API.

## Как это выглядит для рекрутера

1. Сайт вызывает `POST /verification/phone/start` и получает ссылку `https://t.me/{bot}?start={verificationId}`.
2. Человек открывает бота и делится номером.
3. Бот сверяет номер, статус становится `CONFIRMED`.
4. Сайт опрашивает `GET /verification/phone/{id}/status`.
5. Дальше `POST /auth/register-recruiter`. Номер в заявке должен совпасть с подтверждённым.

Сессия живёт `app.telegram.verification-ttl-minutes` (по умолчанию 15 минут).

## Локально без бота

В `application.yaml` для разработки уже стоит:

```yaml
app.telegram:
  allow-dev-confirm: true
  dev-confirm-code: "7890"
```

`POST /verification/phone/start` создаёт сессию. `POST /verification/phone/{id}/confirm-code` с `{ "code": "7890" }` ставит `CONFIRMED`. На проде `allow-dev-confirm` выключают.

Код на экране — 4 цифры. У студента код почты — 6 цифр, это другой поток.
