package com.example.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.DateUtils
import com.example.domain.model.Category
import com.example.domain.model.Subscription
import com.example.ui.theme.*
import com.example.ui.viewmodel.DueMateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DueMateViewModel,
    onNavigateToAddPayment: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToRates: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allSubscriptions by viewModel.subscriptions.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val aiState by viewModel.aiInsightUiState.collectAsState()
    val backendStatus by viewModel.backendStatusUiState.collectAsState()
    
    val categoryMap = categories.associateBy { it.id }
    val colors = LocalKotNestColors.current

    // Derive due today status values dynamically
    val dueTodayCount = allSubscriptions.count { it.status == "Due Today" && !it.isPaused }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundGradientStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_kotnest_logo),
                            contentDescription = "KotNest Icon",
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "WELCOME TO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.mutedText,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "KotNest",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.primaryText,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }

                    val backendDotColor = when {
                        backendStatus.isChecking -> colors.warning
                        backendStatus.isOnline -> colors.success
                        else -> colors.danger
                    }
                    val backendLabel = when {
                        backendStatus.isChecking -> "Checking..."
                        backendStatus.isOnline -> "Active Mode"
                        else -> "Offline Mode"
                    }

                    // Elegant status tracker indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.glassWhite)
                            .border(1.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(backendDotColor, CircleShape)
                            )
                            Text(
                                text = backendLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            if (backendStatus.latencyMs != null && backendStatus.isOnline) {
                                Text(
                                    text = "${backendStatus.latencyMs}ms",
                                    fontSize = 10.sp,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Total Summary Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToReport() }
                        .border(
                            width = if (colors.isLight) 1.5.dp else 1.dp,
                            color = if (colors.isLight) colors.border else colors.cyanAccent.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TOTAL AMOUNT DUE THIS MONTH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.mutedText,
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format("%,.0f", metrics.currentMonthTotal),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.primaryText,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VND",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        
                        if (metrics.hasForeignCurrency && settingsState?.showEstimatedVnd != false) {
                            Text(
                                text = if (settingsState?.language == "vi") "*(gồm ngoại tệ đã quy đổi)*" else "*(includes converted foreign currencies)*",
                                fontSize = 11.sp,
                                color = colors.mutedText,
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Beautiful contrast divider
                        Box(
                            modifier = Modifier
                                .padding(vertical = 18.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border)
                        )
                        
                        // Sub Status Card grid with proper alignment & contrast
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Upcoming
                            Column {
                                Text(
                                    "UPCOMING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${metrics.upcomingCount} dues",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAqua
                                )
                            }

                            // Due Today
                            Column {
                                Text(
                                    "DUE TODAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$dueTodayCount dues",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.warning
                                )
                            }

                            // Overdue
                            Column {
                                Text(
                                    "OVERDUE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${metrics.overdueCount} dues",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.danger
                                )
                            }
                        }
                    }
                }
            }

            // 2. Quick Actions
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickActionButton(
                                glyph = QuickActionGlyph.AddDue,
                                label = if (viewModel.settings.value?.language == "vi") "Them han" else "Add Dues",
                                containerColor = colors.primaryAqua.copy(alpha = 0.12f),
                                contentColor = colors.primaryAqua,
                                onClick = onNavigateToAddPayment
                            )
                            QuickActionButton(
                                glyph = QuickActionGlyph.Report,
                                label = if (viewModel.settings.value?.language == "vi") "Bao cao" else "Report",
                                containerColor = colors.success.copy(alpha = 0.12f),
                                contentColor = colors.success,
                                onClick = onNavigateToReport
                            )
                            QuickActionButton(
                                glyph = QuickActionGlyph.Rates,
                                label = if (viewModel.settings.value?.language == "vi") "Ty gia" else "Rates",
                                containerColor = colors.deepAqua.copy(alpha = 0.12f),
                                contentColor = colors.deepAqua,
                                onClick = onNavigateToRates
                            )
                            QuickActionButton(
                                glyph = QuickActionGlyph.Overdue,
                                label = if (viewModel.settings.value?.language == "vi") "Qua han" else "Overdue",
                                containerColor = colors.danger.copy(alpha = 0.12f),
                                contentColor = colors.danger,
                                onClick = {
                                    viewModel.selectedSubFilter.value = "Overdue"
                                }
                            )
                            QuickActionButton(
                                glyph = QuickActionGlyph.AiChat,
                                label = "AI Chat",
                                containerColor = colors.cyanAccent.copy(alpha = 0.12f),
                                contentColor = colors.cyanAccent,
                                onClick = onNavigateToAiChat
                            )
                        }
                    }
                }
            }

            // 3. Closest Renewal Card Option
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = aiState.brandName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI SPENDING INSIGHT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mutedText,
                                    letterSpacing = 1.1.sp
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.refreshBackendHealth()
                                        viewModel.fetchAiInsights()
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = colors.glassWhite,
                                        contentColor = colors.primaryAqua
                                    )
                                ) {
                                    if (aiState.isLoading) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp),
                                            color = colors.primaryAqua
                                        )
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh AI insight")
                                    }
                                }
                            }

                            Text(
                                text = aiState.insight,
                                fontSize = 13.sp,
                                color = colors.primaryText,
                                lineHeight = 20.sp
                            )

                            aiState.actions.take(3).forEach { action ->
                                Text(
                                    text = "- $action",
                                    fontSize = 12.sp,
                                    color = colors.secondaryText
                                )
                            }

                            if (aiState.filtered) {
                                Text(
                                    text = "Filtered by backend safety policy",
                                    fontSize = 11.sp,
                                    color = colors.warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!aiState.errorMessage.isNullOrBlank()) {
                                Text(
                                    text = aiState.errorMessage!!,
                                    fontSize = 11.sp,
                                    color = colors.mutedText
                                )
                            }
                        }
                    }
                }
            }

            // 4. Closest Renewal Card Option
            if (metrics.closestUpcoming != null) {
                item {
                    val sub = metrics.closestUpcoming!!
                    val category = categoryMap[sub.categoryId]
                    
                    Text(
                        text = "CLOSEST NEXT RENEWAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SubscriptionRow(
                        subscription = sub,
                        category = category,
                        onClick = { onNavigateToDetail(sub.id) },
                        onMarkAsPaid = { viewModel.markAsPaid(sub.id) }
                    )
                }
            }

            // 5. Due in next 7 days list section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPCOMING (NEXT 7 DAYS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "See all (${metrics.dueNextSevenDays.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.deepAqua
                    )
                }
            }

            if (metrics.dueNextSevenDays.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No subscription payments in 7 days.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.mutedText
                            )
                        }
                    }
                }
            } else {
                items(metrics.dueNextSevenDays, key = { it.id }) { sub ->
                    val category = categoryMap[sub.categoryId]
                    SubscriptionRow(
                        subscription = sub,
                        category = category,
                        onClick = { onNavigateToDetail(sub.id) },
                        onMarkAsPaid = { viewModel.markAsPaid(sub.id) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    glyph: QuickActionGlyph,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalKotNestColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(containerColor, RoundedCornerShape(16.dp))
                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            QuickActionGlyphIcon(
                glyph = glyph,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText
        )
    }
}

enum class QuickActionGlyph {
    AddDue,
    Report,
    Rates,
    Overdue,
    AiChat,
}

@Composable
fun QuickActionGlyphIcon(
    glyph: QuickActionGlyph,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = size.minDimension * 0.12f)

        when (glyph) {
            QuickActionGlyph.AddDue -> {
                val cx = w / 2f
                val cy = h / 2f
                drawCircle(color = tint, radius = size.minDimension * 0.46f, style = Stroke(width = size.minDimension * 0.1f))
                drawLine(tint, androidx.compose.ui.geometry.Offset(cx, h * 0.24f), androidx.compose.ui.geometry.Offset(cx, h * 0.76f), strokeWidth = size.minDimension * 0.12f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.24f, cy), androidx.compose.ui.geometry.Offset(w * 0.76f, cy), strokeWidth = size.minDimension * 0.12f)
            }
            QuickActionGlyph.Report -> {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.8f), androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.8f), strokeWidth = size.minDimension * 0.1f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.5f), strokeWidth = size.minDimension * 0.12f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.34f), strokeWidth = size.minDimension * 0.12f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.2f), strokeWidth = size.minDimension * 0.12f)
            }
            QuickActionGlyph.Rates -> {
                val points = listOf(
                    androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.7f),
                    androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.54f),
                    androidx.compose.ui.geometry.Offset(w * 0.56f, h * 0.62f),
                    androidx.compose.ui.geometry.Offset(w * 0.82f, h * 0.28f),
                )
                for (i in 0 until points.lastIndex) {
                    drawLine(tint, points[i], points[i + 1], strokeWidth = size.minDimension * 0.1f)
                }
                points.forEach { drawCircle(tint, radius = size.minDimension * 0.08f, center = it) }
            }
            QuickActionGlyph.Overdue -> {
                drawCircle(color = tint, radius = size.minDimension * 0.44f, style = Stroke(width = size.minDimension * 0.1f))
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.25f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f),
                    strokeWidth = size.minDimension * 0.1f
                )
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.7f, h * 0.66f),
                    strokeWidth = size.minDimension * 0.1f
                )
            }
            QuickActionGlyph.AiChat -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f),
                    style = stroke
                )
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.66f), strokeWidth = size.minDimension * 0.09f)
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.42f), androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.44f), strokeWidth = size.minDimension * 0.09f)
                drawCircle(tint, radius = size.minDimension * 0.04f, center = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.32f))
                drawCircle(tint, radius = size.minDimension * 0.04f, center = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.32f))
            }
        }
    }
}

