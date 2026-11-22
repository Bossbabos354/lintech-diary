# ЛинТех 28 — Дневник для ПК

Нативная десктопная версия школьного дневника на **Kotlin + Compose Desktop**.
Работает на Windows и Linux (Debian/Ubuntu, Fedora), не использует WebView и браузерный движок.

## Что перенесено из Android-версии

- вход по логину и паролю журнала АВЕРС (`journal.school28-kirov.ru`);
- дневник по неделям: оценки, темы, домашние задания, чек-лист «сделал»;
- оценки по предметам: средний балл, итоговые за четверти, цели;
- расписание классов и звонков с сайта школы;
- цели по предметам с расчётом «сколько оценок ещё нужно»;
- критические предметы и итоги недели;
- события школы со ссылками (открываются в браузере);
- уведомления об оценках, ДЗ, событиях и новых версиях, тихие часы и дайджест;
- работа в системном трее, офлайн-кэш дневника и оценок.

Не перенесено (осознанно): виджеты рабочего стола, QR-сканер, картинки в событиях,
отдельный экран поиска.

## Требования

- JDK 21 для сборки (приложение несёт собственный рантайм, у пользователя Java не нужна);
- Gradle не нужен — используется wrapper.

## Запуск из исходников

```bash
cd desktop
./gradlew run
```

## Сборка дистрибутивов

```bash
cd desktop
./gradlew createDistributable   # app-image: build/compose/binaries/main/app
./gradlew packageDeb            # Debian/Ubuntu (нужен dpkg-deb)
./gradlew packageRpm            # Fedora/RHEL, стандартный RPM от jpackage (нужен rpm-build)
./gradlew packageRpmWithMenu    # Fedora/RHEL, RPM с нормальным ярлыком в меню
./gradlew packageMsi            # только на Windows, нужен WiX Toolset 3
```

Кросс-сборки нет: `.deb`/`.rpm` собираются на Linux, `.msi` — на Windows.
Готовый CI — `.github/workflows/desktop.yml` (матрица `ubuntu-latest` + `windows-latest`).

На Fedora для rpm: `sudo dnf install rpm-build`.
На Debian/Ubuntu для deb: `sudo apt install dpkg-dev fakeroot`.

## Где хранятся данные

| Что | Linux | Windows |
|---|---|---|
| Настройки, логин и пароль | `~/.config/lintech28/settings.properties` | `%APPDATA%\Linteh28\settings.properties` |
| Кэш дневника и оценок | `~/.cache/lintech28` | `%LOCALAPPDATA%\Linteh28\cache` |
| Лог | `~/.local/share/lintech28/diary.log` | `%LOCALAPPDATA%\Linteh28\data\diary.log` |

Файл настроек на Linux создаётся с правами `600` (только владелец).
Пароль хранится в открытом виде — как и в Android-версии; системный keyring не используется.

## Обновления

Клиент проверяет `http://d91950hv.beget.tech/desktop/update.json`:

```json
{
  "versionName": "1.1.0",
  "url": "https://d91950hv.beget.tech/desktop/lintech28-diary_1.1.0_amd64.deb",
  "notes": "Что нового"
}
```

Пока файла нет, сервер отвечает 404 и приложение честно пишет, что канал обновлений не настроен.
Версия задаётся один раз — `version` в `build.gradle.kts`. Оттуда Gradle подставляет её
в `packageVersion`, в ресурс `version.properties`, а `BuildInfo.VERSION_NAME` читает ресурс.

## Структура

```
desktop/src/main/kotlin/ru/school28/diary/
├── api/            ApiClient (порт с Android), модели расписания
├── ui/             тема, экраны, общие компоненты, оверлеи
├── Settings.kt     настройки в Properties-файле
├── Background.kt   фоновая проверка (замена WorkManager)
├── Tray.kt         иконка в трее и уведомления
├── UpdateChecker.kt
├── Repo.kt         общие загрузки: класс, оценки, дневник
└── Main.kt         точка входа и навигация
```

## Проверка

```bash
cd desktop
./gradlew build        # компиляция, jar и тесты
./gradlew test         # 18 тестов: расчёт целей, склонения, границы недели, подстановка версии
./gradlew createDistributable
```
