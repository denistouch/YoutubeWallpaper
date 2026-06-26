package org.denistouch.youtubescreensaver

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Получает название видео через публичный oEmbed-эндпоинт YouTube.
 * Не требует API-ключа. Должен вызываться в фоновом потоке.
 */
object YoutubeTitleFetcher {

    /**
     * @return название видео либо null при любой ошибке сети/парсинга.
     */
    fun fetchTitle(videoId: String): String? {
        val endpoint =
            "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        return runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).getString("title")
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
