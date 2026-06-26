# Youtube Screensaver (Android TV)

Заставка (DreamService) для Android TV, проигрывающая видео YouTube во весь экран
через YouTube IFrame Player JS API в `WebView`.

Пакет: `org.denistouch.youtubescreensaver`

## Возможности

- Добавление видео по любой ссылке YouTube (`watch?v=`, `youtu.be/`, `embed/`, `shorts/`,
  `live/`, голый id) — id извлекается автоматически ([`YoutubeUrlParser`](app/src/main/java/org/denistouch/youtubescreensaver/YoutubeUrlParser.kt)).
- Название видео подтягивается из публичного oEmbed-эндпоинта YouTube (без API-ключа).
- Список сохранённых видео в настройках: выбор активного видео, удаление.
- Воспроизведение во весь экран с автозапуском и зацикливанием, без элементов управления.

## Требования к окружению

- JDK 17
- Android SDK: platform-34, build-tools 34.0.0, platform-tools
- Путь к SDK укажите в `local.properties` (файл в `.gitignore`):
  ```
  sdk.dir=/opt/homebrew/share/android-commandlinetools
  ```

## Сборка и тесты

```bash
# Unit-тесты парсера ссылок
./gradlew testDebugUnitTest

# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug
```

## Запуск на эмуляторе Android TV

1. Установите образ системы Android TV и создайте AVD (через `sdkmanager`/`avdmanager` или
   Android Studio → Device Manager → Television):
   ```bash
   export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
   sdkmanager "system-images;android-34;android-tv;arm64-v8a"   # для Apple Silicon
   avdmanager create avd -n tv34 -k "system-images;android-34;android-tv;arm64-v8a" -d tv_1080p
   $ANDROID_HOME/emulator/emulator -avd tv34
   ```
   (пакет `emulator` ставится отдельно: `sdkmanager "emulator"`.)

2. Установите приложение:
   ```bash
   ./gradlew installDebug
   # или: adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Откройте приложение «Youtube Screensaver» из лаунчера, вставьте ссылку на видео,
   нажмите «Добавить» и выберите видео из списка.

4. Проверьте заставку:
   - назначьте её системной: **Настройки → Система → Заставка → Youtube Screensaver**;
   - запустите немедленно для проверки:
     ```bash
     adb shell am start -n com.android.systemui/.Somnambulator
     ```

## Установка dev-сборки на реальное устройство (Android TV / приставка)

1. На устройстве включите режим разработчика: **Настройки → Об устройстве** → 7 раз нажмите
   на «Сборка», затем в **Настройки → Система → Для разработчиков** включите
   **Отладку по USB / по сети (ADB)**.

2. Подключитесь по сети (у TV-приставок обычно нет USB-host):
   ```bash
   adb connect <IP_приставки>:5555
   adb devices            # убедитесь, что устройство в списке
   ```

3. Установите APK:
   ```bash
   adb -s <IP_приставки>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
   ```

4. Назначьте заставку в **Настройки → Система → Заставка** и задайте время запуска.

## Примечания

- Автозапуск видео в `WebView` обеспечивается флагом
  `mediaPlaybackRequiresUserGesture = false` и загрузкой страницы через
  `loadDataWithBaseURL("https://www.youtube.com", …)` (корректный Referer для IFrame API).
- Некоторые видео могут быть запрещены правообладателем для встраивания — тогда плеер
  покажет ошибку. Это ограничение YouTube, а не приложения.
