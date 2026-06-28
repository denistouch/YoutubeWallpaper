package org.denistouch.youtubescreensaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewMediaIntegrityApiStatusConfig

/**
 * Общая логика воспроизведения YouTube-видео во весь экран через IFrame Player JS API
 * в [WebView]. Используется и заставкой ([VideoScreensaverService]), и ручным запуском,
 * чтобы не дублировать настройку WebView и HTML плеера.
 */
object YoutubePlayer {

    /** Тег для logcat: `adb logcat -s YoutubePlayer`. */
    const val TAG = "YoutubePlayer"

    /** Создаёт настроенный для автозапуска видео [WebView] на чёрном фоне. */
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Без этого WebView блокирует автозапуск видео без пользовательского жеста.
            mediaPlaybackRequiresUserGesture = false
            // YouTube отдаёт «обрезанный» плеер для TV/mobile UA — притворяемся десктопом.
            userAgentString = DESKTOP_UA
        }
        // Выключаем WebView Media Integrity API: sideload-сборка не проходит аттестацию,
        // из-за чего YouTube отдаёт onError 152 на любом видео. Без токена плеер
        // откатывается к обычным правилам web-embed.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS)) {
            val config = WebViewMediaIntegrityApiStatusConfig.Builder(
                WebViewMediaIntegrityApiStatusConfig.WEBVIEW_MEDIA_INTEGRITY_API_DISABLED,
            ).build()
            WebSettingsCompat.setWebViewMediaIntegrityApiStatus(settings, config)
        }
        // Пробрасываем console.* плеера в logcat — иначе ошибки YouTube не видны.
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.i(TAG, "console: ${msg.message()} @ ${msg.sourceId()}:${msg.lineNumber()}")
                return true
            }
        }
    }

    /** Загружает видео [videoId] в [webView]. */
    fun load(webView: WebView, videoId: String) {
        webView.loadDataWithBaseURL(
            BASE_URL,
            buildHtml(videoId),
            "text/html",
            "utf-8",
            null,
        )
    }

    /** Безопасно останавливает и освобождает [webView]. */
    fun release(webView: WebView) {
        webView.apply {
            loadUrl("about:blank")
            stopLoading()
            destroy()
        }
    }

    /** Origin страницы (совпадает с baseUrl в [load]) — нужен IFrame API для проверки.
     * Нейтральный сторонний домен: с origin youtube.com плеер считает нас «первой
     * стороной» и падает на проверке (onError 152). */
    private const val BASE_URL = "https://www.example.com"

    /** Десктопный Chrome UA, чтобы YouTube не подсовывал TV/mobile-вариант плеера. */
    private const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * HTML-страница c YouTube IFrame Player API. Видео занимает весь экран,
     * автозапуск, зацикливание, без элементов управления.
     *
     * Плеер грузим с `youtube-nocookie.com` и передаём `origin` — это повышает шанс
     * пройти проверки встраивания (ошибки 15x). Любую ошибку плеера логируем через console.
     */
    private fun buildHtml(videoId: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
                #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        playerVars: {
                            autoplay: 1,
                            controls: 0,
                            disablekb: 1,
                            fs: 0,
                            modestbranding: 1,
                            rel: 0,
                            playsinline: 1,
                            loop: 1,
                            playlist: '$videoId',
                            enablejsapi: 1,
                            origin: '$BASE_URL'
                        },
                        events: {
                            onReady: function(e) { e.target.playVideo(); },
                            onStateChange: function(e) {
                                if (e.data === YT.PlayerState.ENDED) {
                                    player.playVideo();
                                }
                            },
                            onError: function(e) {
                                console.error('YT onError code=' + e.data);
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
