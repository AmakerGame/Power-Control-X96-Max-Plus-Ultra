package com.eds.powercontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eds.powercontrol.MainActivity
import com.eds.powercontrol.R
import com.eds.powercontrol.model.OperatingMode
import com.eds.powercontrol.util.AppPreferences
import com.eds.powercontrol.util.ForegroundDetector
import com.eds.powercontrol.util.RootDaemonManager
import com.eds.powercontrol.util.RootUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background Service that maintains active monitoring and supervises the root daemon process.
 * Provides dual-layer interception: foreground polling in Kotlin + persistent root daemon in shell.
 */
class MonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var preferences: AppPreferences
    private var lastInterceptTime = 0L

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PowerControl:MonitorWakeLock")?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        preferences.isServiceRunning = true
        try {
            wakeLock?.acquire(24 * 60 * 60 * 1000L)
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquire: ${e.message}")
        }

        val mode = preferences.operatingMode
        val targetPackage = preferences.selectedPackageName

        if (mode == OperatingMode.NONE || targetPackage.isBlank()) {
            stopMonitoring()
            return
        }

        // 1. Start persistent Root Daemon process
        serviceScope.launch(Dispatchers.IO) {
            RootDaemonManager.startDaemon(this@MonitorService, mode, targetPackage)
        }

        // 2. Continuous parallel monitoring loop
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    val currentMode = preferences.operatingMode
                    val currentTarget = preferences.selectedPackageName

                    if (currentMode == OperatingMode.NONE || currentTarget.isBlank()) {
                        break
                    }

                    // Foreground app check via UsageStats / ActivityManager
                    val foreground = ForegroundDetector.getForegroundPackageName(this@MonitorService)
                    if (foreground != null && foreground == AppPreferences.TARGET_POWER_UI_PACKAGE) {
                        val now = System.currentTimeMillis()
                        if (now - lastInterceptTime > 1500L) {
                            lastInterceptTime = now
                            Log.w(TAG, "Service detected $foreground -> Executing root intercept for $currentTarget")
                            RootUtil.killPowerUiAndLaunch(this@MonitorService, currentTarget)
                            delay(1500L)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in service monitor loop", e)
                }

                delay(200L)
            }
            preferences.isServiceRunning = false
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        preferences.isServiceRunning = false

        serviceScope.launch(Dispatchers.IO) {
            RootDaemonManager.stopDaemon(this@MonitorService)
        }

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val targetApp = preferences.selectedAppName.ifBlank { preferences.selectedPackageName }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(
                if (targetApp.isNotBlank()) {
                    getString(R.string.status_monitoring_active, targetApp, preferences.operatingMode.name)
                } else {
                    getString(R.string.service_notification_desc)
                }
            )
            .setSmallIcon(R.drawable.power_control_icon_1791558022473)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MonitorService"
        private const val CHANNEL_ID = "power_control_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.eds.powercontrol.action.START_MONITOR"
        const val ACTION_STOP = "com.eds.powercontrol.action.STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
