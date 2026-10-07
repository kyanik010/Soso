package com.lumora.iptv.util

import android.util.Log
import com.lumora.iptv.BuildConfig

/**
 * Production-Safe Logger.
 * Strict rules:
 * - Redacts passwords, tokens, sensitive credentials, and playback URLs with embedded user/pass.
 * - Suppresses debug/verbose logs in release builds (5,000+ real users).
 */
object AppLogger {
    private const val TAG_PREFIX = "LumoraIPTV_"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX$tag", sanitize(message))
        }
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX$tag", sanitize(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG_PREFIX$tag", sanitize(message), throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX$tag", sanitize(message), throwable)
    }

    /**
     * Sanitizes strings to prevent credential leaks in Logcat.
     */
    fun sanitize(raw: String): String {
        return raw
            // Mask password parameters in URLs or JSON
            .replace(Regex("password=([^&\\s]+)"), "password=***REDACTED***")
            .replace(Regex("\"password\"\\s*:\\s*\"[^\"]+\""), "\"password\":\"***REDACTED***\"")
            // Mask Xtream stream URLs: /live/user/pass/id.ts
            .replace(Regex("/(live|movie|series)/([^/]+)/([^/]+)/"), "/$1/$2/***REDACTED***/")
            // Mask auth tokens
            .replace(Regex("token=([^&\\s]+)"), "token=***REDACTED***")
    }
}
