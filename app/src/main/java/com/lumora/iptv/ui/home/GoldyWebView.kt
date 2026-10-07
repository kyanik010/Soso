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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
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
                            view?.let {
                                injectU(it)
                                sendMoviesToWebView(it, movies)
                                it.requestFocus()
                            }
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
 *   --u: min(calc(100vw / 739), calc(100dvh / 415));
 *
 * But 100dvh is not supported in all Android WebView versions,
 * which makes --u invalid and collapses .stage to 0x0.
 *
 * This function computes --u in pixels using JavaScript and sets
 * it as an inline style on :root, without modifying goldy.html.
 *
 * It also listens for resize/orientationchange and recomputes.
 */
fun injectU(webView: WebView) {
    val js = """
        (function() {
            function calcU() {
                var w = window.innerWidth;
                var h = window.innerHeight;
                var u;
                if (h > w) {
                    // Portrait
                    u = w / 400;
                } else {
                    // Landscape
                    u = Math.min(w / 739, h / 415);
                }
                document.documentElement.style.setProperty('--u', u + 'px');

                // Force reflow to make CSS recalculate dependent properties
                void document.documentElement.offsetHeight;
                if (document.body) {
                    void document.body.offsetHeight;
                }

                // Fix: bypass calc(739 * var(--u)) by setting explicit width/height.
                // 100dvh may be unsupported in older WebView, making --u invalid at computed-value time.
                var stage = document.querySelector('.stage');
                if (stage) {
                    var stageW = 739 * u;
                    var stageH = 415 * u;
                    stage.style.setProperty('width', stageW + 'px', 'important');
                    stage.style.setProperty('height', stageH + 'px', 'important');
                    void stage.offsetHeight;

                    // DIAGNOSTIC: log actual rendered sizes
                    var stageCS = window.getComputedStyle(stage);
                    console.log('DIAG_STAGE: offsetW=' + stage.offsetWidth +
                        ' offsetH=' + stage.offsetHeight +
                        ' clientW=' + stage.clientWidth +
                        ' clientH=' + stage.clientHeight +
                        ' computedW=' + stageCS.width +
                        ' computedH=' + stageCS.height +
                        ' display=' + stageCS.display +
                        ' visibility=' + stageCS.visibility +
                        ' opacity=' + stageCS.opacity +
                        ' zIndex=' + stageCS.zIndex +
                        ' position=' + stageCS.position +
                        ' overflow=' + stageCS.overflow);

                    var panel = document.querySelector('.panel');
                    if (panel) {
                        var panelCS = window.getComputedStyle(panel);
                        console.log('DIAG_PANEL: offsetW=' + panel.offsetWidth +
                            ' offsetH=' + panel.offsetHeight +
                            ' display=' + panelCS.display +
                            ' visibility=' + panelCS.visibility +
                            ' opacity=' + panelCS.opacity +
                            ' zIndex=' + panelCS.zIndex +
                            ' position=' + panelCS.position);
                    }

                    var account = document.querySelector('.account-info-portrait');
                    if (account) {
                        var accountCS = window.getComputedStyle(account);
                        console.log('DIAG_ACCOUNT: offsetW=' + account.offsetWidth +
                            ' offsetH=' + account.offsetHeight +
                            ' display=' + accountCS.display +
                            ' visibility=' + accountCS.visibility +
                            ' opacity=' + accountCS.opacity +
                            ' transform=' + accountCS.transform);
                    }

                    var logo = document.querySelector('.landscape-logo');
                    if (logo) {
                        var logoCS = window.getComputedStyle(logo);
                        console.log('DIAG_LOGO: offsetW=' + logo.offsetWidth +
                            ' offsetH=' + logo.offsetHeight +
                            ' display=' + logoCS.display +
                            ' visibility=' + logoCS.visibility +
                            ' opacity=' + logoCS.opacity +
                            ' position=' + logoCS.position +
                            ' top=' + logoCS.top +
                            ' left=' + logoCS.left);
                    }

                    // Count body children and their z-index
                    var bodyChildren = document.body.children;
                    console.log('DIAG_BODY: count=' + bodyChildren.length);
                    for (var i = 0; i < bodyChildren.length; i++) {
                        var c = bodyChildren[i];
                        var cCS = window.getComputedStyle(c);
                        console.log('DIAG_BODY_' + i + ': tag=' + c.tagName +
                            ' class=' + c.className +
                            ' zIndex=' + cCS.zIndex +
                            ' position=' + cCS.position +
                            ' display=' + cCS.display);
                    }

                    // Also log the value of --u currently in :root
                    console.log('DIAG_ROOT_U: ' + getComputedStyle(document.documentElement).getPropertyValue('--u'));

                    // Log matchMedia result
                    console.log('DIAG_MEDIA: portrait=' + window.matchMedia('(orientation: portrait)').matches +
                        ' landscape=' + window.matchMedia('(orientation: landscape)').matches);
                }

                console.log('INJECT_U_DEBUG: w=' + w + ' h=' + h + ' u=' + u + ' (reflow+dvh fix)');
                return u;
            }
            calcU();
            window.addEventListener('resize', calcU);
            window.addEventListener('orientationchange', calcU);
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
