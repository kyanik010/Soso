package com.lumora.iptv.ui.series

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.data.model.Episode
import com.lumora.iptv.data.model.Series
import com.lumora.iptv.ui.theme.GoldyColors

@Composable
fun SeriesScreen(
    repository: IptvRepository,
    onBack: () -> Unit,
    onSelectSeries: (Series) -> Unit
) {
    BackHandler { onBack() }

    val config = LocalConfiguration.current
    val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
    val columns = if (isPortrait) 3 else 5

    val categories by repository.getSeriesCategories().collectAsState(initial = emptyList())
    var selectedCatId by remember { mutableStateOf("all") }

    val seriesList by (if (selectedCatId == "all") repository.getAllSeries()
    else repository.getSeriesByCategory(selectedCatId)).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
            .padding(16.dp)
            .testTag("series_screen")
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
                    .testTag("series_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldyColors.CyanLight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "المسلسلات (Series)",
                color = GoldyColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${seriesList.size} مسلسل",
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
                            text = "الكل",
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

        // Grid
        if (seriesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مسلسلات متاحة في هذا التصنيف",
                    color = GoldyColors.TextMuted,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(seriesList, key = { it.id }) { s ->
                    SeriesCard(series = s, onClick = { onSelectSeries(s) })
                }
            }
        }
    }
}

@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, label = "series_scale")
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = Modifier
            .scale(scale)
            .shadow(if (isFocused) 16.dp else 4.dp, shape, ambientColor = GoldyColors.Cyan)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(GoldyColors.PosterGradient1, GoldyColors.PosterGradient2))
            )
            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) GoldyColors.Cyan else GoldyColors.Cyan.copy(alpha = 0.4f), shape)
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .testTag("series_card_${series.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .background(GoldyColors.PanelDark2)
        ) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = series.title,
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
fun SeriesDetailsScreen(
    seriesId: Int,
    repository: IptvRepository,
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit
) {
    BackHandler { onBack() }

    var series by remember { mutableStateOf<Series?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    val episodes by repository.getEpisodes(seriesId, selectedSeason).collectAsState(initial = emptyList())

    androidx.compose.runtime.LaunchedEffect(seriesId) {
        series = repository.getSeriesById(seriesId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
            .padding(16.dp)
            .testTag("series_details_screen")
    ) {
        // Back
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldyColors.PanelDark1)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GoldyColors.CyanLight
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = series?.title ?: "تفاصيل المسلسل",
                color = GoldyColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GoldyColors.PanelDark1)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.67f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldyColors.PanelDark2)
            ) {
                AsyncImage(
                    model = series?.posterUrl,
                    contentDescription = series?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series?.title ?: "",
                    color = GoldyColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!series?.genre.isNullOrBlank()) {
                    Text(
                        text = "النوع: ${series?.genre}",
                        color = GoldyColors.CyanLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (!series?.plot.isNullOrBlank()) {
                    Text(
                        text = series?.plot ?: "",
                        color = GoldyColors.TextMuted,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Season Selector Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in 1..5) {
                val isSel = selectedSeason == s
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) GoldyColors.BtnActive1 else GoldyColors.PanelDark1)
                        .border(1.dp, if (isSel) GoldyColors.Cyan else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { selectedSeason = s }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .focusable()
                ) {
                    Text(
                        text = "الموسم $s",
                        color = if (isSel) Color.White else GoldyColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Episodes List (Section 27)
        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد حلقات مسجلة لهذا الموسم",
                    color = GoldyColors.TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(episodes, key = { it.id }) { ep ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldyColors.PanelDark1)
                            .border(1.dp, GoldyColors.Cyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable { onPlayEpisode(ep) }
                            .padding(12.dp)
                            .focusable()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(GoldyColors.BtnActive2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحلقة ${ep.number}: ${ep.title ?: ""}",
                                color = GoldyColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!ep.duration.isNullOrBlank()) {
                                Text(
                                    text = "المدة: ${ep.duration}",
                                    color = GoldyColors.CyanLight,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
