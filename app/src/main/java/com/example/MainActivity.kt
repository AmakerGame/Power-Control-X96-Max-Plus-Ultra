package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.MainTvScreen
import com.example.ui.screens.RootCheckScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Main Android TV Activity for Power Control.
 * Runs exclusively on Android 11 (API 30) on TV boxes like X96 Max Plus Ultra (SlimBoxTV).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!uiState.isRootGranted) {
                        // 1. Root check screen: shown until root is granted
                        RootCheckScreen(
                            isLoading = uiState.isCheckingRoot,
                            errorMessage = uiState.rootErrorMessage,
                            onRequestRoot = { viewModel.requestRoot() }
                        )
                    } else {
                        // 2. Main TV screen: App selection, mode toggle, save & test
                        MainTvScreen(
                            installedApps = uiState.installedApps,
                            isLoadingApps = uiState.isLoadingApps,
                            currentMode = uiState.operatingMode,
                            selectedPackage = uiState.selectedPackageName,
                            selectedAppName = uiState.selectedAppName,
                            isServiceActive = uiState.isServiceRunning,
                            hasUsageStatsAccess = uiState.hasUsageStatsAccess,
                            onModeChanged = { mode -> viewModel.setOperatingMode(mode) },
                            onAppSelected = { app -> viewModel.selectApp(app) },
                            onSaveSettings = { viewModel.saveSettings(this) },
                            onTestInterception = { viewModel.testInterception(this) },
                            onGrantUsageAccess = { openUsageSettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUsagePermission()
    }

    private fun openUsageSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Some TV box firmwares don't have standard Usage Access settings activity
            // Try silently via root as backup
            viewModel.checkAndGrantUsageAccessSilently()
        }
    }
}
