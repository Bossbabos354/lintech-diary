<?php
/**
 * Прокси для веб-версии «Электронного дневника».
 * Браузер не может вручную ставить Cookie-заголовок (Ext JS сессия АВЕРС),
 * поэтому JS передаёт endpoint/параметры сюда, а этот скрипт обращается
 * к journal.school28-kirov.ru от лица сервера, подставляя Cookie.
 *
 * Принимает POST body: { "е": "<endpoint>", "p": { "k": "v" }, "cookie": "ys-user=...; ...", "get": false }
 * Возвращает: { "ok": true, "text": "<исходный ответ журнала>" } либо { "ok": false, "error": "..." }
 */

$BASE = 'https://journal.school28-kirov.ru';

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: ' . (isset($_SERVER['HTTP_ORIGIN']) ? $_SERVER['HTTP_ORIGIN'] : '*'));
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Access-Control-Max-Age: 3600');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'Только POST'], JSON_UNESCAPED_UNICODE);
    exit;
}

$raw = file_get_contents('php://input');
$in = json_decode($raw, true);
if (!is_array($in)) {
    $in = $_POST;
}

$endpoint = isset($in['e']) ? trim((string)$in['e']) : '';
$params = isset($in['p']) && is_array($in['p']) ? $in['p'] : [];
$cookie = isset($in['cookie']) ? (string)$in['cookie'] : '';
$isGet = !empty($in['get']);

if ($endpoint === '') {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'Пустой endpoint'], JSON_UNESCAPED_UNICODE);
    exit;
}
// Защита от обращения к произвольным хостам: допускаем только путь внутри журнала.
if (strpos($endpoint, '/') !== 0 || preg_match('#[?#:\s]#', $endpoint)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'Недопустимый endpoint'], JSON_UNESCAPED_UNICODE);
    exit;
}

$url = $BASE . $endpoint;

$ch = curl_init();
$curlOpts = [
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_CONNECTTIMEOUT => 30,
    CURLOPT_TIMEOUT => 120,
    CURLOPT_USERAGENT => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) SchoolDiaryWeb/1.0',
    CURLOPT_HTTPHEADER => ['X-Requested-With: XMLHttpRequest', 'Referer: ' . $BASE . '/'],
];

if ($isGet) {
    $curlOpts[CURLOPT_HTTPGET] = true;
} else {
    $curlOpts[CURLOPT_POST] = true;
    $curlOpts[CURLOPT_POSTFIELDS] = http_build_query($params);
}

if ($cookie !== '') {
    $curlOpts[CURLOPT_COOKIE] = $cookie;
}

curl_setopt_array($ch, $curlOpts);
$body = curl_exec($ch);
$errno = curl_errno($ch);
$error = curl_error($ch);
$code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($body === false) {
    http_response_code(502);
    echo json_encode(['ok' => false, 'error' => 'Ошибка сети к журналу: ' . $error], JSON_UNESCAPED_UNICODE);
    exit;
}

echo json_encode([
    'ok' => true,
    'code' => $code,
    'text' => $body,
], JSON_UNESCAPED_UNICODE);