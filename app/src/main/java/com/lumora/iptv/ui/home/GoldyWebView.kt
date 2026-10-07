package com.lumora.iptv.ui.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebResourceErrorCompat
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun initializeGoldyPage(view: WebView) {
        val finishInitialization: () -> Unit = {
            injectU(view)
            sendMoviesToWebView(view, movies)
            view.post {
                view.requestLayout()
                view.invalidate()
                view.requestFocus()
                AppLogger.d(
                    "GoldyWebView",
                    "Native bounds: x=" + view.x +
                        " y=" + view.y +
                        " w=" + view.width +
                        " h=" + view.height +
                        " root=" + view.rootView.width + "x" + view.rootView.height
                )
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
            WebViewCompat.postVisualStateCallback(
                view,
                System.nanoTime(),
                object : WebViewCompat.VisualStateCallback {
                    override fun onComplete(requestId: Long) {
                        finishInitialization()
                    }
                }
            )
        } else {
            view.postDelayed(finishInitialization, 80L)
        }
    }

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
                        // Let the HTML viewport meta tag be the single source of truth.
                        // Wide/overview scaling can create a CSS viewport that differs from
                        // window.innerWidth/innerHeight on the legacy verifier WebView.
                        useWideViewPort = false
                        loadWithOverviewMode = false
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
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            // Polyfill for replaceChildren() on older WebView versions
                            view?.evaluateJavascript("""
                                (function() {
                                    if (!Element.prototype.replaceChildren) {
                                        Element.prototype.replaceChildren = function() {
                                            while (this.firstChild) {
                                                this.removeChild(this.firstChild);
                                            }
                                            for (var i = 0; i < arguments.length; i++) {
                                                var node = arguments[i];
                                                if (typeof node === 'string') {
                                                    this.appendChild(document.createTextNode(node));
                                                } else if (node) {
                                                    this.appendChild(node);
                                                }
                                            }
                                        };
                                        console.log('POLYFILL: replaceChildren installed');
                                    }
                                })();
                            """.trimIndent(), null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.let { initializeGoldyPage(it) }
                        }

                        override fun onPageCommitVisible(view: WebView, url: String) {
                            super.onPageCommitVisible(view, url)
                            initializeGoldyPage(view)
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceErrorCompat
                        ) {
                            super.onReceivedError(view, request, error)
                            AppLogger.e(
                                "GoldyWebView",
                                "onReceivedError: ${request?.url} - " +
                                    "code=${error?.errorCode} desc=${error?.description}"
                            )
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            AppLogger.e(
                                "GoldyWebView",
                                "onReceivedHttpError: ${request?.url} - " +
                                    "status=${errorResponse?.statusCode}"
                            )
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            AppLogger.d(
                                "GoldyConsole",
                                "${consoleMessage.messageLevel()}: " +
                                    "${consoleMessage.message()} " +
                                    "(@${consoleMessage.lineNumber()})"
                            )
                            return true
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
 * Injects a computed --u value into the WebView.
 *
 * goldy.html defines:
fun injectU(webView: WebView) {
    val js = """
        (function() {
            function calcU() {
                var root = document.documentElement;

                if (!root || !document.body) {
                    console.log('DIAG_DOM_NOT_READY');
                    return false;
                }

                var w = Math.max(
                    1,
                    window.innerWidth || root.clientWidth || 1
                );
                var h = Math.max(
                    1,
                    window.innerHeight || root.clientHeight || 1
                );

                var isPortrait = h > w;
                var u = isPortrait
                    ? (w / 400)
                    : Math.min(w / 739, h / 415);

                // Keep --u computed in JavaScript for legacy WebView compatibility.
                // Orientation/layout itself is controlled by goldy.html classes.
                root.style.setProperty('--u', u + 'px', 'important');

                if (typeof window.applyOrientation === 'function') {
                    window.applyOrientation();
                }

                console.log(
                    'DIAG_VIEWPORT: w=' + w +
                    ' h=' + h +
                    ' u=' + u +
                    ' portrait=' + isPortrait
                );

                console.log(
                    'DIAG_ORIENTATION_CLASS: portrait=' +
                    root.classList.contains('goldy-portrait') +
                    ' landscape=' +
                    root.classList.contains('goldy-landscape') +
                    ' smallPortrait=' +
                    root.classList.contains('goldy-small-portrait') +
                    ' shortLandscape=' +
                    root.classList.contains('goldy-short-landscape')
                );

                console.log(
                    'DIAG_ROOT_U: ' +
                    root.style.getPropertyValue('--u')
                );

                var stage = document.querySelector('.stage');

                if (!stage) {
                    console.log('DIAG_STAGE_NOT_FOUND');
                    return false;
                }

                var stageCS = window.getComputedStyle(stage);

                console.log(
                    'DIAG_STAGE: offsetW=' + stage.offsetWidth +
                    ' offsetH=' + stage.offsetHeight +
                    ' computedW=' + stageCS.width +
                    ' computedH=' + stageCS.height +
                    ' display=' + stageCS.display +
                    ' visibility=' + stageCS.visibility +
                    ' opacity=' + stageCS.opacity +
                    ' position=' + stageCS.position +
                    ' top=' + stageCS.top +
                    ' left=' + stageCS.left +
                    ' transform=' + stageCS.transform
                );

                console.log('DIAG_LAYOUT_READY');
                return true;
            }

            function ensureLayout(attempt) {
                if (calcU()) {
                    return;
                }

                if (attempt < 40) {
                    setTimeout(function() {
                        ensureLayout(attempt + 1);
                    }, 50);
                    return;
                }

                console.log('DIAG_LAYOUT_FAILED_AFTER_RETRY');
            }

            ensureLayout(0);
        })();
    """.trimIndent()

    webView.evaluateJavascript(js, null)
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
