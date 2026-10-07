package com.lumora.iptv.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object GoldyHome : Screen("goldy_home")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object SeriesDetails : Screen("series_details/{seriesId}") {
        fun createRoute(seriesId: Int) = "series_details/$seriesId"
    }
    object Live : Screen("live")
    object Settings : Screen("settings")
    object Account : Screen("account")
    object Player : Screen("player/{sourceId}/{mediaType}/{title}") {
        const val SOURCE_LIVE = "live"
        const val SOURCE_VOD = "vod"
        const val SOURCE_EPISODE = "episode"

        fun createRoute(sourceId: String, mediaType: String, title: String): String {
            val encTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "player/$sourceId/$mediaType/$encTitle"
        }
    }
}
