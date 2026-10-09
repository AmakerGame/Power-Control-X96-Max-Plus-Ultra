package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.OperatingMode
import com.example.util.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Power Control", appName)
  }

  @Test
  fun `verify default preferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferences(context)
    assertEquals(OperatingMode.NONE, prefs.operatingMode)
    assertEquals("", prefs.selectedPackageName)
    assertFalse(prefs.isServiceRunning)
  }

  @Test
  fun `verify preference updates`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferences(context)
    prefs.operatingMode = OperatingMode.USER
    prefs.selectedPackageName = "com.google.android.youtube.tv"
    prefs.selectedAppName = "YouTube TV"

    assertEquals(OperatingMode.USER, prefs.operatingMode)
    assertEquals("com.google.android.youtube.tv", prefs.selectedPackageName)
    assertEquals("YouTube TV", prefs.selectedAppName)
  }
}
