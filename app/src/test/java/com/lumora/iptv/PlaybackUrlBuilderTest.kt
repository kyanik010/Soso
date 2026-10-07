package com.lumora.iptv

import com.lumora.iptv.player.PlaybackSource
import com.lumora.iptv.player.PlaybackUrlBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackUrlBuilderTest {

    @Test
    fun testNormalizeBaseUrl_addsHttpIfMissingAndTrimsSlash() {
        assertEquals("http://myiptv.com:8080", PlaybackUrlBuilder.normalizeBaseUrl("myiptv.com:8080/"))
        assertEquals("https://secure.iptv.org", PlaybackUrlBuilder.normalizeBaseUrl("https://secure.iptv.org/"))
    }

    @Test
    fun testBuild_liveStreamUrl() {
        val builder = PlaybackUrlBuilder("http://server.net:8000/", "testuser", "testpass")
        val url = builder.build(PlaybackSource.Live(streamId = 1234, container = "ts"))
        assertEquals("http://server.net:8000/live/testuser/testpass/1234.ts", url)
    }

    @Test
    fun testBuild_vodStreamUrl() {
        val builder = PlaybackUrlBuilder("http://server.net:8000", "testuser", "testpass")
        val url = builder.build(PlaybackSource.Vod(streamId = 999, container = "mp4"))
        assertEquals("http://server.net:8000/movie/testuser/testpass/999.mp4", url)
    }

    @Test
    fun testBuild_episodeStreamUrl() {
        val builder = PlaybackUrlBuilder("http://server.net:8000", "testuser", "testpass")
        val url = builder.build(PlaybackSource.Episode(episodeId = "ep_42", container = "mkv"))
        assertEquals("http://server.net:8000/series/testuser/testpass/ep_42.mkv", url)
    }

    @Test
    fun testBuild_m3uUrlPassthrough() {
        val builder = PlaybackUrlBuilder()
        val directUrl = "http://direct-stream.tv/live.m3u8"
        val url = builder.build(PlaybackSource.M3uUrl(directUrl))
        assertEquals(directUrl, url)
    }
}
