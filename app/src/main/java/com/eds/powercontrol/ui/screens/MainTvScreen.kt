package com.eds.powercontrol.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eds.powercontrol.R
import com.eds.powercontrol.model.AppInfo
import com.eds.powercontrol.model.OperatingMode
import com.eds.powercontrol.ui.components.AppIconImage
import com.eds.powercontrol.ui.components.TvAdBanner
import com.eds.powercontrol.ui.components.TvFocusableButton
import com.eds.powercontrol.ui.components.TvFocusableCard

/**
 * Main Android TV screen for Power Control app.
 * Allows TV remote D-Pad navigation to configure intercepted launch target on SlimBoxTV.
 */
@Composable
fun MainTvScreen(
    installedApps: List<AppInfo>,
    isLoadingApps: Boolean,
    currentMode: OperatingMode,
    selectedPackage: String,
    selectedAppName: String,
    isServiceActive: Boolean,
    hasUsageStatsAccess: Boolean,
    onModeChanged: (OperatingMode) -> Unit,
    onAppSelected: (AppInfo) -> Unit,
    onSaveSettings: () -> Unit,
    onTestInterception: () -> Unit,
    onGrantUsageAccess: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("ALL") } // ALL, USER, SYSTEM

    val filteredApps = remember(installedApps, searchQuery, filterCategory) {
        installedApps.filter { app ->
            val matchesCategory = when (filterCategory) {
                "USER" -> !app.isSystem
                "SYSTEM" -> app.isSystem
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with title & active badges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        // Title: "Вибір програми" / "App selection"
                        Text(
                            text = stringResource(R.string.header_app_selection),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 26.sp
                        )
                        Text(
                            text = stringResource(R.string.target_package_intercepted),
                            color = Color(0xFF81D4FA),
                            fontSize = 13.sp
                        )
                    }
                }

                // Service Status Indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isServiceActive) Color(0xFF1B5E20) else Color(0xFF263238),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isServiceActive) Color(0xFF00E676) else Color(0xFF78909C)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isServiceActive) Color(0xFF00E676) else Color(0xFFB0BEC5))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceActive) {
                                stringResource(
                                    R.string.status_monitoring_active,
                                    selectedAppName.ifBlank { selectedPackage },
                                    currentMode.name
                                )
                            } else {
                                stringResource(R.string.status_monitoring_inactive)
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Mode Selector: Spinner / 3 Options («Нічого», «Системна», «Користувацька»)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.label_mode_selection),
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val modes = listOf(
                        OperatingMode.NONE to stringResource(R.string.mode_none),
                        OperatingMode.SYSTEM to stringResource(R.string.mode_system),
                        OperatingMode.USER to stringResource(R.string.mode_user)
                    )

                    modes.forEach { (mode, label) ->
                        val isCurrentMode = currentMode == mode
                        TvFocusableCard(
                            onClick = { onModeChanged(mode) },
                            isSelected = isCurrentMode,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            testTag = "mode_chip_${mode.name.lowercase()}"
                        ) { isFocused ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isCurrentMode) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isFocused) Color(0xFF00E5FF) else Color(0xFF29B6F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = label,
                                    color = if (isCurrentMode) Color.White else Color(0xFFB0BEC5),
                                    fontWeight = if (isCurrentMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Category filters & Search input
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Filter Pills
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val filters = listOf(
                        "ALL" to stringResource(R.string.filter_all),
                        "SYSTEM" to stringResource(R.string.mode_system),
                        "USER" to stringResource(R.string.mode_user)
                    )
                    filters.forEach { (key, title) ->
                        val isSelected = filterCategory == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF0288D1) else Color(0xFF1E2836),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { filterCategory = key }
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Active App selection status pill
                if (selectedPackage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16212D),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B0FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.selected_app_label)} ",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp
                            )
                            Text(
                                text = selectedAppName.ifBlank { selectedPackage },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Horizontal App Grid / Carousel (TV D-Pad optimized)
        item {
            if (isLoadingApps) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_app_selected),
                        color = Color(0xFF78909C),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isChosen = app.packageName == selectedPackage
                        TvFocusableCard(
                            onClick = { onAppSelected(app) },
                            isSelected = isChosen,
                            modifier = Modifier
                                .width(200.dp)
                                .height(130.dp),
                            testTag = "app_card_${app.packageName}"
                        ) { isFocused ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconImage(
                                        drawable = app.icon,
                                        contentDescription = app.appName,
                                        size = 44.dp
                                    )

                                    if (isChosen) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF05111D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF263238)
                                        ) {
                                            Text(
                                                text = if (app.isSystem) stringResource(R.string.mode_system) else stringResource(R.string.mode_user),
                                                color = Color(0xFF90A4AE),
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = app.appName,
                                        color = if (isFocused) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = Color(0xFF78909C),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Action Controls Row: Save Button, Test Button, Usage Access
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button: «Зберегти»
                TvFocusableButton(
                    text = stringResource(R.string.btn_save),
                    onClick = onSaveSettings,
                    icon = Icons.Default.Save,
                    isPrimary = true,
                    testTag = "save_settings_button"
                )

                // Test Button: «Тест перехоплення»
                TvFocusableButton(
                    text = stringResource(R.string.test_interception_btn),
                    onClick = onTestInterception,
                    icon = Icons.Default.PlayArrow,
                    isPrimary = false,
                    testTag = "test_interception_button"
                )

                // Usage stats access request button if not granted
                if (!hasUsageStatsAccess) {
                    TvFocusableButton(
                        text = stringResource(R.string.grant_usage_stats_btn),
                        onClick = onGrantUsageAccess,
                        icon = Icons.Default.Security,
                        isPrimary = false,
                        testTag = "grant_usage_stats_button"
                    )
                }
            }
        }

        // 6. TV Ad Banner block (Horizontal Ad slot / AdMob / custom partner container)
        item {
            TvAdBanner()
        }

        // 7. Footer: Static Text «Збірка» / «X96 Max Plus Ultra(SlimBoxTV)»
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.build_label),
                        color = Color(0xFF90A4AE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.build_value),
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
