package com.qstar.powerui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PowerUiTest {

    @Test
    fun testSharedPreferencesStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)

        // Initial state is null
        assertNull(prefs.getString(Settings.KEY_TARGET_PACKAGE, null))

        // Save target package
        val youtubeTv = "com.google.android.youtube.tv"
        prefs.edit().putString(Settings.KEY_TARGET_PACKAGE, youtubeTv).commit()
        assertEquals(youtubeTv, prefs.getString(Settings.KEY_TARGET_PACKAGE, null))

        // Clear target package
        prefs.edit().remove(Settings.KEY_TARGET_PACKAGE).commit()
        assertNull(prefs.getString(Settings.KEY_TARGET_PACKAGE, null))
    }

    @Test
    fun testAppItemData() {
        val item = AppItem(
            packageName = "org.xbmc.kodi",
            name = "Kodi",
            icon = null
        )
        assertEquals("org.xbmc.kodi", item.packageName)
        assertEquals("Kodi", item.name)
        assertNull(item.icon)
    }
}
