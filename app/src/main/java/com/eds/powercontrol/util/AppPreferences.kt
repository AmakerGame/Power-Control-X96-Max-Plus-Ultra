package com.eds.powercontrol.util

import android.content.Context
import android.content.SharedPreferences
import com.eds.powercontrol.model.OperatingMode

/**
 * Storage manager for Power Control configuration using SharedPreferences.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var operatingMode: OperatingMode
        get() {
            val raw = prefs.getString(KEY_MODE, OperatingMode.NONE.name) ?: OperatingMode.NONE.name
            return OperatingMode.fromString(raw)
        }
        set(value) {
            prefs.edit().putString(KEY_MODE, value.name).apply()
        }

    var selectedPackageName: String
        get() = prefs.getString(KEY_SELECTED_PACKAGE, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SELECTED_PACKAGE, value).apply()
        }

    var selectedAppName: String
        get() = prefs.getString(KEY_SELECTED_APP_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SELECTED_APP_NAME, value).apply()
        }

    var selectedActivityName: String
        get() = prefs.getString(KEY_SELECTED_ACTIVITY_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SELECTED_ACTIVITY_NAME, value).apply()
        }

    var isServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SERVICE_RUNNING, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "power_control_prefs"
        private const val KEY_MODE = "operating_mode"
        private const val KEY_SELECTED_PACKAGE = "selected_package"
        private const val KEY_SELECTED_APP_NAME = "selected_app_name"
        private const val KEY_SELECTED_ACTIVITY_NAME = "selected_activity_name"
        private const val KEY_SERVICE_RUNNING = "service_running"

        const val TARGET_POWER_UI_PACKAGE = "com.qstar.powerui"
    }
}
