# ЛинТех 28 — дневник для ПК

Нативная десктопная версия школьного дневника на Kotlin + Compose Desktop
(Windows и Linux, без WebView). Исходники — в `desktop/`, там же инструкции
по сборке и упаковке.

## Сборка

```bash
cd desktop
./gradlew run
./gradlew packageDeb          # Debian/Ubuntu
./gradlew packageRpmWithMenu  # Fedora/RHEL, с нормальным ярлыком
./gradlew packageMsi          # Windows + WiX Toolset 3
```

## CI

`.github/workflows/desktop.yml` собирает:

- Linux: `.deb` и `.rpm` (`ubuntu-latest`);
- Windows: `.msi` (`windows-latest`, WiX 3).

Готовые файлы публикуются в Artifacts соответствующего запуска.
