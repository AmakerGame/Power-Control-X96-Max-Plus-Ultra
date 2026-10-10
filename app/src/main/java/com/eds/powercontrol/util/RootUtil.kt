package com.eds.powercontrol.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Robust Root Utility for Android TV (SlimBoxTV / Amlogic).
 * Executes su commands cleanly with proper stream draining to prevent pipe deadlocks.
 */
object RootUtil {
    private const val TAG = "RootUtil"

    private val SU_BINARY_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    fun isSuBinaryPresent(): Boolean {
        for (path in SU_BINARY_PATHS) {
            if (File(path).exists()) return true
        }
        return false
    }

    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readLine() ?: ""
            process.errorStream.bufferedReader().readLines()
            val exited = process.waitFor(3, TimeUnit.SECONDS)
            if (exited && (process.exitValue() == 0 || output.contains("uid=0"))) {
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Root check failed: ${e.message}")
        }
        return@withContext false
    }

    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readLine() ?: ""
            process.errorStream.bufferedReader().readLines()
            val exited = process.waitFor(10, TimeUnit.SECONDS)
            return@withContext exited && (process.exitValue() == 0 || output.contains("uid=0"))
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting root access", e)
        }
        return@withContext false
    }

    /**
     * Executes a command string as root.
     * Starts background reader threads on stdout/stderr to prevent OS pipe buffer overflow.
     */
    fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            // Always drain stdout and stderr so the process never blocks on full pipe
            Thread {
                try { process.inputStream.bufferedReader().readLines() } catch (ignored: Exception) {}
            }.start()
            Thread {
                try { process.errorStream.bufferedReader().readLines() } catch (ignored: Exception) {}
            }.start()
            val exited = process.waitFor(3, TimeUnit.SECONDS)
            exited && (process.exitValue() == 0)
        } catch (e: Exception) {
            Log.e(TAG, "Root command execution error: $command", e)
            false
        }
    }

    /**
     * Instantly kills com.qstar.powerui and launches the target package.
     */
    fun killPowerUiAndLaunch(context: Context, targetPackage: String): Boolean {
        if (targetPackage.isBlank()) return false
        val cmd = "am force-stop com.qstar.powerui; kill -9 \$(pidof com.qstar.powerui) 2>/dev/null; am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $targetPackage 2>/dev/null || am start $targetPackage 2>/dev/null"
        Log.i(TAG, "killPowerUiAndLaunch executing for $targetPackage")
        val rootOk = executeRootCommand(cmd)
        // Also call Android launch intent from context as complement
        AppHelper.launchApp(context, targetPackage)
        return rootOk
    }

    /**
     * Silently grants PACKAGE_USAGE_STATS permission via root.
     */
    suspend fun grantPermissionsSilently(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pkg = context.packageName
        executeRootCommand("pm grant $pkg android.permission.PACKAGE_USAGE_STATS")
    }
}
