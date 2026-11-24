<?php
// === Публичная страница статьи (открывается в WebView приложения и по QR) ===
// ?slug=<slug>
// Стиль — как основной сайт ЛинТех 28 (бумага + чертёж). Автономный, без site.css.
// Совместимость: PHP 5.6+ (БЕЗ оператора ??, без коротких массивов)

header('Content-Type: text/html; charset=utf-8');

define('JSON_FILE', __DIR__ . '/events.json');

$slug = isset($_GET['slug']) ? trim((string)$_GET['slug']) : '';
$events = array();
if (file_exists(JSON_FILE)) {
    $raw = @json_decode(file_get_contents(JSON_FILE), true);
    $events = (is_array($raw) && isset($raw['events'])) ? $raw['events'] : array();
}
$article = null;
foreach ($events as $ev) {
    if ((isset($ev['slug']) ? $ev['slug'] : '') === $slug) { $article = $ev; break; }
}

if ($article === null) {
    echo '<!DOCTYPE html><html lang="ru"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Не найдено</title>
      <style>body{font-family:-apple-system,"Segoe UI",Roboto,Arial,sans-serif;background:#f2f1ee;color:#101014;padding:48px 24px;text-align:center}.hud{font-family:ui-monospace,monospace;font-size:11px;letter-spacing:.15em;text-transform:uppercase;color:rgba(16,16,20,.5);margin-bottom:12px}a{color:#2d81ff;text-decoration:none}</style>
      <body><div class="hud">ЛинТех 28 · 404</div><h1>Статья не найдена</h1><p>Возможно, она была удалена. <br><a href="http://d91950hv.beget.tech/">На главную →</a></p></body></html>';
    exit;
}

$title = htmlspecialchars(isset($article['title']) ? $article['title'] : 'Без названия');
$text = htmlspecialchars(isset($article['text']) ? $article['text'] : '');
$date = htmlspecialchars(isset($article['date']) ? $article['date'] : '');
$time = htmlspecialchars(isset($article['time']) ? $article['time'] : '');
$content = isset($article['content']) ? $article['content'] : '';
$image = htmlspecialchars(isset($article['image']) ? $article['image'] : '');
$link = htmlspecialchars(isset($article['link']) ? $article['link'] : '');

// Форматирование даты для отображения
$dateNice = $date;
if ($date) {
    $ts = strtotime($date);
    if ($ts) $dateNice = date('d.m.Y', $ts);
}

echo '<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>' . $title . '</title>
<style>
  :root {
    --bg:#f2f1ee; --bg-raised:#faf9f7; --bg-sunken:#e9e8e4;
    --ink:#101014; --ink-strong:#000;
    --muted:rgba(16,16,20,.62); --faint:rgba(16,16,20,.42);
    --line:rgba(16,16,20,.16); --line-strong:rgba(16,16,20,.4);
    --accent:#2d81ff; --accent-ink:#1659c4;
    --invert-bg:#101014; --invert-fg:#f2f1ee;
  }
  * { box-sizing:border-box; margin:0; padding:0; }
  html { -webkit-text-size-adjust:100%; scrollbar-gutter:stable; }
  body {
    background:var(--bg); color:var(--ink);
    font-family:"Inter",system-ui,-apple-system,"Segoe UI",Roboto,Arial,sans-serif;
    font-size:16px; line-height:1.6; -webkit-font-smoothing:antialiased;
    padding-bottom:60px;
  }
  a { color:inherit; text-decoration:none; }
  img { display:block; max-width:100%; }
  ::selection { background:var(--accent); color:#fff; }

  /* Шапка — как на основном сайте */
  .nav {
    position:sticky; top:0; z-index:50;
    border-bottom:1px solid var(--line);
    background:color-mix(in srgb, var(--bg) 92%, transparent);
    -webkit-backdrop-filter:blur(8px); backdrop-filter:blur(8px);
  }
  .nav-inner { max-width:820px; margin:0 auto; height:52px; display:flex; align-items:stretch; }
  .nav-logo { display:flex; align-items:center; gap:10px; padding:0 16px; }
  .nav-logo b { font-weight:700; font-size:15px; letter-spacing:-.01em; }
  .nav-links { margin-left:auto; display:flex; align-items:stretch; }
  .nav-links a { display:flex; align-items:center; padding:0 14px; font-family:ui-monospace,monospace; font-size:11px; letter-spacing:.15em; text-transform:uppercase; color:var(--muted); transition:background-color .12s linear,color .12s linear; }
  .nav-links a:hover { background:var(--invert-bg); color:var(--invert-fg); }

  /* Контент — рамка страницы как на основном сайте */
  .frame { max-width:820px; margin:0 auto; border-left:1px solid var(--line); border-right:1px solid var(--line); position:relative; }
  .hero { border-bottom:1px solid var(--line); padding:48px 24px 40px; }
  .hud { font-family:ui-monospace,monospace; font-size:11px; font-weight:500; letter-spacing:.15em; text-transform:uppercase; color:var(--faint); display:flex; align-items:center; gap:10px; margin-bottom:18px; }
  .hud .num { color:var(--ink); }
  h1 { font-size:clamp(1.9rem,6vw,2.8rem); line-height:1.08; letter-spacing:-.03em; font-weight:500; max-width:16em; }
  .file { display:inline-flex; align-items:center; gap:8px; margin-top:18px; border:1px solid var(--line); padding:6px 12px; color:var(--muted); font-family:ui-monospace,monospace; font-size:11px; letter-spacing:.12em; text-transform:uppercase; }
  .file::before { content:""; width:6px; height:6px; background:#34d399; }

  .art { padding:0 24px; }
  .art > :first-child { border-top:1px solid var(--line); }
  .img { width:100%; height:auto; border-bottom:1px solid var(--line); }
  .summary { padding:26px 24px; border-bottom:1px solid var(--line); color:var(--muted); font-size:1.125rem; line-height:1.625; }
  .content { padding:28px 24px 40px; }
  .content p { margin-bottom:16px; color:var(--ink); max-width:40em; }
  .content ul, .content ol { margin:0 0 16px 22px; display:flex; flex-direction:column; gap:8px; }
  .content li::marker { color:var(--accent-ink); }
  .content b, .content strong { color:var(--ink-strong); font-weight:600; }
  .content h1, .content h2, .content h3 { font-weight:500; letter-spacing:-.03em; margin:30px 0 12px; line-height:1.2; }
  .content h2 { font-size:1.6rem; }
  .content h3 { font-size:1.2rem; }
  .content a { color:var(--accent-ink); text-decoration:underline; text-underline-offset:3px; }
  .content code { font-family:ui-monospace,monospace; font-size:.85em; background:var(--bg-sunken); padding:1px 5px; border:1px solid var(--line); }

  .foot { border-top:1px solid var(--line); }
  .btn { display:flex; align-items:center; justify-content:center; gap:8px; height:48px; background:var(--invert-bg); color:var(--invert-fg); font-family:ui-monospace,monospace; font-size:11px; font-weight:700; letter-spacing:.12em; text-transform:uppercase; transition:opacity .15s; }
  .btn:hover { opacity:.8; }
  .cred { padding:14px 24px; text-align:center; color:var(--faint); font-size:12px; border-top:1px solid var(--line); }
</style>
</head>
<body>
  <nav class="nav"><div class="nav-inner">
    <div class="nav-logo"><b>ЛинТех 28</b></div>
    <div class="nav-links"><a href="http://d91950hv.beget.tech/">На Сайт</a></div>
  </div></nav>

  <div class="frame">
    <div class="hero">
      <div class="hud"><span class="num">01</span> Событие школы</div>
      <h1>' . $title . '</h1>';
if ($date) {
    $when = $dateNice;
    if ($time !== '') $when .= ' · ' . $time;
    echo '<div class="file">📅 ' . $when . '</div>';
}
echo '    </div>

    <article class="art">';
if ($image) echo '<img class="img" src="' . $image . '" alt="' . htmlspecialchars($title) . '">';
else echo '<div style="border-top:1px solid var(--line)"></div>';
if ($text) echo '<div class="summary">' . $text . '</div>';
echo '<div class="content">';
if ($content) echo $content;
elseif ($text) echo $text;
echo '</div></article>

    <footer class="foot">
      <a class="btn" href="http://d91950hv.beget.tech/">← На Сайт</a>
      <div class="cred">ЛинТех 28 — Электронный дневник</div>
    </footer>
  </div>
</body>
</html>';