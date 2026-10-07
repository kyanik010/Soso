package com.lumora.iptv.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.lumora.iptv.util.AppLogger
import org.json.JSONObject

class GoldyBridge(
    private val onOpenAction: (String) -> Unit,
    private val onNavigateAction: (String) -> Unit,
    private val onMenuAction: () -> Unit,
    private val onAppsAction: () -> Unit
) {

    @JavascriptInterface
    fun onOpen(movieJson: String) {
        Handler(Looper.getMainLooper()).post {
            try {
                val json = JSONObject(movieJson)
                val id = json.optString("id")
                if (id.isBlank()) return@post
                AppLogger.d("GoldyBridge", "onOpen called with movie id: $id")
                onOpenAction(id)
            } catch (e: Exception) {
                AppLogger.e("GoldyBridge", "Invalid movie payload from WebView", e)
            }
        }
    }

    @JavascriptInterface
    fun onNavigate(key: String) {
        // goldy.html sends JSON.stringify(key) which produces "\"movies\""
        // We sanitize by stripping surrounding quotes.
        val sanitized = key.trim().trim('"')

        val allowed = setOf("live", "movies", "series", "settings")
        if (sanitized !in allowed) {
            AppLogger.w("GoldyBridge", "Blocked navigation to unpermitted destination: $sanitized")
            return
        }

        Handler(Looper.getMainLooper()).post {
            AppLogger.d("GoldyBridge", "Navigating to: $sanitized")
            onNavigateAction(sanitized)
        }
    }

    @JavascriptInterface
    fun onMenu(payload: String) {
        // goldy.html always sends JSON.stringify({}) = "{}" for onMenu.
        // We ignore the payload; it exists only to satisfy the bridge signature.
        Handler(Looper.getMainLooper()).post {
            AppLogger.d("GoldyBridge", "onMenu triggered")
            onMenuAction()
        }
    }

    @JavascriptInterface
    fun onApps(payload: String) {
        // goldy.html always sends JSON.stringify({}) = "{}" for onApps.
        // We ignore the payload; it exists only to satisfy the bridge signature.
        Handler(Looper.getMainLooper()).post {
            AppLogger.d("GoldyBridge", "onApps triggered")
            onAppsAction()
        }
    }
}
