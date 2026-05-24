package com.example.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.database.AppDatabase
import com.example.ui.viewmodel.DueMateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DueMateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsState by viewModel.settings.collectAsState()
    
    val currentSettings = settingsState
    val db = remember { AppDatabase.getDatabase(context, @Suppress("OPT_IN_USAGE") kotlinx.coroutines.GlobalScope) }
    val colors = LocalKotNestColors.current

    var showImportSummaryDialog by remember { mutableStateOf(false) }
    var importResultState by remember { mutableStateOf<com.example.core.util.ImportResult?>(null) }

    // Launcher for JSON file importing
    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importBackupJson(inputStream, db) { result ->
                        if (result != null) {
                            importResultState = result
                            showImportSummaryDialog = true
                        } else {
                            Toast.makeText(context, "Failed to restore JSON. Please verify backup schema.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Could not open selected backup file.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold, color = colors.primaryText) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundGradientStart
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (currentSettings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.deepAqua)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                
                // 0. App Info / Core Branding Hero Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.glassWhite,
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.5.dp, colors.border, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_kotnest_icon),
                                    contentDescription = "KotNest Icon",
                                    tint = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "KotNest",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Version 1.0.0 (Premium Release)",
                                fontSize = 11.sp,
                                color = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Secure • Offline-First • Private",
                                fontSize = 11.sp,
                                color = colors.secondaryText
                            )
                        }
                    }
                }

                // 1. General Preferences Card
                Text(
                    text = "General Preferences",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.2.sp
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Default Currency
                        var currencyTemp by remember { mutableStateOf(currentSettings.defaultCurrency) }
                        OutlinedTextField(
                            value = currencyTemp,
                            onValueChange = {
                                currencyTemp = it.uppercase()
                                if (it.isNotBlank()) {
                                    viewModel.setDefaultCurrency(it.uppercase())
                                }
                            },
                            label = { Text("Default Currency") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedLabelColor = colors.primaryAqua,
                                unfocusedLabelColor = colors.mutedText,
                                focusedBorderColor = colors.primaryAqua,
                                unfocusedBorderColor = colors.border,
                                cursorColor = colors.primaryAqua
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_currency_input"),
                            singleLine = true
                        )

                        // Theme Selection
                        Column {
                            Text(
                                text = "Theme Mode",
                                fontSize = 12.sp,
                                color = colors.secondaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Light", "Dark", "System").forEach { t ->
                                    val isSelected = currentSettings.theme == t
                                    ElevatedFilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setTheme(t) },
                                        label = { Text(t, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = colors.deepAqua,
                                            selectedLabelColor = Color.White,
                                            containerColor = colors.surface,
                                            labelColor = colors.mutedText
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = colors.border,
                                            selectedBorderColor = colors.deepAqua
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_theme_chip_$t")
                                    )
                                }
                            }
                        }

                        // Smart Suggestions switch
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentSettings.language == "vi") "Gợi ý thông minh" else "Smart Suggestions",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = if (currentSettings.language == "vi") "Ví dụ: nhập 'Spotify' tự điền Giải trí, Hàng tháng, Quan trọng trung bình." else "E.g., typing 'Spotify' defaults to Entertainment, Monthly, Medium.",
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Switch(
                                checked = currentSettings.smartSuggestionsEnabled,
                                onCheckedChange = { viewModel.setSmartSuggestionsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primaryAqua,
                                    uncheckedThumbColor = colors.mutedText,
                                    uncheckedTrackColor = colors.border
                                ),
                                modifier = Modifier.testTag("settings_smart_suggestions_switch")
                            )
                        }
                    }
                }

                // 2. Notifications Card
                Text(
                    text = if (currentSettings.language == "vi") "Lời nhắc & Thông báo" else "Reminders & Notifications",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.2.sp
                )

                val areNotificationsEnabledOnDevice = remember {
                    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                }

                if (!areNotificationsEnabledOnDevice) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.danger.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.danger.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 18.sp)
                                Text(
                                    text = if (currentSettings.language == "vi") "Thông báo đang bị tắt!" else "Notifications are disabled!",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.danger,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = if (currentSettings.language == "vi") "Hãy bật thông báo để nhận lời nhắc hoá đơn sắp hạn và quá hạn nhanh chóng và chuẩn xác nhất." else "Enable notifications in Android Settings to receive timely reminders about upcoming dues.",
                                fontSize = 12.sp,
                                color = colors.secondaryText
                            )
                            Button(
                                onClick = {
                                    val intent = Intent().apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        } else {
                                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                            putExtra("app_package", context.packageName)
                                            putExtra("app_uid", context.applicationInfo.uid)
                                        }
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = if (currentSettings.language == "vi") "Ví bật Cài đặt" else "Open Settings",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Enable/Disable Switch
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentSettings.language == "vi") "Lời nhắc Thanh toán" else "Payment Reminders", 
                                    fontWeight = FontWeight.Bold, 
                                    color = colors.primaryText
                                )
                                Text(
                                    text = if (currentSettings.language == "vi") "Nhận cảnh báo hàng ngày về các hóa đơn sắp đến hạn." else "Send daily alerts on upcoming and overdue bills.", 
                                    fontSize = 11.sp, 
                                    color = colors.secondaryText
                                )
                            }
                            Switch(
                                checked = currentSettings.notificationEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primaryAqua,
                                    uncheckedThumbColor = colors.mutedText,
                                    uncheckedTrackColor = colors.border
                                ),
                                modifier = Modifier.testTag("settings_reminder_switch")
                            )
                        }

                        if (currentSettings.notificationEnabled) {
                            Divider(color = colors.border.copy(alpha = 0.5f))

                            // Daily Summary Switch
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentSettings.language == "vi") "Tóm tắt hàng ngày" else "Daily Summary Bulletin", 
                                        fontWeight = FontWeight.Bold, 
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = if (currentSettings.language == "vi") "Tóm tắt toàn bộ hóa đơn trong ngày trong cùng một thông báo." else "Show a single unified overview notification of morning dues.", 
                                        fontSize = 11.sp, 
                                        color = colors.secondaryText
                                    )
                                }
                                Switch(
                                    checked = currentSettings.dailySummaryEnabled,
                                    onCheckedChange = { viewModel.setDailySummaryEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.primaryAqua,
                                        uncheckedThumbColor = colors.mutedText,
                                        uncheckedTrackColor = colors.border
                                    ),
                                    modifier = Modifier.testTag("settings_daily_summary_switch")
                                )
                            }

                            Divider(color = colors.border.copy(alpha = 0.5f))

                            // Overdue Reminders Switch
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentSettings.language == "vi") "Cảnh báo quá hạn" else "Overdue Bill Reminders", 
                                        fontWeight = FontWeight.Bold, 
                                        color = colors.primaryText
                                    )
                                    Text(
                                        text = if (currentSettings.language == "vi") "Tiếp tục thông báo các hóa đơn quá hạn một ngày một lần." else "Remind about unresolved past due payments once daily.", 
                                        fontSize = 11.sp, 
                                        color = colors.secondaryText
                                    )
                                }
                                Switch(
                                    checked = currentSettings.overdueRemindersEnabled,
                                    onCheckedChange = { viewModel.setOverdueRemindersEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = colors.primaryAqua,
                                        uncheckedThumbColor = colors.mutedText,
                                        uncheckedTrackColor = colors.border
                                    ),
                                    modifier = Modifier.testTag("settings_overdue_switch")
                                )
                            }

                            Divider(color = colors.border.copy(alpha = 0.5f))

                            // Default Reminder Time
                            var reminderTimeTemp by remember { mutableStateOf(currentSettings.defaultReminderTime) }
                            OutlinedTextField(
                                value = reminderTimeTemp,
                                onValueChange = {
                                    reminderTimeTemp = it
                                    if (it.matches(Regex("\\d{2}:\\d{2}"))) {
                                        viewModel.setDefaultReminderTime(it)
                                    }
                                },
                                label = { Text(if (currentSettings.language == "vi") "Thời gian thông báo (Giờ:Phút)" else "Default Reminder Time (HH:mm)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.primaryText,
                                    unfocusedTextColor = colors.primaryText,
                                    focusedLabelColor = colors.primaryAqua,
                                    unfocusedLabelColor = colors.mutedText,
                                    focusedBorderColor = colors.primaryAqua,
                                    unfocusedBorderColor = colors.border,
                                    cursorColor = colors.primaryAqua
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Default Reminder Days
                            Column {
                                Text(
                                    text = if (currentSettings.language == "vi") "Nhắc trước mặc định: ${currentSettings.defaultReminderDays} ngày" else "Default Days Before Reminder: ${currentSettings.defaultReminderDays} days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = currentSettings.defaultReminderDays.toFloat(),
                                    onValueChange = { viewModel.setDefaultReminderDays(it.toInt()) },
                                    valueRange = 0f..7f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                        activeTrackColor = colors.primaryAqua,
                                        inactiveTrackColor = colors.border
                                    )
                                )
                            }
                        }
                    }
                }

                // 2.5 Exchange Rates Configuration Card
                Text(
                    text = if (currentSettings.language == "vi") "Tỷ giá Ngoại tệ" else "Exchange Rates & Tỷ giá",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.2.sp
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Preferred Target Currency
                        var targetCurrencyTemp by remember { mutableStateOf(currentSettings.preferredTargetCurrency) }
                        OutlinedTextField(
                            value = targetCurrencyTemp,
                            onValueChange = {
                                targetCurrencyTemp = it.uppercase()
                                if (it.isNotBlank()) {
                                    viewModel.updatePreferredTargetCurrency(it.uppercase())
                                }
                            },
                            label = { Text(if (currentSettings.language == "vi") "Đồng tiền Đích ưu thích" else "Preferred Target Currency") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedLabelColor = colors.primaryAqua,
                                unfocusedLabelColor = colors.mutedText,
                                focusedBorderColor = colors.primaryAqua,
                                unfocusedBorderColor = colors.border,
                                cursorColor = colors.primaryAqua
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("settings_target_currency_input"),
                            singleLine = true
                        )

                        // Auto-refresh switch
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentSettings.language == "vi") "Tự động làm mới tỷ giá" else "Auto Refresh Rates",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = if (currentSettings.language == "vi") "Tự động cập nhật tỷ giá lưu tạm khi đã lỗi thời." else "Automatically update cached rates when they become stale.",
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Switch(
                                checked = currentSettings.autoRefreshRates,
                                onCheckedChange = { viewModel.updateAutoRefreshRates(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primaryAqua,
                                    uncheckedThumbColor = colors.mutedText,
                                    uncheckedTrackColor = colors.border
                                ),
                                modifier = Modifier.testTag("settings_auto_refresh_switch")
                            )
                        }

                        // Refresh Interval
                        Column {
                            Text(
                                text = if (currentSettings.language == "vi") "Chu kỳ làm mới: ${currentSettings.refreshIntervalHours} giờ" else "Refresh Interval: ${currentSettings.refreshIntervalHours} hours",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = currentSettings.refreshIntervalHours.toFloat(),
                                onValueChange = { viewModel.updateRefreshIntervalHours(it.toInt()) },
                                valueRange = 1f..24f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                    activeTrackColor = colors.primaryAqua,
                                    inactiveTrackColor = colors.border
                                ),
                                modifier = Modifier.testTag("settings_refresh_interval_slider")
                            )
                        }

                        // API Provider Picker Selection
                        Column {
                            Text(
                                text = if (currentSettings.language == "vi") "Nguồn dữ liệu Tỷ giá" else "Exchange Rate Provider",
                                fontSize = 12.sp,
                                color = colors.secondaryText,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val providers = listOf("ExchangeRate-API Open Access", "Frankfurter API", "Custom Backend API (Placeholder)")
                                items(providers) { provider ->
                                    val isSelected = currentSettings.exchangeRateProviderName == provider
                                    ElevatedFilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateExchangeRateProviderName(provider) },
                                        label = { Text(provider, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = colors.deepAqua,
                                            selectedLabelColor = Color.White,
                                            containerColor = colors.surface,
                                            labelColor = colors.mutedText
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = colors.border,
                                            selectedBorderColor = colors.deepAqua
                                        ),
                                        modifier = Modifier.testTag("settings_provider_chip_$provider")
                                    )
                                }
                            }
                        }

                        // Show Estimated VND switch
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentSettings.language == "vi") "Hiện Ước lượng VND" else "Show Estimated VND",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryText
                                )
                                Text(
                                    text = if (currentSettings.language == "vi") "Chuyển đổi ngoại tệ sang VND mẫu trên các giao diện tổng quát." else "Show converted estimated VND amounts in main summary views for foreign currency dues.",
                                    fontSize = 11.sp,
                                    color = colors.secondaryText
                                )
                            }
                            Switch(
                                checked = currentSettings.showEstimatedVnd,
                                onCheckedChange = { viewModel.setShowEstimatedVnd(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.primaryAqua,
                                    uncheckedThumbColor = colors.mutedText,
                                    uncheckedTrackColor = colors.border
                                ),
                                modifier = Modifier.testTag("settings_show_estimated_vnd_switch")
                            )
                        }
                    }
                }

                // 3. Backup & Data Sync Settings
                Text(
                    text = "Backup & Local Exports",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.2.sp
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Calculate backup status
                        val isVietnamese = currentSettings.language == "vi"
                        val neverBackedUp = currentSettings.lastBackupAt == 0L
                        val daysSinceLastBase = if (neverBackedUp) 999 else (System.currentTimeMillis() - currentSettings.lastBackupAt) / (1000 * 60 * 60 * 24)
                        val statusText = when {
                            neverBackedUp -> if (isVietnamese) "Chưa bao giờ sao lưu" else "Never Backed Up"
                            daysSinceLastBase >= 7 -> if (isVietnamese) "Khuyến nghị sao lưu" else "Backup Recommended"
                            else -> if (isVietnamese) "Lần sao lưu tốt • An toàn" else "Backup Status: Good"
                        }
                        val statusColor = when {
                            neverBackedUp -> colors.danger
                            daysSinceLastBase >= 7 -> colors.warning
                            else -> colors.primaryAqua
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.primaryText
                                    )
                                }
                                
                                Text(
                                    text = (if (isVietnamese) "Lần sao lưu cuối: " else "Last Backup: ") + formatMetadataTime(currentSettings.lastBackupAt, isVietnamese),
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                                if (currentSettings.lastBackupAt > 0L) {
                                    Text(
                                        text = (if (isVietnamese) "Tên tệp: " else "File: ") + currentSettings.lastBackupFileName,
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                    Text(
                                        text = (if (isVietnamese) "Số khoản mục đã sao lưu: " else "Dues Backed Up: ") + "${currentSettings.lastBackupItemCount} dues",
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border)
                                )
                                Text(
                                    text = (if (isVietnamese) "Lần khôi phục cuối: " else "Last Import: ") + formatMetadataTime(currentSettings.lastImportAt, isVietnamese),
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { 
                                viewModel.exportBackupJson(context, db) { success ->
                                    if (success) {
                                        Toast.makeText(context, if (isVietnamese) "Sao lưu đã xuất thành công! 🎉" else "Backup exported successfully! 🎉", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isVietnamese) "Xuất thất bại." else "Export failed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.deepAqua),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_export_json")
                        ) {
                            Text(if (isVietnamese) "Xuất sao lưu định dạng JSON" else "Export Backup to JSON", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { jsonImportLauncher.launch("application/json") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_import_json"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.glassWhite,
                                contentColor = colors.primaryText
                            )
                        ) {
                            Text(if (isVietnamese) "Nhập sao lưu từ JSON" else "Import Backup from JSON", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportBackupCsv(context, db) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                            border = BorderStroke(1.5.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_export_csv")
                        ) {
                            Text(if (isVietnamese) "Xuất Báo cáo Excel/CSV" else "Export Report to CSV Spreadsheet", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Future Sync configuration placeholder
                Text(
                    text = "Privacy & Synchronisation",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 1.2.sp
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "KotNest is designed completely secure and offline-first. Rest assured your subscription dates, payment histories, and private notes never leave your local workspace.",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border)
                        )

                        var syncUrlTemp by remember { mutableStateOf(currentSettings.backendBaseUrl) }
                        OutlinedTextField(
                            value = syncUrlTemp,
                            onValueChange = {
                                syncUrlTemp = it
                                viewModel.setBackendBaseUrl(it)
                            },
                            label = { Text("Future Sync Server (Base URL)") },
                            placeholder = { Text("http://192.168.1.100:3000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.primaryText,
                                unfocusedTextColor = colors.primaryText,
                                focusedLabelColor = colors.primaryAqua,
                                unfocusedLabelColor = colors.mutedText,
                                focusedBorderColor = colors.primaryAqua,
                                unfocusedBorderColor = colors.border,
                                cursorColor = colors.primaryAqua
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                if (showImportSummaryDialog && importResultState != null) {
                    val result = importResultState!!
                    AlertDialog(
                        onDismissRequest = { showImportSummaryDialog = false },
                        title = { Text(if (currentSettings.language == "vi") "Đã khôi phục thành công! 🎉" else "Import Successful! 🎉", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (currentSettings.language == "vi") {
                                        "Hệ thống đã sáp nhập thành công các danh mục và khoản mục vào bộ nhớ cục bộ:"
                                    } else {
                                        "Successfully imported and merged database backup file into local store:"
                                    },
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                                
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Dues Connections / Khoản mục:", fontSize = 13.sp, color = colors.mutedText)
                                    Text("${result.subscriptionsCount}", fontWeight = FontWeight.Bold, color = colors.primaryAqua)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("New Categories / Danh mục mới:", fontSize = 13.sp, color = colors.mutedText)
                                    Text("${result.categoriesCount}", fontWeight = FontWeight.Bold, color = colors.primaryAqua)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Payment Methods / Phương thức thanh toán:", fontSize = 13.sp, color = colors.mutedText)
                                    Text("${result.paymentMethodsCount}", fontWeight = FontWeight.Bold, color = colors.primaryAqua)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Payment History / Lịch sử thanh toán:", fontSize = 13.sp, color = colors.mutedText)
                                    Text("${result.historiesCount}", fontWeight = FontWeight.Bold, color = colors.primaryAqua)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showImportSummaryDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAqua, contentColor = colors.deepAqua)
                            ) {
                                Text("OK", fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = colors.surface
                    )
                }

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

private fun formatMetadataTime(epoch: Long, isVietnamese: Boolean): String {
    if (epoch == 0L) return if (isVietnamese) "Chưa bao giờ" else "Never"
    val diff = System.currentTimeMillis() - epoch
    if (diff < 60000) return if (isVietnamese) "Vừa xong" else "Just now"
    val minutes = diff / 60000
    if (minutes < 60) return if (isVietnamese) "$minutes phút trước" else "$minutes min ago"
    val hours = minutes / 60
    if (hours < 24) return if (isVietnamese) "$hours giờ trước" else "$hours hours ago"
    return com.example.core.util.DateUtils.formatEpoch(epoch, "yyyy-MM-dd HH:mm")
}
