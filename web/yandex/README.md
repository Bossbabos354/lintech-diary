# Прокси для веб-версии на Yandex Cloud Functions

Нужен, чтобы сайт на GitHub Pages мог обращаться к журналу: браузер не может ставить
заголовок `Cookie`, а журнал опознаёт сессию только по нему.

## Деплой через консоль

1. [console.yandex.cloud](https://console.yandex.cloud) → **Cloud Functions** → **Создать функцию**.
2. Имя — `lintech-proxy`, среда выполнения — **Node.js 18**.
3. **Создать версию** → способ **ZIP-архив** → загрузить `web/yandex/index.js` и `package.json`
   (или архив с этими двумя файлами).
4. Точка входа — `index.handler`.
5. Таймаут — **60 секунд**, память — **256 МБ**.
6. Создать версию, затем на вкладке функции включить **Публичная функция**
   (иначе браузер получит 403).
7. Скопировать ссылку вызова вида `https://functions.yandexcloud.net/<id>`.

## Подключение к сайту

В `web/index.html` перед `<script src="app.js...">`:

```html
<script>window.LINTECH_PROXY = 'https://functions.yandexcloud.net/<id>';</script>
```

## Проверка

```bash
curl -s -X POST https://functions.yandexcloud.net/<id> \
  -H 'Content-Type: application/json' \
  -d '{"e":"/login","p":{"l":"test","p":"test"}}'
```

Ожидается `{"ok":true,"code":200,"text":"[\n]"}`.
