# Релизы

В репозитории две программы: Android-приложение (`ios26-redesign/`) и десктопная
версия (`desktop/`). Их версии нумеруются независимо, поэтому один тег может
относиться только к одной из них.

| Релиз | Что внутри | Тег указывает на | Состав |
|---|---|---|---|
| 3.0.0 | Android 3.0.0-ios26 + десктоп 3.0.0 | `3a04127` | APK `SchoolDiary_ios26.apk`, `.deb`, `.rpm`, `.msi` |
| 3.0.1 | десктоп 3.0.1 | `d2cc4b3` | `.deb`, `.rpm`, `.msi`, APK `SchoolDiary_3.0.1.apk` |
| 3.0.2 | десктоп 3.0.2 | `db61787` | `.deb`, `.rpm`, `.msi`, APK `SchoolDiary_3.0.2.apk` |
| 3.0.3 | Android 3.0.3 | `0f4c5bb` | APK `SchoolDiary_3.0.3.apk` |

## Как выбрать файл

- **Телефон или планшет:** `SchoolDiary_*.apk` из последнего релиза.
- **Linux:** `.deb` (Debian/Ubuntu) или `.rpm` (Fedora/RHEL).
- **Windows:** `.msi`.

## Нумерация

- Android: `versionCode` и `versionName` в `ios26-redesign/app/build.gradle`.
- Десктоп: `version` в `desktop/build.gradle.kts`.

Перед релизом версия поднимается отдельным PR, затем ставится тег `vX.Y.Z`
на коммит из `main` и создаётся релиз с собранным APK.
