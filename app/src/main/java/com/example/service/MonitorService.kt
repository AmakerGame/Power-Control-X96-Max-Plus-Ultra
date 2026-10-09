package com.example.service

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
import com.example.MainActivity
import com.example.R
import com.example.model.OperatingMode
import com.example.util.AppHelper
import com.example.util.AppPreferences
import com.example.util.ForegroundDetector
import com.example.util.RootUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Background Foreground Service that continuously monitors foreground applications on Android TV.
 * Minimizes root usage: queries foreground via UsageStatsManager without root;
 * root is strictly executed ONLY to force-stop com.qstar.powerui when it appears.
 */
class MonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MonitorService created")
        preferences = AppPreferences(this)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PowerControl:MonitorWakeLock")?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.i(TAG, "onStartCommand with action: $action")

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
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // Safe 24hr timeout

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            Log.i(TAG, "Foreground monitoring loop started")
            while (isActive) {
                try {
                    val mode = preferences.operatingMode
                    val targetPackage = preferences.selectedPackageName

                    // If mode is set to NONE or no target package, stop monitoring
                    if (mode == OperatingMode.NONE || targetPackage.isBlank()) {
                        Log.i(TAG, "Operating mode is NONE or target package empty. Stopping monitor.")
                        break
                    }

                    // Foreground check performed without root via SDK APIs
                    val currentForeground = ForegroundDetector.getForegroundPackageName(this@MonitorService)

                    if (currentForeground != null && currentForeground == AppPreferences.TARGET_POWER_UI_PACKAGE) {
                        Log.w(
                            TAG,
                            "INTERCEPT TRIGGERED: Detected ${AppPreferences.TARGET_POWER_UI_PACKAGE} in foreground!"
                        )

                        // 1. Force-stop com.qstar.powerui using root (strictly only root usage)
                        val forceStopSuccess = RootUtil.forceStopPackage(AppPreferences.TARGET_POWER_UI_PACKAGE)
                        Log.i(TAG, "am force-stop execution result: $forceStopSuccess")

                        // 2. Immediately launch the user-selected application without root
                        val launchSuccess = AppHelper.launchApp(this@MonitorService, targetPackage)
                        Log.i(TAG, "Target app $targetPackage launch result: $launchSuccess")

                        // Cooldown to prevent repetitive loop while target app is loading
                        delay(COOLDOWN_AFTER_INTERCEPT_MS)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop iteration", e)
                }

                delay(POLL_INTERVAL_MS)
            }
            preferences.isServiceRunning = false
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "Stopping foreground monitoring")
        monitorJob?.cancel()
        monitorJob = null
        preferences.isServiceRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
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
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
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
        Log.i(TAG, "MonitorService destroyed")
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

        private const val POLL_INTERVAL_MS = 800L
        private const val COOLDOWN_AFTER_INTERCEPT_MS = 2500L

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
