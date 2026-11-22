<?php
// === Админ-API статей (событий) для сайта и приложения ===
// Хранилище: events.json в этой папке
// Действия (POST, JSON): action=list|save|delete|upload_image
// Защита: пароль, SHA-256 (тот же, что в upload.php)
// Совместимость: PHP 5.6+ (БЕЗ оператора ??, БЕЗ mbstring)

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

define('JSON_FILE', __DIR__ . '/events.json');
define('IMG_DIR', __DIR__ . '/articles');

// SHA-256 от пароля на загрузку (тот же, что в upload.php)
define('PASSWORD_HASH', '03ae3dcea8922ef48bb95481925af5c21f2ae2529758d1a89739ad465cbd06bd');

// Любая ошибка должна уехать как JSON, а не как HTML-фатал
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// Если откроют прямо в браузере (GET) — покажем версию PHP и статус
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(array('ok' => false, 'message' => 'Только POST. PHP: ' . phpversion()), 405);
}

function respond($data, $code = 200) {
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}
function fail($msg) { respond(array('ok' => false, 'message' => $msg), 400); }

function checkAuth($password) {
    if (!hash_equals(PASSWORD_HASH, hash('sha256', $password))) {
        respond(array('ok' => false, 'message' => 'Неверный пароль'), 403);
    }
}

function loadEvents() {
    if (!file_exists(JSON_FILE)) return array();
    $raw = @json_decode(file_get_contents(JSON_FILE), true);
    return (is_array($raw) && isset($raw['events'])) ? $raw['events'] : array();
}
function saveEvents($events) {
    usort($events, function($a, $b) {
        $da = isset($b['date']) ? $b['date'] : '';
        $db = isset($a['date']) ? $a['date'] : '';
        return strcmp($da, $db);
    });
    $data = array('events' => array_values($events));
    $json = json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    if (file_put_contents(JSON_FILE, $json) === false) fail('Не удалось записать events.json');
}

// Транслит без mbstring (не работает с многобайтными кодами букв, но для ясности слага ок)
function slugify($text) {
    $text = strtolower(trim($text));
    $map = array(
        'а'=>'a','б'=>'b','в'=>'v','г'=>'g','д'=>'d','е'=>'e','ё'=>'e','ж'=>'j',
        'з'=>'z','и'=>'i','й'=>'i','к'=>'k','л'=>'l','м'=>'m','н'=>'n','о'=>'o',
        'п'=>'p','р'=>'r','с'=>'s','т'=>'t','у'=>'u','ф'=>'f','х'=>'h','ц'=>'c',
        'ч'=>'ch','ш'=>'sh','щ'=>'sch','ъ'=>'y','ы'=>'y','ь'=>'','э'=>'e','ю'=>'yu','я'=>'ya'
    );
    $out = '';
    $len = strlen($text);
    for ($i = 0; $i < $len; $i++) {
        $ch = $text[$i];
        if (isset($map[$ch])) $out .= $map[$ch];
        elseif (preg_match('/[a-z0-9]/i', $ch)) $out .= strtolower($ch);
        else $out .= '-';
    }
    $out = preg_replace('/-+/', '-', $out);
    $out = trim($out, '-');
    if ($out === '') $out = 'article-' . time();
    return substr($out, 0, 60);
}

$rawBody = file_get_contents('php://input');
$body = json_decode($rawBody, true);
if (!is_array($body)) $body = array();
$action = isset($body['action']) ? $body['action'] : '';

switch ($action) {

    case 'list':
        respond(array('ok' => true, 'events' => loadEvents()));

    case 'save':
        $password = isset($body['password']) ? (string)$body['password'] : '';
        checkAuth($password);

        $title = trim((string)(isset($body['title']) ? $body['title'] : ''));
        if ($title === '') fail('Заголовок обязателен');

        $events = loadEvents();
        $id = '';
        if (isset($body['id']) && $body['id'] !== '' && $body['id'] !== null) $id = (string)$body['id'];
        if ($id === '') $id = (string)uniqid('e', true);

        $date = trim((string)(isset($body['date']) ? $body['date'] : date('Y-m-d')));
        $time = trim((string)(isset($body['time']) ? $body['time'] : ''));
        $text = trim((string)(isset($body['text']) ? $body['text'] : ''));
        $content = trim((string)(isset($body['content']) ? $body['content'] : ''));
        $image = trim((string)(isset($body['image']) ? $body['image'] : ''));

        $slug = trim((string)(isset($body['slug']) ? $body['slug'] : ''));
        if ($slug === '') $slug = slugify($title);

        $link = 'http://d91950hv.beget.tech/article.php?slug=' . urlencode($slug);

        $found = false;
        foreach ($events as $i => $ev) {
            if ((isset($ev['id']) ? $ev['id'] : '') === $id) {
                $events[$i]['date'] = $date;
                $events[$i]['time'] = $time;
                $events[$i]['title'] = $title;
                $events[$i]['text'] = $text;
                $events[$i]['content'] = $content;
                $events[$i]['image'] = $image;
                $events[$i]['slug'] = $slug;
                $events[$i]['link'] = $link;
                $found = true;
                break;
            }
        }
        if (!$found) {
            $events[] = array(
                'id' => $id, 'date' => $date, 'time' => $time, 'title' => $title,
                'text' => $text, 'content' => $content, 'image' => $image,
                'slug' => $slug, 'link' => $link
            );
        }
        saveEvents($events);
        respond(array('ok' => true, 'message' => 'Сохранено', 'id' => $id, 'slug' => $slug, 'link' => $link));

    case 'delete':
        $password = isset($body['password']) ? (string)$body['password'] : '';
        checkAuth($password);
        $id = (string)(isset($body['id']) ? $body['id'] : '');
        if ($id === '') fail('Нет id');
        $events = loadEvents();
        $events = array_values(array_filter($events, function($ev) use ($id) {
            return (isset($ev['id']) ? $ev['id'] : '') !== $id;
        }));
        saveEvents($events);
        respond(array('ok' => true, 'message' => 'Удалено'));

    case 'upload_image':
        $password = isset($body['password']) ? (string)$body['password'] : '';
        checkAuth($password);
        $data = (string)(isset($body['file_data']) ? $body['file_data'] : '');
        $name = (string)(isset($body['name']) ? $body['name'] : '');
        if ($data === '') fail('Нет данных');
        $ext = strtolower(pathinfo($name, PATHINFO_EXTENSION));
        if (!in_array($ext, array('jpg','jpeg','png','gif','webp'), true)) fail('Недопустимый формат');
        $bytes = base64_decode($data, false);
        if ($bytes === false || strlen($bytes) < 10) fail('Не удалось декодировать изображение');
        if (strlen($bytes) > 6 * 1024 * 1024) fail('Картинка слишком большая (макс 6МБ)');
        if (!is_dir(IMG_DIR)) @mkdir(IMG_DIR, 0755, true);
        $fname = uniqid('img_', true) . '.' . $ext;
        if (file_put_contents(IMG_DIR . '/' . $fname, $bytes) === false) fail('Не удалось сохранить картинку');
        $url = 'http://d91950hv.beget.tech/articles/' . $fname;
        respond(array('ok' => true, 'url' => $url));

    default:
        fail('Неизвестное действие');
}