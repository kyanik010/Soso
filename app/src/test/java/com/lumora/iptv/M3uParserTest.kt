package com.lumora.iptv

import com.lumora.iptv.data.iptv.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class M3uParserTest {

    @Test
    fun testParse_extractsMetadataCorrectly() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="beIN1" tvg-name="beIN Sports 1" tvg-logo="http://logos.net/1.png" group-title="Sports",beIN Sports 1 HD
            http://stream.net/live/ch1.m3u8

            #EXTINF:-1 group-title="Movies" tvg-logo="http://logos.net/movie.png",Inception 2010
            http://stream.net/vod/movie1.mp4
        """.trimIndent()

        val result = M3uParser.parse(sampleM3u)

        assertEquals(1, result.channels.size)
        assertEquals(1, result.movies.size)

        val channel = result.channels[0]
        assertEquals("beIN Sports 1 HD", channel.name)
        assertEquals("beIN1", channel.epgChannelId)
        assertEquals("http://logos.net/1.png", channel.logoUrl)
        assertEquals("http://stream.net/live/ch1.m3u8", channel.streamUrl)

        val movie = result.movies[0]
        assertEquals("Inception 2010", movie.title)
        assertEquals("http://logos.net/movie.png", movie.posterUrl)
        assertEquals("http://stream.net/vod/movie1.mp4", movie.streamUrl)
        assertEquals("mp4", movie.containerExtension)
    }

    @Test
    fun testParse_handlesEmptyLinesAndMalformedEntriesResiliently() {
        val malformedM3u = """
            #EXTM3U
            
            #EXTINF:-1,Valid Channel
            http://valid.com/live.ts
            
            BROKEN LINE WITHOUT EXTINF
            http://broken.com/ch.ts
            
            #EXTINF:-1 tvg-id=""
            http://lonely.com/ch.ts
        """.trimIndent()

        val result = M3uParser.parse(malformedM3u)
        assertNotNull(result)
        assertEquals(2, result.channels.size)
    }
}
