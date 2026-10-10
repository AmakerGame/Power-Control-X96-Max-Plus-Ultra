package com.eds.powercontrol.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Lightweight, high-performance TV screen for Android TV boxes (Amlogic / SlimBoxTV).
 * Stripped of heavy GPU rendering layers for 60fps responsive D-pad remote navigation.
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
    var filterCategory by remember { mutableStateOf("ALL") }

    val filteredApps = remember(installedApps, filterCategory) {
        when (filterCategory) {
            "USER" -> installedApps.filter { !it.isSystem }
            "SYSTEM" -> installedApps.filter { it.isSystem }
            else -> installedApps
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1318))
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Lightweight Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.header_app_selection),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Simple Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isServiceActive) Color(0xFF1B5E20) else Color(0xFF263238)
                    ) {
                        Text(
                            text = if (isServiceActive) "ROOT ДЕМОН: АКТИВНИЙ" else "ДЕМОН: ВИМКНЕНО",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF004D40)
                    ) {
                        Text(
                            text = "ROOT OK",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Mode Selector (3 Simple Fast TV Buttons)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val modes = listOf(
                    OperatingMode.NONE to stringResource(R.string.mode_none),
                    OperatingMode.SYSTEM to stringResource(R.string.mode_system),
                    OperatingMode.USER to stringResource(R.string.mode_user)
                )

                modes.forEach { (mode, title) ->
                    val isSelected = currentMode == mode
                    TvFocusableCard(
                        onClick = { onModeChanged(mode) },
                        isSelected = isSelected,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        testTag = "mode_${mode.name.lowercase()}"
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0xFF90A4AE),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Current Selection Info Card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF161E28),
                border = BorderStroke(1.dp, Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.target_package_intercepted),
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "➔ Запуск: " + (selectedAppName.ifBlank { selectedPackage.ifBlank { stringResource(R.string.no_app_selected) } }),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Category filter buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "Всі", "SYSTEM" to "Системні", "USER" to "Користувацькі").forEach { (cat, label) ->
                            val isCurrent = filterCategory == cat
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrent) Color(0xFF00B0FF) else Color(0xFF263238),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { filterCategory = cat }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isCurrent) Color(0xFF041424) else Color(0xFFCFD8DC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Lightweight Apps Grid (120dp height, fast horizontal scroll)
        item {
            if (isLoadingApps) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isChosen = app.packageName == selectedPackage
                        TvFocusableCard(
                            onClick = { onAppSelected(app) },
                            isSelected = isChosen,
                            modifier = Modifier
                                .width(170.dp)
                                .height(95.dp),
                            testTag = "app_${app.packageName}"
                        ) { isFocused ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
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
                                        size = 34.dp
                                    )
                                    if (isChosen) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF05111D),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.appName,
                                    color = if (isFocused) Color(0xFF00E5FF) else Color.White,
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
        }

        // 5. Actions Row (Зберегти & Тест закриття)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvFocusableButton(
                    text = stringResource(R.string.btn_save),
                    onClick = onSaveSettings,
                    icon = Icons.Default.Save,
                    isPrimary = true,
                    testTag = "save_btn"
                )

                TvFocusableButton(
                    text = stringResource(R.string.test_interception_btn),
                    onClick = onTestInterception,
                    icon = Icons.Default.PlayArrow,
                    isPrimary = false,
                    testTag = "test_btn"
                )
            }
        }

        // 6. Compact TV Ad Banner
        item {
            TvAdBanner()
        }

        // 7. Footer: Static Text «Збірка: X96 Max Plus Ultra(SlimBoxTV)»
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stringResource(R.string.build_label)}: ${stringResource(R.string.build_value)}",
                    color = Color(0xFF78909C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
