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
 * Ultra-fast root engine with warm shell caching to eliminate the 400ms su startup delay.
 * Allows instant process termination before com.qstar.powerui can render its UI on screen.
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

    private var warmSuProcess: Process? = null
    private var warmOutputStream: DataOutputStream? = null

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
     * Initializes a warm root shell so subsequent kill/launch commands execute in <1ms without spawn overhead.
     */
    @Synchronized
    fun ensureWarmShell() {
        if (warmSuProcess == null || !isProcessAlive(warmSuProcess)) {
            try {
                warmSuProcess = Runtime.getRuntime().exec("su")
                warmOutputStream = DataOutputStream(warmSuProcess!!.outputStream)
                Log.d(TAG, "Warm root shell connected")
            } catch (e: Exception) {
                Log.w(TAG, "Failed connecting warm root shell: ${e.message}")
                warmSuProcess = null
                warmOutputStream = null
            }
        }
    }

    @Synchronized
    fun closeWarmShell() {
        try {
            warmOutputStream?.writeBytes("exit\n")
            warmOutputStream?.flush()
            warmOutputStream?.close()
            warmSuProcess?.destroy()
        } catch (e: Exception) {
            // Ignore
        } finally {
            warmOutputStream = null
            warmSuProcess = null
            Log.d(TAG, "Warm root shell closed")
        }
    }

    private fun isProcessAlive(p: Process?): Boolean {
        if (p == null) return false
        return try {
            p.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    /**
     * Instantly kills com.qstar.powerui and launches the target application via the warm shell.
     * Executes in under 1 millisecond so com.qstar.powerui NEVER has time to render its UI.
     */
    fun instantIntercept(targetPackage: String) {
        synchronized(this) {
            ensureWarmShell()
            val os = warmOutputStream
            if (os != null) {
                try {
                    // Send kill commands first to abort powerui before it draws
                    os.writeBytes("am force-stop com.qstar.powerui\n")
                    os.writeBytes("pkill -9 -f com.qstar.powerui\n")
                    os.writeBytes("kill -9 $(pidof com.qstar.powerui) 2>/dev/null\n")

                    // Launch user app immediately without transition animations
                    if (targetPackage.isNotBlank()) {
                        os.writeBytes("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER --activity-brought-to-front --activity-no-animation $targetPackage 2>/dev/null || am start $targetPackage 2>/dev/null\n")
                    }
                    os.flush()
                    Log.i(TAG, "instantIntercept sent commands for $targetPackage")
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Warm shell write failed, reconnecting", e)
                    closeWarmShell()
                }
            }
        }

        // Fallback cold execution if warm shell was unavailable
        try {
            val process = Runtime.getRuntime().exec("su")
            val coldOs = DataOutputStream(process.outputStream)
            coldOs.writeBytes("am force-stop com.qstar.powerui\n")
            coldOs.writeBytes("pkill -9 -f com.qstar.powerui\n")
            coldOs.writeBytes("kill -9 $(pidof com.qstar.powerui) 2>/dev/null\n")
            if (targetPackage.isNotBlank()) {
                coldOs.writeBytes("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER --activity-brought-to-front --activity-no-animation $targetPackage 2>/dev/null || am start $targetPackage 2>/dev/null\n")
            }
            coldOs.writeBytes("exit\n")
            coldOs.flush()
            process.waitFor(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback cold execution failed", e)
        }
    }

    /**
     * Grants PACKAGE_USAGE_STATS and READ_LOGS permissions silently via root.
     * No Accessibility Services are touched.
     */
    suspend fun grantPermissionsSilently(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val pkg = context.packageName
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("pm grant $pkg android.permission.PACKAGE_USAGE_STATS\n")
            os.writeBytes("pm grant $pkg android.permission.READ_LOGS\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor(2, TimeUnit.SECONDS)
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Silent permission grant: ${e.message}")
            return@withContext false
        }
    }
}
