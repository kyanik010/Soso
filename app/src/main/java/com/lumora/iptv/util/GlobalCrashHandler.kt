package com.lumora.iptv.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Global Crash Handler for 5,000+ users.
 * Catches uncaught exceptions, persists crash diagnostics locally for debugging,
 * and delegates to system default handler to ensure proper lifecycle teardown.
 */
class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            AppLogger.e("CrashHandler", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}")

            // Persist crash log locally for error reporting / user diagnosis
            val crashFile = File(context.filesDir, "last_crash.log")
            crashFile.writeText("Thread: ${thread.name}\nTime: ${System.currentTimeMillis()}\nError: $stackTrace")
        } catch (ignored: Exception) {
            // Failsafe: Never crash inside the crash handler
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context) {
            val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context, currentDefault))
        }
    }
}
