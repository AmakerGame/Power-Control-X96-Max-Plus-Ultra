package com.eds.powercontrol.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eds.powercontrol.R
import com.eds.powercontrol.model.AppInfo
import com.eds.powercontrol.model.OperatingMode
import com.eds.powercontrol.service.MonitorService
import com.eds.powercontrol.util.AppHelper
import com.eds.powercontrol.util.AppPreferences
import com.eds.powercontrol.util.ForegroundDetector
import com.eds.powercontrol.util.RootUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isCheckingRoot: Boolean = true,
    val isRootGranted: Boolean = false,
    val rootErrorMessage: String? = null,
    val installedApps: List<AppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val operatingMode: OperatingMode = OperatingMode.NONE,
    val selectedPackageName: String = "",
    val selectedAppName: String = "",
    val isServiceRunning: Boolean = false,
    val hasUsageStatsAccess: Boolean = false,
    val toastMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AppPreferences(application)
    private val _uiState = MutableStateFlow(
        MainUiState(
            operatingMode = preferences.operatingMode,
            selectedPackageName = preferences.selectedPackageName,
            selectedAppName = preferences.selectedAppName,
            isServiceRunning = preferences.isServiceRunning,
            hasUsageStatsAccess = ForegroundDetector.hasUsageStatsPermission(application)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        performInitialRootCheck()
    }

    fun performInitialRootCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingRoot = true, rootErrorMessage = null) }
            val hasRoot = RootUtil.checkRootAccess()
            if (hasRoot) {
                _uiState.update {
                    it.copy(
                        isCheckingRoot = false,
                        isRootGranted = true,
                        rootErrorMessage = null
                    )
                }
                onRootConfirmed()
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingRoot = false,
                        isRootGranted = false
                    )
                }
            }
        }
    }

    fun requestRoot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingRoot = true, rootErrorMessage = null) }
            val granted = RootUtil.requestRootAccess()
            if (granted) {
                _uiState.update {
                    it.copy(
                        isCheckingRoot = false,
                        isRootGranted = true,
                        rootErrorMessage = null
                    )
                }
                onRootConfirmed()
            } else {
                val context = getApplication<Application>()
                _uiState.update {
                    it.copy(
                        isCheckingRoot = false,
                        isRootGranted = false,
                        rootErrorMessage = context.getString(R.string.root_denied_message)
                    )
                }
            }
        }
    }

    private fun onRootConfirmed() {
        loadInstalledApps()
        checkAndGrantUsageAccessSilently()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val context = getApplication<Application>()
            val apps = AppHelper.getInstalledApps(context)
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    isLoadingApps = false
                )
            }
        }
    }

    fun checkAndGrantUsageAccessSilently() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Automatically grant usage stats and read logs permissions via root without accessibility service
            RootUtil.grantPermissionsSilently(context)
            val hasAccess = ForegroundDetector.hasUsageStatsPermission(context)
            _uiState.update { it.copy(hasUsageStatsAccess = hasAccess) }
        }
    }

    fun setOperatingMode(mode: OperatingMode) {
        _uiState.update { it.copy(operatingMode = mode) }
    }

    fun selectApp(app: AppInfo) {
        val currentMode = _uiState.value.operatingMode
        val newMode = if (currentMode == OperatingMode.NONE) {
            if (app.isSystem) OperatingMode.SYSTEM else OperatingMode.USER
        } else {
            currentMode
        }

        _uiState.update {
            it.copy(
                selectedPackageName = app.packageName,
                selectedAppName = app.appName,
                operatingMode = newMode
            )
        }
    }

    fun saveSettings(context: Context) {
        val state = _uiState.value
        preferences.operatingMode = state.operatingMode
        preferences.selectedPackageName = state.selectedPackageName
        preferences.selectedAppName = state.selectedAppName

        if (state.operatingMode != OperatingMode.NONE && state.selectedPackageName.isNotBlank()) {
            // Start background monitoring service
            MonitorService.start(context)
            preferences.isServiceRunning = true
            _uiState.update { it.copy(isServiceRunning = true) }
            Toast.makeText(
                context,
                context.getString(R.string.status_monitoring_active, state.selectedAppName, state.operatingMode.name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Mode is NONE -> stop service and no root is used
            MonitorService.stop(context)
            preferences.isServiceRunning = false
            _uiState.update { it.copy(isServiceRunning = false) }
            Toast.makeText(
                context,
                context.getString(R.string.status_monitoring_inactive),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun testInterception(context: Context) {
        val state = _uiState.value
        val targetPackage = state.selectedPackageName
        if (targetPackage.isBlank()) {
            Toast.makeText(context, context.getString(R.string.no_app_selected), Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            // Instantly kill com.qstar.powerui and launch target app
            RootUtil.instantIntercept(targetPackage)

            val appName = state.selectedAppName.ifBlank { targetPackage }
            Toast.makeText(
                context,
                context.getString(R.string.test_executed, appName),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun refreshUsagePermission() {
        val context = getApplication<Application>()
        _uiState.update {
            it.copy(hasUsageStatsAccess = ForegroundDetector.hasUsageStatsPermission(context))
        }
    }
}
