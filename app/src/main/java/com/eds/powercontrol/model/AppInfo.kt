package com.eds.powercontrol.model

import android.graphics.drawable.Drawable

/**
 * Data model representing an installed application on the TV Box.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val icon: Drawable? = null,
    val launchActivity: String? = null
)

/**
 * Operating mode options for Power Control.
 */
enum class OperatingMode {
    NONE,
    SYSTEM,
    USER;

    companion object {
        fun fromString(value: String): OperatingMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
