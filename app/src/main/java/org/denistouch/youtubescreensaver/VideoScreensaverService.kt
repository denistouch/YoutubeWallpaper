package org.denistouch.youtubescreensaver

import android.annotation.SuppressLint
import android.graphics.Color
import android.service.dreams.DreamService
import android.webkit.WebView

/**
 * Заставка (DreamService), проигрывающая видео YouTube во весь экран через
 * YouTube IFrame Player JS API, загруженный в [WebView].
 *
 * Используется IFrame API (а не голый тег <iframe> и не сторонняя библиотека), чтобы
 * программно запускать воспроизведение, зацикливать видео и скрывать элементы управления.
 */
class VideoScreensaverService : DreamService() {

    private var webView: WebView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()

        val videoId = VideoStore(this).getSelectedVideoId()
        if (videoId.isNullOrEmpty()) {
            // Нет выбранного видео — показываем чёрный экран вместо падения.
            return
        }

        val view = createWebView()
        webView = view
        setContentView(view)
        view.loadDataWithBaseURL(
            "https://www.youtube.com",
            buildPlayerHtml(videoId),
            "text/html",
            "utf-8",
            null,
        )
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        releaseWebView()
    }

    override fun onDetachedFromWindow() {
        releaseWebView()
        super.onDetachedFromWindow()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView = WebView(this).apply {
        setBackgroundColor(Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Без этого WebView блокирует автозапуск видео без пользовательского жеста.
            mediaPlaybackRequiresUserGesture = false
        }
    }

    private fun releaseWebView() {
        webView?.apply {
            loadUrl("about:blank")
            stopLoading()
            destroy()
        }
        webView = null
    }

    /**
     * HTML-страница c YouTube IFrame Player API. Видео занимает весь экран,
     * автозапуск, зацикливание, без элементов управления.
     */
    private fun buildPlayerHtml(videoId: String): String = """
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
