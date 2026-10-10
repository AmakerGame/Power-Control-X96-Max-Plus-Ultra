package com.qstar.powerui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Proxy-launcher Activity:
 * Immediately launches the user-configured target application with no UI delays or animations,
 * then finishes itself immediately. If no target application is configured (or if it was uninstalled),
 * redirects to the Settings activity.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)
        val targetPackage = prefs.getString(Settings.KEY_TARGET_PACKAGE, null)

        if (!targetPackage.isNullOrBlank()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                startActivity(launchIntent)
                overridePendingTransition(0, 0)
                finish()
                return
            }
        }

        // If target app is not configured or uninstalled, open Settings
        val settingsIntent = Intent(this, Settings::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(settingsIntent)
        overridePendingTransition(0, 0)
        finish()
    }
}
