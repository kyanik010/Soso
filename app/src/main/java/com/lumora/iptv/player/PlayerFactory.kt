package com.lumora.iptv.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lumora.iptv.util.AppLogger

@OptIn(UnstableApi::class)
object PlayerFactory {

    private const val USER_AGENT = "Lumora/1.0"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000

    fun createHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
    }

    fun createPlayer(context: Context): ExoPlayer {
        val httpDataSource = createHttpDataSourceFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSource)

        // Robust load control with 50s buffer to prevent stutter on 5,000+ real world networks
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // min buffer
                50_000, // max buffer
                2_500,  // buffer for playback
                5_000   // buffer for playback after rebuffer
            )
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                playWhenReady = true
            }
    }

    /**
     * Builds a MediaItem with optional subtitles and MIME type auto-detection.
     */
    fun buildMediaItem(
        streamUrl: String,
        subtitleUrl: String? = null,
        subtitleLanguage: String = "ar"
    ): MediaItem {
        val uri = Uri.parse(streamUrl)
        val builder = MediaItem.Builder().setUri(uri)

        // Resolve MIME types based on stream extension if specified
        when {
            streamUrl.contains(".m3u8", ignoreCase = true) -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            streamUrl.contains(".mpd", ignoreCase = true) -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            streamUrl.startsWith("rtsp://", ignoreCase = true) -> builder.setMimeType(MimeTypes.APPLICATION_RTSP)
            streamUrl.contains(".mp4", ignoreCase = true) -> builder.setMimeType(MimeTypes.VIDEO_MP4)
            streamUrl.contains(".mkv", ignoreCase = true) -> builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
        }

        // Add subtitle configuration if provided
        if (!subtitleUrl.isNullOrBlank()) {
            val subUri = Uri.parse(subtitleUrl)
            val subMime = if (subtitleUrl.endsWith(".srt", ignoreCase = true)) {
                MimeTypes.APPLICATION_SUBRIP
            } else {
                MimeTypes.TEXT_VTT
            }

            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(subMime)
                .setLanguage(subtitleLanguage)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            builder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        return builder.build()
    }

    /**
     * Audio Track Selection (Section 25):
     * Switches the active audio track using Media3 TrackSelectionParameters.
     */
    fun selectAudioTrack(player: ExoPlayer, trackGroupIndex: Int, trackIndex: Int, tracks: Tracks) {
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (trackGroupIndex in audioGroups.indices) {
            val group = audioGroups[trackGroupIndex].mediaTrackGroup
            val params = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(
                    androidx.media3.common.TrackSelectionOverride(group, trackIndex)
                )
                .build()
            player.trackSelectionParameters = params
            AppLogger.i("PlayerFactory", "Selected audio track $trackIndex in group $trackGroupIndex")
        }
    }
}
