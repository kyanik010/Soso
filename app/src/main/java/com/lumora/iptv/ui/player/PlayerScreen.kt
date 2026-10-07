package com.lumora.iptv.ui.player

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.player.PlaybackSource
import com.lumora.iptv.player.PlaybackUrlBuilder
import com.lumora.iptv.player.PlayerFactory
import com.lumora.iptv.ui.theme.GoldyColors
import com.lumora.iptv.util.AppLogger
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    title: String,
    sourceId: String,
    mediaType: String, // "channel", "movie", "episode"
    repository: IptvRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var tracksInfo by remember { mutableStateOf<Tracks?>(null) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var subtitleDelayMs by remember { mutableLongStateOf(0L) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val sourceResult by produceState<Result<PlaybackSource?>?>(initialValue = null, key1 = sourceId) {
        value = runCatching { repository.getPlaybackSource(sourceId) }
    }

    if (sourceResult == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = GoldyColors.Cyan,
                modifier = Modifier.size(50.dp),
                strokeWidth = 3.dp
            )
        }
        return
    }

    val playbackSource = sourceResult.getOrNull()
    if (sourceResult.isFailure || playbackSource == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "تعذر العثور على مصدر التشغيل",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "سيتم الرجوع تلقائياً...",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
        LaunchedEffect(sourceId) {
            delay(3000)
            onBack()
        }
        return
    }

    val credentialsResult by produceState<Result<com.lumora.iptv.data.model.Credentials?>?>(initialValue = null, key1 = playbackSource) {
        value = runCatching { repository.getCredentials() }
    }

    if (playbackSource !is PlaybackSource.M3uUrl && credentialsResult == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = GoldyColors.Cyan,
                modifier = Modifier.size(50.dp),
                strokeWidth = 3.dp
            )
        }
        return
    }

    if (playbackSource !is PlaybackSource.M3uUrl && (
            credentialsResult?.isFailure == true ||
            credentialsResult?.getOrNull() == null
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "تعذر تحميل بيانات الاشتراك",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "سيتم الرجوع تلقائياً...",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }
        LaunchedEffect(sourceId) {
            delay(3000)
            onBack()
        }
        return
    }

    val streamUrl = if (playbackSource is PlaybackSource.M3uUrl) {
        playbackSource.url
    } else {
        val credentials = credentialsResult?.getOrNull()
        if (credentials == null) {
            AppLogger.e("PlayerScreen", "Unexpected null credentials after guards")
            onBack()
            return
        }
        PlaybackUrlBuilder(
            credentials.serverUrl,
            credentials.username,
            credentials.password
        ).build(playbackSource)
    }

    val isLive = mediaType == "channel"

    val player: ExoPlayer = remember(streamUrl) {
        PlayerFactory.createPlayer(context).apply {
            val mediaItem = PlayerFactory.buildMediaItem(streamUrl)
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                tracksInfo = tracks
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                AppLogger.e("PlayerScreen", "Playback Error: ${error.errorCodeName}", error)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    // Position updater for VOD
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
            .testTag("player_screen")
    ) {
        // Video View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            }
        )

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GoldyColors.Cyan,
                    modifier = Modifier.size(50.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Audio Track Button (Section 25)
                    IconButton(
                        onClick = { showAudioDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("audio_track_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio Tracks",
                            tint = GoldyColors.CyanLight
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Subtitle Button (Section 26)
                    IconButton(
                        onClick = { showSubtitleDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("subtitle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Subtitles",
                            tint = GoldyColors.CyanLight
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }
                }

                // Center Play / Rewind / Fast Forward
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                ) {
                    if (!isLive) {
                        IconButton(
                            onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0L)) },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(38.dp))
                        }

                        Spacer(modifier = Modifier.width(28.dp))
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(GoldyColors.Cyan)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = GoldyColors.BgDark,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    if (!isLive) {
                        Spacer(modifier = Modifier.width(28.dp))

                        IconButton(
                            onClick = { player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration)) },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                    }
                }

                // Bottom Seekbar & Time (VOD only)
                if (!isLive && durationMs > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Slider(
                            value = currentPositionMs.toFloat(),
                            onValueChange = { currentPositionMs = it.toLong() },
                            onValueChangeFinished = { player.seekTo(currentPositionMs) },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = GoldyColors.Cyan,
                                activeTrackColor = GoldyColors.Cyan,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = formatTime(currentPositionMs), color = Color.White, fontSize = 12.sp)
                            Text(text = formatTime(durationMs), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Audio Track Dialog (Section 25)
        if (showAudioDialog) {
            TrackSelectionDialog(
                title = "اختيار المسار الصوتي (Audio Track)",
                trackType = C.TRACK_TYPE_AUDIO,
                tracks = tracksInfo,
                player = player,
                onDismiss = { showAudioDialog = false }
            )
        }

        // Subtitle Dialog (Section 26)
        if (showSubtitleDialog) {
            TrackSelectionDialog(
                title = "اختيار الترجمة (Subtitles)",
                trackType = C.TRACK_TYPE_TEXT,
                tracks = tracksInfo,
                player = player,
                onDismiss = { showSubtitleDialog = false }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TrackSelectionDialog(
    title: String,
    trackType: Int,
    tracks: Tracks?,
    player: ExoPlayer,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(14.dp))
                .background(GoldyColors.PanelDark1)
                .padding(20.dp)
        ) {
            Text(text = title, color = GoldyColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(14.dp))

            // Disable track option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(trackType, true)
                            .build()
                        onDismiss()
                    }
                    .padding(vertical = 10.dp)
            ) {
                Text(text = "تعطيل المسار (Disabled)", color = Color.Red, fontSize = 14.sp)
            }

            // List available tracks
            val groups = tracks?.groups?.filter { it.type == trackType } ?: emptyList()
            if (groups.isEmpty()) {
                Text(text = "لا توجد مسارات إضافية متاحة في هذا البث", color = GoldyColors.TextMuted, fontSize = 13.sp)
            } else {
                groups.forEachIndexed { groupIndex, group ->
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val isSelected = group.isTrackSelected(trackIndex)
                        val lang = format.language ?: "افتراضي (${trackIndex + 1})"
                        val label = format.label ?: "مسار ${trackIndex + 1}"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    player.trackSelectionParameters = player.trackSelectionParameters
                                        .buildUpon()
                                        .setTrackTypeDisabled(trackType, false)
                                        .setOverrideForType(
                                            TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                                        )
                                        .build()
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "$label ($lang)",
                                color = if (isSelected) GoldyColors.Cyan else GoldyColors.TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "إغلاق",
                    color = GoldyColors.CyanLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
