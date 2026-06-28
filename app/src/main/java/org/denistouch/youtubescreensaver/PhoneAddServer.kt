package org.denistouch.youtubescreensaver

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface

class PhoneAddServer(
    private val onVideoAdded: (Video) -> Unit,
) : NanoHTTPD(PORT) {

    override fun serve(session: IHTTPSession): Response {
        val url = session.parameters["url"]?.firstOrNull()
        if (!url.isNullOrBlank()) {
            val videoId = YoutubeUrlParser.extractVideoId(url)
            if (videoId != null) {
                val title = YoutubeTitleFetcher.fetchTitle(videoId) ?: videoId
                onVideoAdded(Video(videoId, title))
                return newFixedLengthResponse(Response.Status.OK, MIME_HTML, successHtml(title))
            }
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, formHtml("Не удалось распознать ссылку YouTube"))
        }
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, formHtml())
    }

    companion object {
        const val PORT = 18080
        private const val MIME_HTML = "text/html; charset=utf-8"

        fun localIp(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { iface ->
                    if (!iface.isUp || iface.isLoopback) emptyList()
                    else iface.inetAddresses.toList()
                }
                ?.firstOrNull { addr ->
                    !addr.isLoopbackAddress && addr is Inet4Address && addr.isSiteLocalAddress
                }
                ?.hostAddress
        }.getOrNull()

        fun qrBitmap(text: String, sizePx: Int = 512): Bitmap {
            val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        }

        private fun formHtml(error: String? = null) = """
            <!DOCTYPE html>
            <html lang="ru">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Добавить видео</title>
            <style>
            *{box-sizing:border-box}
            body{background:#0f1216;color:#e7ebf1;font-family:system-ui,sans-serif;
                 display:flex;flex-direction:column;align-items:center;justify-content:center;
                 min-height:100vh;margin:0;padding:24px}
            h1{font-size:20px;margin:0 0 24px;text-align:center}
            input{width:100%;max-width:420px;padding:14px 16px;
                  border:2px solid #252c36;border-radius:12px;
                  background:#1a1f27;color:#e7ebf1;font-size:16px;outline:none}
            input:focus{border-color:#5aa0ff}
            button{margin-top:12px;width:100%;max-width:420px;padding:14px;
                   background:#5aa0ff;color:#fff;border:none;border-radius:12px;
                   font-size:16px;cursor:pointer;font-weight:600}
            .err{color:#ff6b6b;margin-top:12px;text-align:center;font-size:14px}
            </style>
            </head>
            <body>
            <h1>Добавить видео в заставку</h1>
            <form method="get" action="/">
              <input name="url" type="url" placeholder="https://youtube.com/watch?v=…"
                     autofocus required>
              <button type="submit">Добавить</button>
            </form>
            ${if (error != null) "<p class=\"err\">$error</p>" else ""}
            </body>
            </html>
        """.trimIndent()

        private fun successHtml(title: String): String {
            val safe = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Добавлено</title>
                <style>
                body{background:#0f1216;color:#e7ebf1;font-family:system-ui,sans-serif;
                     display:flex;flex-direction:column;align-items:center;justify-content:center;
                     min-height:100vh;margin:0;padding:24px;text-align:center}
                h1{color:#54d08a;font-size:28px;margin:0 0 12px}
                p{color:#9aa3b0;margin:0 0 28px;font-size:15px}
                a{color:#5aa0ff;text-decoration:none;font-size:16px;
                  border:2px solid #5aa0ff;padding:12px 24px;border-radius:12px}
                </style>
                </head>
                <body>
                <h1>✓ Добавлено</h1>
                <p>$safe</p>
                <a href="/">← Добавить ещё</a>
                </body>
                </html>
            """.trimIndent()
        }
    }
}
