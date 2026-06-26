package org.denistouch.youtubescreensaver

import android.annotation.SuppressLint
import android.service.dreams.DreamService
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.preference.PreferenceManager

class VideoScreensaverService : DreamService() {
    private lateinit var webView: WebView

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        setContentView(R.layout.main_layout)

        webView = findViewById(R.id.webView)
        setupWebView(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString("youtube.id", "") ?: ""
        )
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Toast.makeText(baseContext, "Dreaming Started", Toast.LENGTH_SHORT).show()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Toast.makeText(baseContext, "Dreaming Stopped", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(videoId: String) {
        webView.apply {
            settings.javaScriptEnabled = true
            webChromeClient = object : WebChromeClient() {}
            loadData(getYouTubeHTML(videoId), "text/html", "utf-8")
        }
    }

    private fun getYouTubeHTML(videoId: String): String {
        return """
            <iframe 
                width="560" 
                height="315" 
                src="https://www.youtube.com/embed/$videoId?si=4sJVtRvCUPTmkh93" 
                title="YouTube video player" 
                frameborder="0" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                referrerpolicy="strict-origin-when-cross-origin" 
                allowfullscreen>
            </iframe>
        """.trimIndent()
    }
}