package com.lumora.iptv.ui.navigation

import java.net.URLEncoder

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
    object Player : Screen("player/{title}/{url}/{mediaType}") {
        fun createRoute(title: String, url: String, mediaType: String): String {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encUrl = URLEncoder.encode(url, "UTF-8")
            return "player/$encTitle/$encUrl/$mediaType"
        }
    }
}