@Composable
fun SubscriptionRow(
    subscription: Subscription,
    category: Category?,
    onClick: () -> Unit,
    onMarkAsPaid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKotNestColors.current
    val categoryColor = parseColor(category?.color ?: "#4CAF50")
    val statusColor = when (subscription.status) {
        "Overdue" -> colors.danger
        "Due Today" -> colors.warning
        "Paid" -> colors.success
        else -> colors.primaryAqua
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant status colored left stripe matching guidelines
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category circular indicator block
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(categoryColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val emojiText = category?.icon ?: "CARD"
                    Text(
                        text = emojiText,
                        fontSize = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateUtils.formatEpoch(subscription.nextDueDate),
                        fontSize = 12.sp,
                        color = colors.secondaryText
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = String.format("%,.0f", subscription.amount) + " " + subscription.currency,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(subscription.status)
                }
                
                IconButton(
                    onClick = onMarkAsPaid,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.glassWhite,
                        contentColor = colors.success
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .testTag("row_paid_button_${subscription.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Paid",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val colors = LocalKotNestColors.current
    val (bgColor, textColor) = when (status) {
        "Paid" -> colors.paidBackground to colors.success
        "Due Today" -> colors.dueTodayBackground to colors.warning
        "Overdue" -> colors.overdueBackground to colors.danger
        "Paused" -> if (colors.isLight) Color(0xFFECEFF1) to Color(0xFF78909C) else Color(0x28B0BEC5) to Color(0xFFB0BEC5)
        else -> colors.upcomingBackground to colors.primaryAqua
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun parseColor(colorStr: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        Color.Gray
    }
}


