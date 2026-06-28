package org.denistouch.youtubescreensaver

import android.service.dreams.DreamService
import android.webkit.WebView

/**
 * Заставка (DreamService), проигрывающая видео YouTube во весь экран через
 * YouTube IFrame Player JS API (см. [YoutubePlayer]).
 *
 * Примечание: на Android TV / Google TV системное меню «Спящий режим» не показывает
 * сторонние (sideload) заставки. Чтобы запустить воспроизведение вручную без ADB,
 * используется [PlayerActivity] (кнопка в настройках).
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

        val view = YoutubePlayer.createWebView(this)
        webView = view
        setContentView(view)
        YoutubePlayer.load(view, videoId)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        releaseWebView()
    }

    override fun onDetachedFromWindow() {
        releaseWebView()
        super.onDetachedFromWindow()
    }

    private fun releaseWebView() {
        webView?.let { YoutubePlayer.release(it) }
        webView = null
    }
}
