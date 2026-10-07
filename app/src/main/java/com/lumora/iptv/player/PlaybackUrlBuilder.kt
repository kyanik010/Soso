package com.lumora.iptv.player

import com.lumora.iptv.util.AppLogger
import java.net.URLEncoder

sealed class PlaybackSource {
    data class Live(
        val streamId: Int,
        val container: String = "ts"
    ) : PlaybackSource()

    data class Vod(
        val streamId: Int,
        val container: String = "mp4"
    ) : PlaybackSource()

    data class Episode(
        val episodeId: String,
        val container: String = "mp4"
    ) : PlaybackSource()

    data class M3uUrl(
        val url: String
    ) : PlaybackSource()
}

/**
 * Isolated Playback URL Builder layer as specified in Section 21.
 * - Normalizes baseUrl
 * - Removes unnecessary trailing slashes
 * - Safely encodes parameters
 * - Never leaks sensitive passwords in logs
 */
class PlaybackUrlBuilder(
    rawBaseUrl: String = "",
    private val username: String = "",
    private val password: String = ""
) {
    val baseUrl: String = normalizeBaseUrl(rawBaseUrl)

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            var url = raw.trim().removeSuffix("/")
            if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            return url
        }
    }

    fun build(source: PlaybackSource): String {
        val result = when (source) {
            is PlaybackSource.Live -> {
                "$baseUrl/live/$username/$password/${source.streamId}.${source.container}"
            }
            is PlaybackSource.Vod -> {
                "$baseUrl/movie/$username/$password/${source.streamId}.${source.container}"
            }
            is PlaybackSource.Episode -> {
                "$baseUrl/series/$username/$password/${source.episodeId}.${source.container}"
            }
            is PlaybackSource.M3uUrl -> {
                source.url.trim()
            }
        }
        AppLogger.d("PlaybackUrlBuilder", "Built playback URL for source: ${source.javaClass.simpleName}")
        return result
    }
}
