package com.lumora.iptv.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.lumora.iptv.bridge.GoldyBridge
import com.lumora.iptv.data.iptv.IptvRepository
import com.lumora.iptv.data.model.Movie
import com.lumora.iptv.ui.theme.GoldyColors
import com.lumora.iptv.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoldyHomeScreen(
    repository: IptvRepository,
    onNavigate: (String) -> Unit,
    onOpenMovie: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val movies by repository.getAllMovies().collectAsState(initial = emptyList())

    // Hold reference to WebView for evaluation
    var webViewRef: WebView? = remember { null }

    val bridge = remember {
        GoldyBridge(
            onOpenAction = onOpenMovie,
            onNavigateAction = onNavigate,
            onMenuAction = onOpenMenu,
            onAppsAction = onOpenApps
        )
    }

    LaunchedEffect(movies) {
        webViewRef?.let { wv ->
            sendMoviesToWebView(wv, movies)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GoldyColors.BgDark)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        mediaPlaybackRequiresUserGesture = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    setBackgroundColor(Color.parseColor("#020617"))
                    overScrollMode = View.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    isLongClickable = false
                    isHapticFeedbackEnabled = true
                    isFocusable = true
                    isFocusableInTouchMode = true

                    val assetLoader = WebViewAssetLoader.Builder()
                        .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(ctx))
                        .build()

                    webViewClient = object : WebViewClientCompat() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            /**
                             * TEMPORARY ASSET INTERCEPTOR
                             *
                             * 40.png -> goldy_logo.png
                             *
                             * goldy.html is the source of truth and references 40.png.
                             * A real 40.png asset should eventually replace this interceptor.
                             */
                            val urlStr = request.url.toString()
                            if (urlStr.endsWith("/40.png") || urlStr.endsWith("40.png")) {
                                try {
                                    val stream = ctx.assets.open("goldy_logo.png")
                                    return WebResourceResponse("image/png", null, stream)
                                } catch (e: Exception) {
                                    AppLogger.d("GoldyWebView", "Interception fallback for 40.png")
                                }
                            }
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.let {
                                sendMoviesToWebView(it, movies)
                                it.requestFocus()
                            }
                        }
                    }

                    addJavascriptInterface(bridge, "Android")
                    loadUrl("https://appassets.androidplatform.net/goldy.html")
                    requestFocus()
                }
            },
            update = { webView ->
                webViewRef = webView
                if (movies.isNotEmpty()) {
                    sendMoviesToWebView(webView, movies)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }
}

/**
 * Pushes up to 30 movies to Goldy HTML UI via window.IPTV.setMovies
 */
fun sendMoviesToWebView(webView: WebView, movies: List<Movie>) {
    Handler(Looper.getMainLooper()).post {
        try {
            val json = JSONArray()
            movies.take(30).forEach { movie ->
                json.put(
                    JSONObject().apply {
                        put("id", movie.id)
                        put("title", movie.title)
                        put("img", movie.posterUrl ?: "")
                    }
                )
            }
            webView.evaluateJavascript("if (window.IPTV && typeof window.IPTV.setMovies === 'function') { window.IPTV.setMovies($json); }", null)
            AppLogger.d("GoldyWebView", "Pushed ${minOf(movies.size, 30)} movies to Goldy HTML")
        } catch (e: Exception) {
            AppLogger.e("GoldyWebView", "Failed to evaluate Javascript setMovies", e)
        }
    }
}
