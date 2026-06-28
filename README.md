# Youtube Screensaver (Android TV)

Заставка (`DreamService`) для Android TV, проигрывающая YouTube-видео во весь экран
через YouTube IFrame Player JS API в `WebView`.

Пакет: `org.denistouch.youtubescreensaver`

## Возможности

- Добавление видео по ссылке YouTube (`watch?v=`, `youtu.be/`, `embed/`, `shorts/`, `live/`)
  или по голому 11-символьному ID — извлекается автоматически.
- Добавление видео с телефона: приложение поднимает локальный HTTP-сервер (порт 18080),
  показывает QR-код; телефон сканирует, открывает форму в браузере, вставляет ссылку.
- Название видео подтягивается из oEmbed YouTube без API-ключа.
- Список видео с превью-миниатюрами, выбор активного, удаление.
- Кнопка «Сделать активной заставкой» — назначает приложение системной заставкой
  без ADB (требует разового `pm grant`, см. ниже).
- Выбор времени простоя до запуска заставки.
- Воспроизведение во весь экран: автозапуск, зацикливание, без элементов управления.

## Требования к окружению

| Инструмент | Версия |
|---|---|
| JDK | 17 |
| Android SDK platform | 34 |
| Android SDK build-tools | 34.0.0 |
| Gradle (wrapper) | 8.7 |

Укажите путь к SDK в `local.properties` (файл в `.gitignore`):

```
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

## Сборка

```bash
# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Юнит-тесты парсера ссылок
./gradlew testDebugUnitTest
```

## Установка на реальное устройство

### 1. Включить ADB на ТВ

**Настройки → О устройстве** — 7 раз нажать «Сборка» (режим разработчика).  
**Настройки → Система → Для разработчиков** — включить **Отладку по сети (ADB)**.

```bash
adb connect <IP_телевизора>:5555
adb devices   # убедиться что устройство в списке
```

### 2. Установить APK

```bash
adb -s <IP>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
```

> **Важно (TCL и ряд других прошивок):** после установки откройте приложение вручную
> из лаунчера, прежде чем пытаться запустить заставку. Прошивка блокирует bind
> `DreamService` пока приложение находится в состоянии «stopped».

### 3. Выдать разрешение (один раз)

Для кнопок «Сделать активной заставкой» и «Применить» (таймаут) нужно привилегированное
разрешение `WRITE_SECURE_SETTINGS`. Выдаётся один раз с компьютера:

```bash
adb shell pm grant org.denistouch.youtubescreensaver android.permission.WRITE_SECURE_SETTINGS
```

После этого обе функции работают прямо из приложения без ADB.

### 4. Назначить заставку вручную (альтернатива)

Если кнопка недоступна или нужно сделать через ADB:

```bash
adb shell settings put secure screensaver_components \
    org.denistouch.youtubescreensaver/.VideoScreensaverService
adb shell settings put secure screensaver_enabled 1
adb shell settings put secure screensaver_activate_on_sleep 1
adb shell settings put system screen_off_timeout 360000   # 6 минут, минимум на Android 12
```

### 5. Проверить заставку немедленно

```bash
adb shell am start -n com.android.systemui/.Somnambulator
```

## Добавление видео с телефона (QR)

1. Откройте приложение на ТВ → нажмите **«Добавить с телефона (QR)»**.
2. Отсканируйте QR-код телефоном (телефон и ТВ должны быть в одной Wi-Fi сети).
3. В открывшемся браузере вставьте ссылку на YouTube-видео и нажмите **Добавить**.

Видео появится в списке на ТВ без пульта и ввода с экранной клавиатуры.

## Технические примечания

**Origin WebView.** `loadDataWithBaseURL` должен использовать нейтральный сторонний
домен (`https://www.example.com`), то же значение передаётся в `playerVars.origin`.
Если указать `https://www.youtube.com` — плеер считает страницу «первой стороной»
и возвращает ошибку 152 на всех видео без исключения.

**Android 12 / Google TV.** Сторонние заставки не отображаются в системном меню
выбора заставки. Назначение — через кнопку в приложении (после `pm grant`) или ADB.

**Embed-ограничения YouTube.** Некоторые видео запрещены правообладателем для
встраивания — плеер вернёт ошибку 101 или 150. Это ограничение YouTube, не приложения.
