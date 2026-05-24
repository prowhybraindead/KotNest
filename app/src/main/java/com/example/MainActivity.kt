package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.*
import com.example.core.notification.ReminderWorker
import com.example.core.sync.CloudSyncWorker
import com.example.feature.ai.AIChatScreen
import com.example.feature.calendar.CalendarScreen
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.payments.AddEditPaymentScreen
import com.example.feature.payments.PaymentDetailScreen
import com.example.feature.payments.PaymentsScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.rates.RatesScreen
import com.example.feature.report.MonthlyReportScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DueMateViewModel
import com.example.ui.viewmodel.DueMateViewModelFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val activityIntentState = mutableStateOf<Intent?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        activityIntentState.value = intent

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Sync ViewModel initialization with custom factory
        val viewModel: DueMateViewModel = ViewModelProvider(
            this, 
            DueMateViewModelFactory(applicationContext)
        )[DueMateViewModel::class.java]

        // Schedule daily background WorkManager due notifications
        scheduleDailyReminder()
        scheduleCloudAutoSync()
        CloudSyncWorker.enqueueNow(applicationContext)

        setContent {
            val settingsState by viewModel.settings.collectAsState()
            val themeMode = settingsState?.theme ?: "System"

            MyApplicationTheme(themeMode = themeMode) {
                if (settingsState != null) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Handle deep link intents (e.g. from notifications)
                    val activityIntent = activityIntentState.value
                    LaunchedEffect(activityIntent) {
                        val subId = activityIntent?.getIntExtra("subscription_id", -1) ?: -1
                        if (subId > 0) {
                            // Empty intent extras to avoid re-triggering on rotation/re-create
                            activityIntent?.removeExtra("subscription_id")
                            navController.navigate("detail/$subId")
                        }
                    }

                    // Bottom Navigation Items Definition
                    val tabs = listOf(
                        NavigationItem("dashboard", Icons.Default.Home, if (settingsState?.language == "vi") "Tổng quan" else "Dashboard", "tab_dashboard"),
                        NavigationItem("payments", Icons.Default.Star, if (settingsState?.language == "vi") "Thanh toán" else "Payments", "tab_payments"),
                        NavigationItem("calendar", Icons.Default.DateRange, if (settingsState?.language == "vi") "Lịch" else "Calendar", "tab_calendar"),
                        NavigationItem("settings", Icons.Default.Settings, if (settingsState?.language == "vi") "Cài đặt" else "Settings", "tab_settings")
                    )

                    // Should we display bottom navigation bar on current screen?
                    val showBottomBar = currentRoute in listOf("dashboard", "payments", "calendar", "settings")

                    val themeColors = com.example.ui.theme.LocalKotNestColors.current

                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(horizontal = 20.dp, vertical = 6.dp)
                                        .height(84.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // 1. Transparent floating backdrop container with shadow and custom borders
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .shadow(
                                                elevation = 12.dp,
                                                shape = RoundedCornerShape(28.dp),
                                                clip = false,
                                                ambientColor = if (themeColors.isLight) Color(0x1F000000) else Color(0x3D00F5FF),
                                                spotColor = if (themeColors.isLight) Color(0x290077B6) else Color(0x4D00F5FF)
                                            )
                                            .clip(RoundedCornerShape(28.dp))
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = if (themeColors.isLight) {
                                                        listOf(Color(0xE0FFFFFF), Color(0xC0F0F8FF))
                                                    } else {
                                                        listOf(Color(0xE00D1B2A), Color(0xC01B263B))
                                                    }
                                                )
                                            )
                                            .border(
                                                width = 1.2.dp,
                                                color = if (themeColors.isLight) themeColors.border else themeColors.primaryAqua.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(28.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left 2 items (Dashboard, Payments)
                                            val leftTabs = tabs.take(2)
                                            leftTabs.forEach { tab ->
                                                val isSelected = currentRoute == tab.route
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(28.dp))
                                                        .clickable {
                                                            if (currentRoute != tab.route) {
                                                                navController.navigate(tab.route) {
                                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                        }
                                                        .testTag(tab.testTag),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val iconColor = if (isSelected) {
                                                        if (themeColors.isLight) themeColors.deepAqua else themeColors.cyanAccent
                                                    } else {
                                                        themeColors.mutedText
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isSelected) {
                                                                    if (themeColors.isLight) themeColors.deepAqua.copy(alpha = 0.1f) else themeColors.primaryAqua.copy(alpha = 0.15f)
                                                                } else {
                                                                    Color.Transparent
                                                                }
                                                            )
                                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = tab.icon,
                                                            contentDescription = tab.label,
                                                            tint = iconColor,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Spacer in center to align with protruding Add button
                                            Spacer(modifier = Modifier.weight(1f))

                                            // Right 2 items (Calendar, Settings)
                                            val rightTabs = tabs.drop(2)
                                            rightTabs.forEach { tab ->
                                                val isSelected = currentRoute == tab.route
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(28.dp))
                                                        .clickable {
                                                            if (currentRoute != tab.route) {
                                                                navController.navigate(tab.route) {
                                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                        }
                                                        .testTag(tab.testTag),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val iconColor = if (isSelected) {
                                                        if (themeColors.isLight) themeColors.deepAqua else themeColors.cyanAccent
                                                    } else {
                                                        themeColors.mutedText
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(
                                                                if (isSelected) {
                                                                    if (themeColors.isLight) themeColors.deepAqua.copy(alpha = 0.1f) else themeColors.primaryAqua.copy(alpha = 0.15f)
                                                                } else {
                                                                    Color.Transparent
                                                                }
                                                            )
                                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = tab.icon,
                                                            contentDescription = tab.label,
                                                            tint = iconColor,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. Beautiful Protruding Add button (exactly centered above the bar spacer)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(bottom = 10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(62.dp)
                                                .shadow(
                                                    elevation = 14.dp,
                                                    shape = CircleShape,
                                                    clip = false,
                                                    ambientColor = Color(0x2E00E5FF),
                                                    spotColor = if (themeColors.isLight) Color(0xFF0077B6) else Color(0xFF00E5FF)
                                                )
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFF00E5FF), // Cyan/Aqua
                                                            Color(0xFF0077B6)  // Tech Blue
                                                        )
                                                    )
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    navController.navigate("add_edit/0")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Payment",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Tab 1: Dashboard
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddPayment = { navController.navigate("add_edit/0") },
                                    onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                    onNavigateToRates = { navController.navigate("rates") },
                                    onNavigateToReport = { navController.navigate("report") },
                                    onNavigateToAiChat = { navController.navigate("ai_chat") }
                                )
                            }

                            // Tab 2: Payments
                            composable("payments") {
                                PaymentsScreen(
                                    viewModel = viewModel,
                                    onNavigateToAddPayment = { navController.navigate("add_edit/0") },
                                    onNavigateToDetail = { id -> navController.navigate("detail/$id") }
                                )
                            }

                            // Tab 2.5: Rates
                            composable("report") {
                                MonthlyReportScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToDetail = { id -> navController.navigate("detail/$id") }
                                )
                            }

                            composable("rates") {
                                RatesScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("ai_chat") {
                                AIChatScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // Tab 3: Calendar
                            composable("calendar") {
                                CalendarScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { id -> navController.navigate("detail/$id") }
                                )
                            }

                            // Tab 4: Settings
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel
                                )
                            }

                            // Screen: Payment detail
                            composable(
                                route = "detail/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val subId = backStackEntry.arguments?.getInt("id") ?: 0
                                PaymentDetailScreen(
                                    viewModel = viewModel,
                                    subscriptionId = subId,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEdit = { id -> navController.navigate("add_edit/$id") }
                                )
                            }

                            // Screen: Add/Edit Payment Form
                            composable(
                                route = "add_edit/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val subId = backStackEntry.arguments?.getInt("id") ?: 0
                                AddEditPaymentScreen(
                                    viewModel = viewModel,
                                    subscriptionId = subId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                } else {
                    // Initial state DataStore splash loading state
                    val colors = com.example.ui.theme.LocalKotNestColors.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.backgroundGradientStart,
                                        colors.backgroundGradientEnd
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.primaryAqua
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activityIntentState.value = intent
    }

    private fun scheduleDailyReminder() {
        val repeatingRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DueMateDailyChecks",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }

    private fun scheduleCloudAutoSync() {
        CloudSyncWorker.schedulePeriodic(applicationContext)
    }
}

// Simple Bottom Bar item model helper
data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val testTag: String
)
