package com.lumora.iptv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.ui.account.AccountScreen
import com.lumora.iptv.ui.home.GoldyHomeScreen
import com.lumora.iptv.ui.live.LiveScreen
import com.lumora.iptv.ui.movies.MoviesScreen
import com.lumora.iptv.ui.player.PlayerScreen
import com.lumora.iptv.ui.series.SeriesDetailsScreen
import com.lumora.iptv.ui.series.SeriesScreen
import com.lumora.iptv.ui.settings.SettingsScreen
import com.lumora.iptv.ui.splash.SplashScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    repository: IptvRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                repository = repository,
                onProceed = {
                    navController.navigate(Screen.GoldyHome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Goldy Home (WebView + Goldy HTML)
        composable(Screen.GoldyHome.route) {
            GoldyHomeScreen(
                repository = repository,
                onNavigate = { key ->
                    when (key) {
                        "live" -> navController.navigate(Screen.Live.route)
                        "movies" -> navController.navigate(Screen.Movies.route)
                        "series" -> navController.navigate(Screen.Series.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                    }
                },
                onOpenMovie = { id ->
                    scope.launch {
                        val streamId = id.toIntOrNull() ?: 0
                        val movie = repository.getMovieById(streamId)
                        if (movie != null && movie.streamUrl.isNotBlank()) {
                            navController.navigate(
                                Screen.Player.createRoute(movie.title, movie.streamUrl, "movie")
                            )
                        } else {
                            navController.navigate(Screen.Movies.route)
                        }
                    }
                },
                onOpenMenu = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenApps = {
                    navController.navigate(Screen.Account.route)
                }
            )
        }

        // 3. Movies
        composable(Screen.Movies.route) {
            MoviesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlayMovie = { movie ->
                    navController.navigate(
                        Screen.Player.createRoute(movie.title, movie.streamUrl, "movie")
                    )
                }
            )
        }

        // 4. Live TV
        composable(Screen.Live.route) {
            LiveScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlayChannel = { channel ->
                    navController.navigate(
                        Screen.Player.createRoute(channel.name, channel.streamUrl, "channel")
                    )
                }
            )
        }

        // 5. Series
        composable(Screen.Series.route) {
            SeriesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onSelectSeries = { series ->
                    val seriesId = series.id.toIntOrNull() ?: 0
                    navController.navigate(Screen.SeriesDetails.createRoute(seriesId))
                }
            )
        }

        // 6. Series Details
        composable(
            route = Screen.SeriesDetails.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.IntType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("seriesId") ?: 0
            SeriesDetailsScreen(
                seriesId = seriesId,
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlayEpisode = { ep ->
                    navController.navigate(
                        Screen.Player.createRoute(ep.title ?: "Episode ${ep.number}", ep.streamUrl, "episode")
                    )
                }
            )
        }

        // 7. Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onNavigateToAccount = { navController.navigate(Screen.Account.route) }
            )
        }

        // 8. Account
        composable(Screen.Account.route) {
            AccountScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.GoldyHome.route) { inclusive = false }
                    }
                }
            )
        }

        // 9. Player (Native Media3 / ExoPlayer)
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "channel"
            val title = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("title") ?: "",
                "UTF-8"
            )

            PlayerScreen(
                title = title,
                sourceId = sourceId,
                mediaType = mediaType,
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
