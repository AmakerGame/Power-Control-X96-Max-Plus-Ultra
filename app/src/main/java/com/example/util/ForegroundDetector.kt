package com.example.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log

/**
 * Detects the foreground application using Android SDK APIs without root privileges.
 * Respects the root-minimization requirement by using UsageStatsManager.
 */
object ForegroundDetector {
    private const val TAG = "ForegroundDetector"

    /**
     * Checks if the app has permission to access usage stats (PACKAGE_USAGE_STATS).
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.w(TAG, "Error checking usage stats permission: ${e.message}")
            false
        }
    }

    /**
     * Determines the currently active foreground package name without executing root commands.
     */
    fun getForegroundPackageName(context: Context): String? {
        // Strategy 1: UsageStatsManager queryEvents (Android 5.0+ / 11)
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageManager != null) {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10_000 // look back 10 seconds

            try {
                val usageEvents = usageManager.queryEvents(beginTime, endTime)
                val event = UsageEvents.Event()
                var lastForegroundPackage: String? = null
                var lastTimestamp: Long = 0

                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                    ) {
                        if (event.timeStamp >= lastTimestamp) {
                            lastTimestamp = event.timeStamp
                            lastForegroundPackage = event.packageName
                        }
                    }
                }

                if (!lastForegroundPackage.isNullOrBlank()) {
                    return lastForegroundPackage
                }
            } catch (e: Exception) {
                Log.w(TAG, "UsageEvents query failed: ${e.message}")
            }
        }

        // Strategy 2: ActivityManager running processes fallback
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = am?.runningAppProcesses
            if (!processes.isNullOrEmpty()) {
                for (proc in processes) {
                    if (proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        return proc.pkgList?.firstOrNull() ?: proc.processName
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ActivityManager fallback failed: ${e.message}")
        }

        return null
    }
}
