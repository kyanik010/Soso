package com.lumora.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumora.iptv.data.model.Channel
import com.lumora.iptv.data.model.Movie
import com.lumora.iptv.ui.theme.GoldyColors

@Composable
fun GoldyMovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "scale")

    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (isFocused) GoldyColors.Cyan else GoldyColors.Cyan.copy(alpha = 0.4f)
    val borderWidth = if (isFocused) 2.dp else 1.dp

    Column(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = shape,
                ambientColor = GoldyColors.Cyan,
                spotColor = GoldyColors.Cyan
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(GoldyColors.PosterGradient1, GoldyColors.PosterGradient2)
                )
            )
            .border(borderWidth, borderColor, shape)
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("movie_card_${movie.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .background(GoldyColors.PanelDark2)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = GoldyColors.Cyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Text(
            text = movie.title,
            color = if (isFocused) Color.White else GoldyColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun GoldyChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "channel_scale")

    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isFocused) GoldyColors.Cyan else GoldyColors.Cyan.copy(alpha = 0.3f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .scale(scale)
            .shadow(if (isFocused) 12.dp else 2.dp, shape, ambientColor = GoldyColors.Cyan)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (isFocused) listOf(GoldyColors.BtnActive2, GoldyColors.PanelDark1)
                    else listOf(GoldyColors.PanelDark1, GoldyColors.PanelDark2)
                )
            )
            .border(if (isFocused) 2.dp else 1.dp, borderColor, shape)
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("channel_card_${channel.id}")
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(GoldyColors.PanelDark2),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                color = GoldyColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // LIVE Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
