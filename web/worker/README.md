# Прокси на Cloudflare Workers

Замена `web/proxy.php` для размещения сайта на GitHub Pages: Pages не выполняет PHP,
а Worker делает то же самое — принимает `{ e, p, cookie, get }` и ходит в журнал.

## Деплой

```bash
cd web/worker
npx wrangler login
npx wrangler deploy
```

После деплоя Wrangler покажет адрес вида
`https://lintech-proxy.<аккаунт>.workers.dev`.

## Подключение к сайту

В `web/index.html` перед `<script src="app.js...">` добавьте:

```html
<script>window.LINTECH_PROXY = 'https://lintech-proxy.<аккаунт>.workers.dev';</script>
```

Без этой строки используется `proxy.php` — как на Beget.

## Проверка

```bash
curl -s -X POST https://lintech-proxy.<аккаунт>.workers.dev \
  -H 'Content-Type: application/json' \
  -d '{"e":"/","cookie":"","get":true}' | head -c 200
```

Ожидается JSON вида `{"ok":true,"code":200,"text":"..."}`.
