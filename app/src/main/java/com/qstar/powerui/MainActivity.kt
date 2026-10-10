package com.qstar.powerui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Proxy-launcher Activity:
 * Immediately launches the user-configured target application with no UI delays or animations,
 * then finishes itself immediately. If no target application is configured (or if it was uninstalled),
 * redirects to the Settings activity.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)
        val targetPackage = prefs.getString(Settings.KEY_TARGET_PACKAGE, null)
        val targetComponent = prefs.getString(Settings.KEY_TARGET_COMPONENT, null)

        if (!targetPackage.isNullOrBlank()) {
            var launchIntent: Intent? = null

            // 1. Ultra-fast direct ComponentName launch (0ms delay, no PackageManager query needed)
            if (!targetComponent.isNullOrBlank()) {
                try {
                    val comp = ComponentName.unflattenFromString(targetComponent)
                    if (comp != null) {
                        launchIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            component = comp
                        }
                    }
                } catch (e: Exception) {
                    launchIntent = null
                }
            }

            // 2. Fallback to packageManager if component not cached
            if (launchIntent == null) {
                launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            }

            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                startActivity(launchIntent)
                overridePendingTransition(0, 0)
                finish()
                return
            }
        }

        // If target app is not configured or uninstalled, open Settings
        val settingsIntent = Intent(this, Settings::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(settingsIntent)
        overridePendingTransition(0, 0)
        finish()
    }
}
