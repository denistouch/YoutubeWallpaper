package org.denistouch.youtubescreensaver

/**
 * Извлекает идентификатор видео YouTube из ссылок различных форматов.
 *
 * Поддерживаемые форматы:
 *  - https://www.youtube.com/watch?v=VIDEOID
 *  - https://youtu.be/VIDEOID
 *  - https://www.youtube.com/embed/VIDEOID
 *  - https://www.youtube.com/v/VIDEOID
 *  - https://www.youtube.com/shorts/VIDEOID
 *  - https://www.youtube.com/live/VIDEOID
 *  - https://m.youtube.com/watch?v=VIDEOID
 *  - голый идентификатор (11 символов)
 *
 * Лишние параметры (&t=, ?si=, list= и т.п.) игнорируются.
 */
object YoutubeUrlParser {

    /** Идентификатор видео YouTube всегда состоит из 11 символов [A-Za-z0-9_-]. */
    private const val ID = "[A-Za-z0-9_-]{11}"

    private val patterns = listOf(
        // watch?v=ID или любой параметр v=ID в query
        Regex("""[?&]v=($ID)"""),
        // youtu.be/ID
        Regex("""youtu\.be/($ID)"""),
        // /embed/ID, /v/ID, /shorts/ID, /live/ID, /e/ID
        Regex("""youtube\.com/(?:embed|v|shorts|live|e)/($ID)"""),
    )

    /** Голый 11-символьный идентификатор без какого-либо окружения. */
    private val bareIdRegex = Regex("""^$ID$""")

    /**
     * @return идентификатор видео либо null, если извлечь не удалось.
     */
    fun extractVideoId(input: String?): String? {
        val text = input?.trim().orEmpty()
        if (text.isEmpty()) return null

        if (bareIdRegex.matches(text)) return text

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}
