package com.eds.powercontrol.util

import android.content.Context
import android.util.Log
import com.eds.powercontrol.model.OperatingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            appendLine("echo \"RUNNING ${d}(date +%s)\" > \"${d}STATUS_FILE\"")
            appendLine()
            appendLine("while true; do")
            appendLine("    if [ -f \"${d}CONF_FILE\" ]; then")
            appendLine("        MODE=\"NONE\"")
            appendLine("        TARGET_PKG=\"\"")
            appendLine("        . \"${d}CONF_FILE\"")
            appendLine()
            appendLine("        if [ \"${d}MODE\" = \"SYSTEM\" ] || [ \"${d}MODE\" = \"USER\" ]; then")
            appendLine("            if [ -n \"${d}TARGET_PKG\" ]; then")
            appendLine("                FOCUS=\"${d}(dumpsys window 2>/dev/null | grep -m 1 'mCurrentFocus')\"")
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
            appendLine("    sleep 0.2 2>/dev/null || usleep 200000 2>/dev/null || sleep 1")
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

        val cmd = "kill -0 $pid 2>/dev/null"
        return@withContext RootUtil.executeRootCommand(cmd)
    }

    /**
     * Starts the root daemon in the background via su without relying on nohup.
     */
    suspend fun startDaemon(context: Context, mode: OperatingMode, targetPackage: String): Boolean = withContext(Dispatchers.IO) {
        deployScript(context)
        writeConfig(context, mode, targetPackage)

        val scriptPath = getScriptFile(context).absolutePath
        val cmd = "pkill -9 -f $DAEMON_SCRIPT_NAME 2>/dev/null; chmod 755 $scriptPath; (/system/bin/sh $scriptPath >/dev/null 2>&1 &)"
        val ok = RootUtil.executeRootCommand(cmd)
        Log.i(TAG, "Started daemon via root: $ok")
        return@withContext ok
    }

    /**
     * Stops the root daemon process completely.
     */
    suspend fun stopDaemon(context: Context): Boolean = withContext(Dispatchers.IO) {
        writeConfig(context, OperatingMode.NONE, "")
        val pidFile = getPidFile(context)
        val pid = if (pidFile.exists()) pidFile.readText().trim() else ""

        val cmd = if (pid.isNotBlank()) {
            "kill -9 $pid 2>/dev/null; pkill -9 -f $DAEMON_SCRIPT_NAME 2>/dev/null"
        } else {
            "pkill -9 -f $DAEMON_SCRIPT_NAME 2>/dev/null"
        }
        val ok = RootUtil.executeRootCommand(cmd)
        pidFile.delete()
        Log.i(TAG, "Stopped daemon: $ok")
        return@withContext ok
    }
}
