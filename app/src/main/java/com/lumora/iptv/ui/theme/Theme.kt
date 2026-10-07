package com.lumora.iptv.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GoldyColors.Cyan,
    onPrimary = GoldyColors.BgDark,
    secondary = GoldyColors.CyanLight,
    onSecondary = GoldyColors.BgDark,
    background = GoldyColors.BgDark,
    onBackground = GoldyColors.TextPrimary,
    surface = GoldyColors.PanelDark1,
    onSurface = GoldyColors.TextPrimary
)

@Composable
fun rememberU(): Dp {
    val config = LocalConfiguration.current
    val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT

    return if (isPortrait) {
        (config.screenWidthDp / 400f).dp
    } else {
        minOf(
            config.screenWidthDp / 739f,
            config.screenHeightDp / 415f
        ).dp
    }
}

@Composable
fun LumoraIPTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
