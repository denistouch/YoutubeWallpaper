package org.denistouch.youtubescreensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeUrlParserTest {

    private val expectedId = "g6Ye4xwXyAw"

    @Test
    fun watchUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=$expectedId"))
    }

    @Test
    fun watchUrlWithExtraParams() {
        assertEquals(
            expectedId,
            YoutubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=$expectedId&t=42s&list=PLxyz"),
        )
    }

    @Test
    fun watchUrlVNotFirstParam() {
        assertEquals(
            expectedId,
            YoutubeUrlParser.extractVideoId("https://www.youtube.com/watch?list=PLxyz&v=$expectedId"),
        )
    }

    @Test
    fun shortUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://youtu.be/$expectedId"))
    }

    @Test
    fun shortUrlWithParams() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://youtu.be/$expectedId?si=abcDEF123_-"))
    }

    @Test
    fun embedUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://www.youtube.com/embed/$expectedId"))
    }

    @Test
    fun vUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://www.youtube.com/v/$expectedId"))
    }

    @Test
    fun shortsUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://www.youtube.com/shorts/$expectedId"))
    }

    @Test
    fun liveUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://www.youtube.com/live/$expectedId"))
    }

    @Test
    fun mobileUrl() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("https://m.youtube.com/watch?v=$expectedId"))
    }

    @Test
    fun bareId() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId(expectedId))
    }

    @Test
    fun bareIdWithSurroundingWhitespace() {
        assertEquals(expectedId, YoutubeUrlParser.extractVideoId("   $expectedId  "))
    }

    @Test
    fun idWithUnderscoreAndDash() {
        val id = "abc_DEF-123"
        assertEquals(id, YoutubeUrlParser.extractVideoId("https://youtu.be/$id"))
    }

    @Test
    fun emptyReturnsNull() {
        assertNull(YoutubeUrlParser.extractVideoId(""))
        assertNull(YoutubeUrlParser.extractVideoId("   "))
        assertNull(YoutubeUrlParser.extractVideoId(null))
    }

    @Test
    fun nonYoutubeUrlReturnsNull() {
        assertNull(YoutubeUrlParser.extractVideoId("https://example.com/watch?v=short"))
    }

    @Test
    fun tooShortIdReturnsNull() {
        assertNull(YoutubeUrlParser.extractVideoId("abc123"))
    }
}
