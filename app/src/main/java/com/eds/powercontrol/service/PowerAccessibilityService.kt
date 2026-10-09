package com.eds.powercontrol.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.eds.powercontrol.model.OperatingMode
import com.eds.powercontrol.util.AppPreferences
import com.eds.powercontrol.util.RootUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Real-time event-driven detector for Android TV.
 * Receives immediate OS callbacks (0 ms latency) when window states change,
 * allowing instant interception and termination of com.qstar.powerui without polling lag.
 */
class PowerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastInterceptTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (packageName == AppPreferences.TARGET_POWER_UI_PACKAGE) {
            val now = System.currentTimeMillis()
            // 1.5s debounce to avoid repeat triggers during activity transition
            if (now - lastInterceptTime < 1500L) return
            lastInterceptTime = now

            val prefs = AppPreferences(this)
            val mode = prefs.operatingMode
            val targetPackage = prefs.selectedPackageName

            if (mode != OperatingMode.NONE && targetPackage.isNotBlank()) {
                Log.w(TAG, "REAL-TIME INTERCEPT via AccessibilityService: $packageName detected!")
                scope.launch {
                    RootUtil.killPowerUiAndLaunchApp(targetPackage)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "PowerAccessibility"
    }
}
