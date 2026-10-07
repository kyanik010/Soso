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
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setInitialScale(0)
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
                var root = document.documentElement;
                var body = document.body;

                if (!root || !body) {
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

                // Do not depend on dvh or viewport CSS math
                // in the legacy WebView used by the verifier.
                root.style.setProperty('--u', u + 'px', 'important');
                
                // ============================================
                // ================================================
                // PHASE 3: CSS Orientation Compatibility Layer
                // ================================================
                // Mirrors goldy.html @media (orientation: portrait) rules.
                // All values are explicit pixels (no calc(var(--u))).
                // Uses `order` for correct visual stacking.

                (function() {
                    var innerW = window.innerWidth;
                    var innerH = window.innerHeight;
                    var actualPortrait = innerH > innerW;
                    var mqPortrait = window.matchMedia('(orientation: portrait)').matches;

                    var html = document.documentElement;
                    html.classList.toggle('goldy-force-portrait', actualPortrait);
                    html.classList.toggle('goldy-force-landscape', !actualPortrait);

                    var u = actualPortrait
                        ? (innerW / 400)
                        : Math.min(innerW / 739, innerH / 415);

                    function px(n) { return (n * u) + 'px'; }

                    if (!document.getElementById('goldy-orientation-compat')) {
                        var style = document.createElement('style');
                        style.id = 'goldy-orientation-compat';
                        style.textContent = [
                            'html.goldy-force-portrait,',
                            'html.goldy-force-portrait body {',
                            '  width: ' + innerW + 'px !important;',
                            '  height: ' + innerH + 'px !important;',
                            '  margin: 0 !important;',
                            '  padding: 0 !important;',
                            '  overflow: hidden !important;',
                            '  background: #020617 !important;',
                            '}',

                            'html.goldy-force-portrait .stage {',
                            '  width: ' + innerW + 'px !important;',
                            '  height: ' + innerH + 'px !important;',
                            '  max-height: ' + innerH + 'px !important;',
                            '  top: 0 !important;',
                            '  left: 0 !important;',
                            '  transform: none !important;',
                            '  margin: 0 !important;',
                            '  display: flex !important;',
                            '  flex-direction: column !important;',
                            '  align-items: center !important;',
                            '  justify-content: flex-start !important;',
                            '  padding: ' + px(12) + ' ' + px(14) + ' ' + px(10) + ' !important;',
                            '  overflow: hidden !important;',
                            '  box-sizing: border-box !important;',
                            '}',
                            'html.goldy-force-portrait .stage > * {',
                            '  position: relative !important;',
                            '  left: auto !important;',
                            '  top: auto !important;',
                            '  transform: none !important;',
                            '}',

                            'html.goldy-force-portrait .landscape-logo {',
                            '  display: flex !important;',
                            '  position: absolute !important;',
                            '  left: 50% !important;',
                            '  top: ' + px(10) + ' !important;',
                            '  transform: translateX(-50%) !important;',
                            '  width: ' + px(200) + ' !important;',
                            '  height: ' + px(135) + ' !important;',
                            '  align-items: center !important;',
                            '  justify-content: center !important;',
                            '  z-index: 100 !important;',
                            '  pointer-events: none !important;',
                            '  order: 1 !important;',
                            '}',
                            'html.goldy-force-portrait .landscape-logo img {',
                            '  display: block !important;',
                            '  width: auto !important;',
                            '  height: auto !important;',
                            '  max-width: 100% !important;',
                            '  max-height: 100% !important;',
                            '  object-fit: contain !important;',
                            '}',

                            'html.goldy-force-portrait .account-info-landscape {',
                            '  display: none !important;',
                            '}',
                            'html.goldy-force-portrait h1,',
                            'html.goldy-force-portrait .cat,',
                            'html.goldy-force-portrait .tl,',
                            'html.goldy-force-portrait .br,',
                            'html.goldy-force-portrait .feat,',
                            'html.goldy-force-portrait .info {',
                            '  display: none !important;',
                            '}',

                            'html.goldy-force-portrait .rail {',
                            '  width: 100% !important;',
                            '  margin-top: ' + px(20) + ' !important;',
                            '  display: flex !important;',
                            '  flex-direction: column !important;',
                            '  align-items: center !important;',
                            '  left: auto !important;',
                            '  top: auto !important;',
                            '  max-width: none !important;',
                            '  padding: 0 !important;',
                            '  order: 2 !important;',
                            '}',

                            'html.goldy-force-portrait .row {',
                            '  width: 100% !important;',
                            '  display: flex !important;',
                            '  justify-content: center !important;',
                            '  gap: ' + px(11) + ' !important;',
                            '  padding: ' + px(5) + ' !important;',
                            '  overflow: visible !important;',
                            '  margin-top: 0 !important;',
                            '}',
                            'html.goldy-force-portrait .row + .row {',
                            '  display: flex !important;',
                            '  margin-top: ' + px(13) + ' !important;',
                            '}',

                            'html.goldy-force-portrait .poster {',
                            '  width: ' + px(105) + ' !important;',
                            '  height: ' + px(172) + ' !important;',
                            '  flex: none !important;',
                            '}',
                            'html.goldy-force-portrait .poster img {',
                            '  width: 100% !important;',
                            '  height: 100% !important;',
                            '  object-fit: cover !important;',
                            '}',

                            'html.goldy-force-portrait .account-info-portrait {',
                            '  display: block !important;',
                            '  width: 100% !important;',
                            '  margin-top: ' + px(2) + ' !important;',
                            '  margin-bottom: ' + px(5) + ' !important;',
                            '  padding: ' + px(7) + ' ' + px(10) + ' !important;',
                            '  transform: none !important;',
                            '  order: 3 !important;',
                            '  border: 1px solid rgba(125, 211, 252, 0.16) !important;',
                            '  border-radius: ' + px(8) + ' !important;',
                            '  background: rgba(2, 15, 31, 0.58) !important;',
                            '  box-shadow: 0 ' + px(4) + ' ' + px(16) + ' rgba(0, 0, 0, 0.18) !important;',
                            '  backdrop-filter: blur(8px) !important;',
                            '  -webkit-backdrop-filter: blur(8px) !important;',
                            '  box-sizing: border-box !important;',
                            '  z-index: 5 !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-title {',
                            '  display: flex !important;',
                            '  align-items: center !important;',
                            '  gap: ' + px(6) + ' !important;',
                            '  margin-bottom: ' + px(5) + ' !important;',
                            '  color: #e0f2fe !important;',
                            '  font: 600 ' + px(10.5) + ' / 1.2 Roboto, Arial, sans-serif !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-user-icon {',
                            '  display: flex !important;',
                            '  align-items: center !important;',
                            '  justify-content: center !important;',
                            '  width: ' + px(19) + ' !important;',
                            '  height: ' + px(19) + ' !important;',
                            '  max-width: ' + px(19) + ' !important;',
                            '  max-height: ' + px(19) + ' !important;',
                            '  flex: none !important;',
                            '  border-radius: 50% !important;',
                            '  color: #7dd3fc !important;',
                            '  background: rgba(0, 212, 255, 0.07) !important;',
                            '  border: 1px solid rgba(0, 212, 255, 0.14) !important;',
                            '  overflow: hidden !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-user-icon svg {',
                            '  width: ' + px(12) + ' !important;',
                            '  height: ' + px(12) + ' !important;',
                            '  max-width: ' + px(12) + ' !important;',
                            '  max-height: ' + px(12) + ' !important;',
                            '  fill: none !important;',
                            '  stroke: currentColor !important;',
                            '  stroke-width: 1.6 !important;',
                            '  stroke-linecap: round !important;',
                            '  stroke-linejoin: round !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-line {',
                            '  display: flex !important;',
                            '  align-items: center !important;',
                            '  justify-content: space-between !important;',
                            '  gap: ' + px(8) + ' !important;',
                            '  min-height: ' + px(14) + ' !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-label {',
                            '  color: rgba(224, 242, 254, 0.48) !important;',
                            '  font: 400 ' + px(7.5) + ' / 1.2 Roboto, Arial, sans-serif !important;',
                            '  white-space: nowrap !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-value {',
                            '  color: #e0f2fe !important;',
                            '  font: 500 ' + px(8) + ' / 1.2 Roboto, Arial, sans-serif !important;',
                            '  white-space: nowrap !important;',
                            '  text-align: right !important;',
                            '}',
                            'html.goldy-force-portrait .account-info-portrait .account-expiry {',
                            '  color: #7dd3fc !important;',
                            '}',

                            'html.goldy-force-portrait .panel {',
                            '  width: 100% !important;',
                            '  height: auto !important;',
                            '  margin-top: auto !important;',
                            '  padding: ' + px(8) + ' !important;',
                            '  display: grid !important;',
                            '  grid-template-columns: 1fr 1fr !important;',
                            '  gap: ' + px(8) + ' !important;',
                            '  left: auto !important;',
                            '  top: auto !important;',
                            '  transform: translateY(' + px(-10) + ') !important;',
                            '  order: 4 !important;',
                            '}',
                            'html.goldy-force-portrait .btn,',
                            'html.goldy-force-portrait .btn.act {',
                            '  width: 100% !important;',
                            '  height: ' + px(52) + ' !important;',
                            '  justify-content: center !important;',
                            '  padding: 0 !important;',
                            '  gap: ' + px(8) + ' !important;',
                            '  font-size: ' + px(14) + ' !important;',
                            '  flex: none !important;',
                            '}',
                            'html.goldy-force-portrait .btn svg {',
                            '  width: ' + px(26) + ' !important;',
                            '  height: ' + px(27) + ' !important;',
                            '}',

                            'html.goldy-force-landscape .account-info-portrait {',
                            '  display: none !important;',
                            '}'
                        ].join('\n');
                        document.head.appendChild(style);
                    }

                    console.log('DIAG_ORIENTATION_COMPAT: w=' + innerW + ' h=' + innerH +
                        ' portrait=' + actualPortrait +
                        ' mqPortrait=' + mqPortrait + ' u=' + u);
                })();
                root.style.setProperty('width', '100%', 'important');
                root.style.setProperty('height', h + 'px', 'important');
                body.style.setProperty('width', '100%', 'important');
                body.style.setProperty('height', h + 'px', 'important');

                var stage = document.querySelector('.stage');

                console.log(
                    'DIAG_VIEWPORT: w=' + w +
                    ' h=' + h +
                    ' u=' + u +
                    ' portrait=' + isPortrait
                );

                if (!stage) {
                    console.log('DIAG_STAGE_NOT_FOUND');
                    return false;
                }

                // Do not override .stage dimensions here.
                // goldy.html remains the single source of truth for
                // portrait/landscape stage sizing. The WebView viewport
                // settings above are responsible for giving its CSS the
                // correct orientation context.

                void root.offsetHeight;
                void body.offsetHeight;
                void stage.offsetHeight;

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

                var panel = document.querySelector('.panel');
                if (panel) {
                    var panelCS = window.getComputedStyle(panel);
                    console.log(
                        'DIAG_PANEL: offsetW=' + panel.offsetWidth +
                        ' offsetH=' + panel.offsetHeight +
                        ' display=' + panelCS.display +
                        ' visibility=' + panelCS.visibility +
                        ' opacity=' + panelCS.opacity
                    );
                }

                var account = document.querySelector('.account-info-portrait');
                if (account) {
                    var accountCS = window.getComputedStyle(account);
                    console.log(
                        'DIAG_ACCOUNT: offsetW=' + account.offsetWidth +
                        ' offsetH=' + account.offsetHeight +
                        ' display=' + accountCS.display +
                        ' visibility=' + accountCS.visibility +
                        ' opacity=' + accountCS.opacity +
                        ' transform=' + accountCS.transform
                    );
                }

                var logo = document.querySelector('.landscape-logo');
                if (logo) {
                    var logoCS = window.getComputedStyle(logo);
                    console.log(
                        'DIAG_LOGO: offsetW=' + logo.offsetWidth +
                        ' offsetH=' + logo.offsetHeight +
                        ' display=' + logoCS.display +
                        ' visibility=' + logoCS.visibility +
                        ' opacity=' + logoCS.opacity
                    );
                }

                console.log(
                    'DIAG_ROOT_U: ' +
                    root.style.getPropertyValue('--u')
                );
                console.log(
                    'DIAG_MEDIA: portrait=' +
                    window.matchMedia('(orientation: portrait)').matches +
                    ' landscape=' +
                    window.matchMedia('(orientation: landscape)').matches
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

            function recalcAfterResize() {
                setTimeout(function() {
                    calcU();
                }, 0);
            }

            if (!window.__GOLDY_LAYOUT_LISTENERS_BOUND__) {
                window.__GOLDY_LAYOUT_LISTENERS_BOUND__ = true;
                window.addEventListener(
                    'resize',
                    recalcAfterResize
                );
                window.addEventListener(
                    'orientationchange',
                    recalcAfterResize
                );
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
