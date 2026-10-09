package com.eds.powercontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.eds.powercontrol.model.OperatingMode
import com.eds.powercontrol.service.MonitorService
import com.eds.powercontrol.util.AppPreferences

/**
 * BroadcastReceiver triggered when the TV Box completes boot sequence.
 * Automatically initiates foreground monitoring if configured with System or User mode.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val preferences = AppPreferences(context)
            val mode = preferences.operatingMode
            val targetPackage = preferences.selectedPackageName

            Log.d(TAG, "Boot check: mode=$mode, targetPackage=$targetPackage")

            // Autostart monitoring if user configured SYSTEM or USER mode and selected a target app
            if ((mode == OperatingMode.SYSTEM || mode == OperatingMode.USER) && targetPackage.isNotBlank()) {
                Log.i(TAG, "Starting MonitorService on device boot")
                MonitorService.start(context)
            } else {
                Log.d(TAG, "Monitoring autostart skipped: mode is NONE or no app selected")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
