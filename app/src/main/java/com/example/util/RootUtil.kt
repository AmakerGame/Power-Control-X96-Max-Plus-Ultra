package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Utility for managing root (Superuser) permissions and executing root commands.
 * Minimizes root usage: root is strictly used for force-stopping com.qstar.powerui.
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

    /**
     * Checks if the `su` binary exists on the file system.
     */
    fun isSuBinaryPresent(): Boolean {
        for (path in SU_BINARY_PATHS) {
            val file = File(path)
            if (file.exists()) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if root access is granted by running `su -c id`.
     * Must be called off the main thread.
     */
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine() ?: ""
            val exited = process.waitFor(3, TimeUnit.SECONDS)
            if (exited && process.exitValue() == 0 && output.contains("uid=0")) {
                Log.d(TAG, "Root check passed: $output")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Root check failed: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Requests root access by invoking `su` shell, which prompts the Superuser
     * dialogue (Magisk, SuperSU, SlimBoxTV SU) if not yet granted.
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("id\n")
            os.writeBytes("exit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine() ?: ""
            val exited = process.waitFor(15, TimeUnit.SECONDS)

            if (exited && process.exitValue() == 0 && line.contains("uid=0")) {
                Log.d(TAG, "Root requested and granted successfully")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting root access", e)
        }
        return@withContext false
    }

    /**
     * Force stops the specified package using root am force-stop command.
     * This is strictly the single place where root command execution occurs.
     */
    suspend fun forceStopPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext false
        try {
            val command = "am force-stop $packageName"
            Log.i(TAG, "Executing root command: $command")
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exited = process.waitFor(3, TimeUnit.SECONDS)
            if (exited && process.exitValue() == 0) {
                Log.d(TAG, "Force-stop succeeded for $packageName")
                return@withContext true
            }
            Log.w(TAG, "Force-stop exited with code ${process.exitValue()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force-stop $packageName via root", e)
        }
        return@withContext false
    }

    /**
     * Helper to silently grant PACKAGE_USAGE_STATS permission via root on TV box.
     * Useful for TV boxes where navigating to Android Settings is cumbersome.
     */
    suspend fun grantUsageStatsPermissionSilently(appPackageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = "pm grant $appPackageName android.permission.PACKAGE_USAGE_STATS"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exited = process.waitFor(3, TimeUnit.SECONDS)
            return@withContext exited && process.exitValue() == 0
        } catch (e: Exception) {
            Log.w(TAG, "Unable to silently grant usage stats via root: ${e.message}")
            return@withContext false
        }
    }
}
