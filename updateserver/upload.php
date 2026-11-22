<?php
// Загрузка новой версии APK на Beget
// Загружает файл в /apk/SchoolDiary.apk и обновляет /update.json

$apkDir = __DIR__ . '/apk';
$apkFile = $apkDir . '/SchoolDiary.apk';
$jsonFile = __DIR__ . '/update.json';

$dir = $apkDir;
$file = $apkFile;
$json = $jsonFile;

function fail($msg) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'message' => $msg], JSON_UNESCAPED_UNICODE);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'message' => 'Только POST'], JSON_UNESCAPED_UNICODE);
    exit;
}

// Проверка пароля на загрузку (хранится в виде SHA-256)
$passwordHash = '03ae3dcea8922ef48bb95481925af5c21f2ae2529758d1a89739ad465cbd06bd';
$provided = isset($_POST['password']) ? (string)$_POST['password'] : '';
if (!hash_equals($passwordHash, hash('sha256', $provided))) {
    http_response_code(403);
    echo json_encode(['ok' => false, 'message' => 'Неверный пароль'], JSON_UNESCAPED_UNICODE);
    exit;
}

if (!isset($_FILES['apk']) || $_FILES['apk']['error'] !== UPLOAD_ERR_OK) {
    fail('Файл не был загружен');
}
if ($_FILES['apk']['size'] > 100 * 1024 * 1024) {
    fail('Файл слишком большой (макс. 100 МБ)');
}
$ext = strtolower(pathinfo($_FILES['apk']['name'], PATHINFO_EXTENSION));
if ($ext !== 'apk') {
    fail('Нужен файл .apk');
}

if (!is_dir($dir)) {
    mkdir($dir, 0755, true);
}

if (!move_uploaded_file($_FILES['apk']['tmp_name'], $file)) {
    fail('Не удалось сохранить файл');
}

// Читаем опциональную версию из формы
$version = isset($_POST['version']) && trim($_POST['version']) !== ''
    ? trim($_POST['version'])
    : '1.0';

$notes = isset($_POST['notes']) && trim($_POST['notes']) !== ''
    ? trim($_POST['notes'])
    : 'Обновление';

// Читаем прошлый versionCode для инкремента
$oldCode = 1;
if (file_exists($json)) {
    $old = json_decode(file_get_contents($json), true);
    if ($old && isset($old['versionCode']) && is_numeric($old['versionCode'])) {
        $oldCode = (int)$old['versionCode'];
    }
}
$newCode = $oldCode + 1;

$data = [
    'versionCode' => $newCode,
    'versionName' => $version,
    'apkUrl' => 'http://' . $_SERVER['HTTP_HOST'] . '/apk/SchoolDiary.apk',
    'notes' => $notes
];

if (file_put_contents($json, json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT)) === false) {
    fail('Не удалось записать update.json');
}

echo json_encode([
    'ok' => true,
    'message' => 'Версия ' . $version . ' загружена (код ' . $newCode . ')'
], JSON_UNESCAPED_UNICODE);