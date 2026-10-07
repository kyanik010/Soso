package com.lumora.iptv.ui.live

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.data.model.Channel
import com.lumora.iptv.ui.components.GoldyChannelCard
import com.lumora.iptv.ui.theme.GoldyColors

@Composable
fun LiveScreen(
    repository: IptvRepository,
    onBack: () -> Unit,
    onPlayChannel: (Channel) -> Unit
) {
    BackHandler { onBack() }

    val config = LocalConfiguration.current
    val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
    val columns = if (isPortrait) 2 else 3

    val categories by repository.getLiveCategories().collectAsState(initial = emptyList())
    var selectedCatId by remember { mutableStateOf("all") }

    val channels by (if (selectedCatId == "all") repository.getAllLiveChannels()
    else repository.getChannelsByCategory(selectedCatId)).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
            .padding(16.dp)
            .testTag("live_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldyColors.PanelDark1)
                    .testTag("live_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldyColors.CyanLight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "البث المباشر (LIVE TV)",
                color = GoldyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${channels.size} قناة",
                color = GoldyColors.CyanLight,
                fontSize = 13.sp
            )
        }

        // Categories
        if (categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                item {
                    val isSel = selectedCatId == "all"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) GoldyColors.BtnActive1 else GoldyColors.PanelDark1)
                            .border(1.dp, if (isSel) GoldyColors.Cyan else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable { selectedCatId = "all" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .focusable()
                    ) {
                        Text(
                            text = "جميع القنوات",
                            color = if (isSel) Color.White else GoldyColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(categories) { cat ->
                    val isSel = selectedCatId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) GoldyColors.BtnActive1 else GoldyColors.PanelDark1)
                            .border(1.dp, if (isSel) GoldyColors.Cyan else Color.Transparent, RoundedCornerShape(20.dp))
                            .clickable { selectedCatId = cat.id }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .focusable()
                    ) {
                        Text(
                            text = cat.name,
                            color = if (isSel) Color.White else GoldyColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Channels Grid (Section 29: Portrait 2, Landscape 3)
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد قنوات متاحة",
                    color = GoldyColors.TextMuted,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
                    GoldyChannelCard(
                        channel = channel,
                        onClick = { onPlayChannel(channel) }
                    )
                }
            }
        }
    }
}
