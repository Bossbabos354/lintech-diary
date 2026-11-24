<?php
// === Админ-API расписания (звонки + уроки по классам) ===
// Хранилище: schedule.json в этой папке
// Действия (POST, JSON): action=load | action=save
// Защита: пароль SHA-256 (тот же, что в upload.php)
// Совместимость: PHP 5.6+

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

define('JSON_FILE', __DIR__ . '/schedule.json');
require_once __DIR__ . '/config.php';

error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

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

function emptySchedule() {
    return array(
        'classes' => array(),
        'bells' => array('weekday' => array(), 'saturday' => array()),
        'lessons' => new stdClass()
    );
}

function loadSchedule() {
    if (!file_exists(JSON_FILE)) return emptySchedule();
    $raw = @json_decode(file_get_contents(JSON_FILE), true);
    if (!is_array($raw)) return emptySchedule();
    if (!isset($raw['classes']) || !is_array($raw['classes'])) $raw['classes'] = array();
    if (!isset($raw['bells']) || !is_array($raw['bells'])) {
        $raw['bells'] = array('weekday' => array(), 'saturday' => array());
    }
    if (!isset($raw['bells']['weekday'])) $raw['bells']['weekday'] = array();
    if (!isset($raw['bells']['saturday'])) $raw['bells']['saturday'] = array();
    if (!isset($raw['lessons']) || !is_array($raw['lessons'])) $raw['lessons'] = new stdClass();
    return $raw;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    respond(array('ok' => false, 'message' => 'Только POST'), 405);
}

$body = json_decode(file_get_contents('php://input'), true);
if (!is_array($body)) $body = array();
$action = isset($body['action']) ? $body['action'] : '';

switch ($action) {

    case 'load':
        respond(array('ok' => true, 'schedule' => loadSchedule()));

    case 'save':
        $password = isset($body['password']) ? (string)$body['password'] : '';
        checkAuth($password);

        $schedule = isset($body['schedule']) ? $body['schedule'] : null;
        if (!is_array($schedule)) fail('Нет данных расписания');

        // Нормализуем структуру, чтобы не сломать приложение
        if (!isset($schedule['classes']) || !is_array($schedule['classes'])) $schedule['classes'] = array();
        if (!isset($schedule['bells']) || !is_array($schedule['bells'])) {
            $schedule['bells'] = array('weekday' => array(), 'saturday' => array());
        }
        if (!isset($schedule['bells']['weekday'])) $schedule['bells']['weekday'] = array();
        if (!isset($schedule['bells']['saturday'])) $schedule['bells']['saturday'] = array();
        if (!isset($schedule['lessons']) || !is_array($schedule['lessons'])) $schedule['lessons'] = new stdClass();

        $json = json_encode($schedule, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        if ($json === false) fail('Не удалось закодировать JSON');
        if (file_put_contents(JSON_FILE, $json) === false) fail('Не удалось записать schedule.json');

        respond(array('ok' => true, 'message' => 'Расписание сохранено'));

    default:
        fail('Неизвестное действие');
}