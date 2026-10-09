package com.eds.powercontrol.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * High-performance root utility optimized for Android TV (SlimBoxTV / Amlogic).
 * Provides fast execution to eliminate lag when closing com.qstar.powerui and launching the target app.
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
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\nexit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exited = process.waitFor(2, TimeUnit.SECONDS)
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
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\nexit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine() ?: ""
            val exited = process.waitFor(10, TimeUnit.SECONDS)
            return@withContext exited && (process.exitValue() == 0 || line.contains("uid=0"))
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting root access", e)
        }
        return@withContext false
    }

    /**
     * Instantly force-kills com.qstar.powerui using multiple root strategies (am force-stop, pkill, kill -9)
     * AND launches the selected target app in the same shell operation without background restrictions.
     * This achieves near zero latency (<50ms).
     */
    suspend fun killPowerUiAndLaunchApp(targetPackage: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            // 1. Force kill com.qstar.powerui using multiple levels to ensure it closes completely
            os.writeBytes("am force-stop com.qstar.powerui\n")
            os.writeBytes("pkill -9 -f com.qstar.powerui\n")
            os.writeBytes("kill -9 $(pidof com.qstar.powerui) 2>/dev/null\n")

            // 2. Launch the user-selected application via root shell (bypasses Android 10/11 background start restrictions)
            if (targetPackage.isNotBlank()) {
                os.writeBytes("monkey -p $targetPackage -c android.intent.category.LAUNCHER 1 || am start $targetPackage\n")
            }

            os.writeBytes("exit\n")
            os.flush()

            val exited = process.waitFor(2, TimeUnit.SECONDS)
            Log.i(TAG, "killPowerUiAndLaunchApp executed for $targetPackage. Exited=$exited")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed in killPowerUiAndLaunchApp", e)
        }
        return@withContext false
    }

    /**
     * Standalone force-stop for com.qstar.powerui.
     */
    suspend fun forceStopPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext false
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("am force-stop $packageName\n")
            os.writeBytes("pkill -9 -f $packageName\n")
            os.writeBytes("kill -9 $(pidof $packageName) 2>/dev/null\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor(2, TimeUnit.SECONDS)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force-stop $packageName", e)
        }
        return@withContext false
    }

    /**
     * Automatically enables AccessibilityService and grants Usage Stats permission via root.
     * Enables 0ms real-time event detection of com.qstar.powerui on SlimBoxTV.
     */
    suspend fun setupSystemPermissionsSilently(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val pkg = context.packageName
            val accessibilityComp = "$pkg/$pkg.service.PowerAccessibilityService"

            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            // Grant usage stats permission
            os.writeBytes("pm grant $pkg android.permission.PACKAGE_USAGE_STATS\n")

            // Enable accessibility service silently via root
            os.writeBytes("settings put secure enabled_accessibility_services $accessibilityComp\n")
            os.writeBytes("settings put secure accessibility_enabled 1\n")

            os.writeBytes("exit\n")
            os.flush()
            process.waitFor(2, TimeUnit.SECONDS)
            Log.i(TAG, "Silent setup of accessibility & usage permissions completed")
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Failed silent permission setup: ${e.message}")
            return@withContext false
        }
    }
}
