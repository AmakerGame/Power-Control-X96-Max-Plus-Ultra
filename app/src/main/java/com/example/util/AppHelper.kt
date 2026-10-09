package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper to query installed applications and launch applications without requiring root.
 */
object AppHelper {
    private const val TAG = "AppHelper"

    /**
     * Retrieves all installed applications with their launch intent.
     * Categorizes into system and user applications.
     */
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPackage = context.packageName

        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<AppInfo>()

        for (app in packages) {
            // Exclude our own app from selection to avoid recursion
            if (app.packageName == selfPackage) continue

            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            val launchActivity = launchIntent?.component?.className

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val appName = try {
                app.loadLabel(pm).toString()
            } catch (e: Exception) {
                app.packageName
            }

            val icon = try {
                app.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            appList.add(
                AppInfo(
                    packageName = app.packageName,
                    appName = appName,
                    isSystem = isSystem,
                    icon = icon,
                    launchActivity = launchActivity
                )
            )
        }

        // Sort alphabetically by app name
        appList.sortedBy { it.appName.lowercase() }
    }

    /**
     * Launches the target application using standard Android Intent APIs.
     * Does NOT use root — adheres to requirement of root minimization.
     */
    fun launchApp(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
                Log.d(TAG, "Launched application $packageName via Intent")
                return true
            }

            // Fallback shell launch if standard launch intent is not defined
            Log.w(TAG, "Launch intent not found for $packageName, falling back to monkey am start")
            val cmd = "monkey -p $packageName 1"
            Runtime.getRuntime().exec(cmd)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
        }
        return false
    }
}
