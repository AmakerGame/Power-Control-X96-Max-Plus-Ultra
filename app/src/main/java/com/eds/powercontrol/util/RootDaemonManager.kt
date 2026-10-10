package com.eds.powercontrol.util

import android.content.Context
import android.util.Log
import com.eds.powercontrol.model.OperatingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File

/**
 * Manages the background Root Daemon process that monitors and intercepts com.qstar.powerui.
 * Runs directly as a persistent root process with full system privileges.
 */
object RootDaemonManager {
    private const val TAG = "RootDaemonManager"

    private const val DAEMON_SCRIPT_NAME = "powercontrol_daemon.sh"
    private const val CONFIG_FILE_NAME = "monitor.conf"
    private const val STATUS_FILE_NAME = "monitor.status"
    private const val PID_FILE_NAME = "daemon.pid"

    fun getScriptFile(context: Context): File = File(context.filesDir, DAEMON_SCRIPT_NAME)
    fun getConfigFile(context: Context): File = File(context.filesDir, CONFIG_FILE_NAME)
    fun getStatusFile(context: Context): File = File(context.filesDir, STATUS_FILE_NAME)
    fun getPidFile(context: Context): File = File(context.filesDir, PID_FILE_NAME)

    /**
     * Creates or updates the daemon script file on internal storage.
     */
    fun deployScript(context: Context) {
        val scriptFile = getScriptFile(context)
        val confPath = getConfigFile(context).absolutePath
        val statusPath = getStatusFile(context).absolutePath
        val pidPath = getPidFile(context).absolutePath

        val d = "$"
        val scriptContent = StringBuilder().apply {
            appendLine("#!/system/bin/sh")
            appendLine("# Power Control Monitor Daemon (Root)")
            appendLine("CONF_FILE=\"$confPath\"")
            appendLine("STATUS_FILE=\"$statusPath\"")
            appendLine("PID_FILE=\"$pidPath\"")
            appendLine("TARGET_UI=\"com.qstar.powerui\"")
            appendLine()
            appendLine("echo \"${d}${d}\" > \"${d}PID_FILE\"")
            appendLine("echo \"STARTED ${d}(date +%s)\" > \"${d}STATUS_FILE\"")
            appendLine()
            appendLine("while true; do")
            appendLine("    if [ -f \"${d}CONF_FILE\" ]; then")
            appendLine("        MODE=\"NONE\"")
            appendLine("        TARGET_PKG=\"\"")
            appendLine("        . \"${d}CONF_FILE\"")
            appendLine()
            appendLine("        if [ \"${d}MODE\" = \"SYSTEM\" ] || [ \"${d}MODE\" = \"USER\" ]; then")
            appendLine("            if [ -n \"${d}TARGET_PKG\" ]; then")
            appendLine("                FOCUS=\"${d}(dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp')\"")
            appendLine("                case \"${d}FOCUS\" in")
            appendLine("                    *\"${d}TARGET_UI\"*)")
            appendLine("                        echo \"INTERCEPT ${d}(date +%s) ${d}TARGET_PKG\" >> \"${d}STATUS_FILE\"")
            appendLine("                        am force-stop \"${d}TARGET_UI\"")
            appendLine("                        kill -9 \"${d}(pidof \"${d}TARGET_UI\")\" 2>/dev/null")
            appendLine("                        am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \"${d}TARGET_PKG\" 2>/dev/null || am start \"${d}TARGET_PKG\" 2>/dev/null")
            appendLine("                        sleep 1.5")
            appendLine("                        ;;")
            appendLine("                esac")
            appendLine("            fi")
            appendLine("        fi")
            appendLine("    fi")
            appendLine("    sleep 0.15 2>/dev/null || usleep 150000 2>/dev/null || sleep 1")
            appendLine("done")
        }.toString()

        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true, false)
        Log.i(TAG, "Daemon script deployed to ${scriptFile.absolutePath}")
    }

    /**
     * Writes configuration to monitor.conf.
     */
    fun writeConfig(context: Context, mode: OperatingMode, targetPackage: String) {
        val configFile = getConfigFile(context)
        val content = """
MODE=${mode.name}
TARGET_PKG=$targetPackage
""".trimIndent()
        configFile.writeText(content)
        Log.d(TAG, "Wrote config: MODE=${mode.name}, TARGET_PKG=$targetPackage")
    }

    /**
     * Checks if the root daemon process is currently running.
     */
    suspend fun isDaemonRunning(context: Context): Boolean = withContext(Dispatchers.IO) {
        val pidFile = getPidFile(context)
        if (!pidFile.exists()) return@withContext false
        val pid = pidFile.readText().trim()
        if (pid.isBlank()) return@withContext false

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("kill -0 $pid 2>/dev/null && echo ALIVE\nexit\n")
            os.flush()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return@withContext output.contains("ALIVE")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check daemon PID: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Starts the root daemon in the background via su.
     */
    suspend fun startDaemon(context: Context, mode: OperatingMode, targetPackage: String): Boolean = withContext(Dispatchers.IO) {
        deployScript(context)
        writeConfig(context, mode, targetPackage)

        val scriptPath = getScriptFile(context).absolutePath

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            // Kill any old instance first
            os.writeBytes("pkill -9 -f $DAEMON_SCRIPT_NAME 2>/dev/null\n")
            // Launch as background root process detached with nohup
            os.writeBytes("chmod 755 $scriptPath\n")
            os.writeBytes("nohup /system/bin/sh $scriptPath > /dev/null 2>&1 &\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()

            Log.i(TAG, "Started daemon via root: $scriptPath")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting root daemon", e)
        }
        return@withContext false
    }

    /**
     * Stops the root daemon process completely.
     */
    suspend fun stopDaemon(context: Context): Boolean = withContext(Dispatchers.IO) {
        writeConfig(context, OperatingMode.NONE, "")
        val pidFile = getPidFile(context)
        val pid = if (pidFile.exists()) pidFile.readText().trim() else ""

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            if (pid.isNotBlank()) {
                os.writeBytes("kill -9 $pid 2>/dev/null\n")
            }
            os.writeBytes("pkill -9 -f $DAEMON_SCRIPT_NAME 2>/dev/null\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()

            pidFile.delete()
            Log.i(TAG, "Stopped daemon")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed stopping root daemon", e)
        }
        return@withContext false
    }

    /**
     * Directly executes test interception via root (am force-stop + am start).
     */
    suspend fun testDirectIntercept(targetPackage: String): Boolean = withContext(Dispatchers.IO) {
        if (targetPackage.isBlank()) return@withContext false
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("am force-stop com.qstar.powerui\n")
            os.writeBytes("kill -9 $(pidof com.qstar.powerui) 2>/dev/null\n")
            os.writeBytes("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $targetPackage 2>/dev/null || am start $targetPackage 2>/dev/null\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            Log.i(TAG, "testDirectIntercept executed for $targetPackage")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed testDirectIntercept", e)
        }
        return@withContext false
    }
}
