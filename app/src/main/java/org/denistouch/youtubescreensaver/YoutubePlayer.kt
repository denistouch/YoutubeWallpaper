package org.denistouch.youtubescreensaver

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebView

/**
 * Общая логика воспроизведения YouTube-видео во весь экран через IFrame Player JS API
 * в [WebView]. Используется и заставкой ([VideoScreensaverService]), и ручным запуском
 * ([PlayerActivity]), чтобы не дублировать настройку WebView и HTML плеера.
 */
object YoutubePlayer {

    /** Создаёт настроенный для автозапуска видео [WebView] на чёрном фоне. */
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Без этого WebView блокирует автозапуск видео без пользовательского жеста.
            mediaPlaybackRequiresUserGesture = false
        }
    }

    /** Загружает видео [videoId] в [webView]. */
    fun load(webView: WebView, videoId: String) {
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
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

    /**
     * HTML-страница c YouTube IFrame Player API. Видео занимает весь экран,
     * автозапуск, зацикливание, без элементов управления.
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
                            playlist: '$videoId'
                        },
                        events: {
                            onReady: function(e) { e.target.playVideo(); },
                            onStateChange: function(e) {
                                if (e.data === YT.PlayerState.ENDED) {
                                    player.playVideo();
                                }
                            }
                        }
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
