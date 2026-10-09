package com.eds.powercontrol.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log

/**
 * Fast foreground app detector for Android TV.
 */
object ForegroundDetector {
    private const val TAG = "ForegroundDetector"

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
            false
        }
    }

    /**
     * Determines current foreground package using fastest available API.
     */
    fun getForegroundPackageName(context: Context): String? {
        // Method 1: UsageStatsManager UsageEvents (recent 5 seconds)
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageManager != null) {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 5_000

            try {
                val usageEvents = usageManager.queryEvents(beginTime, endTime)
                val event = UsageEvents.Event()
                var latestPackage: String? = null
                var latestTime: Long = 0

                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                    ) {
                        if (event.timeStamp >= latestTime) {
                            latestTime = event.timeStamp
                            latestPackage = event.packageName
                        }
                    }
                }

                if (!latestPackage.isNullOrBlank()) {
                    return latestPackage
                }
            } catch (e: Exception) {
                // Ignore and try fallback
            }
        }

        // Method 2: ActivityManager running processes
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = am?.runningAppProcesses
            if (!processes.isNullOrEmpty()) {
                for (proc in processes) {
                    if (proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
                        return pkg
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }
}
