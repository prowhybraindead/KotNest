package com.example.feature.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.util.DateUtils
import com.example.domain.model.Category
import com.example.domain.model.Subscription
import com.example.domain.model.PaymentHistory
import com.example.ui.theme.LocalKotNestColors
import com.example.ui.viewmodel.DueMateViewModel
import com.example.domain.usecase.ConvertAmountToVndUseCase
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: DueMateViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { com.example.core.database.AppDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope) }
    
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val histories by viewModel.historyList.collectAsStateWithLifecycle()
    val ratesState by viewModel.ratesUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settings.collectAsStateWithLifecycle(null)
    
    val isVietnamese = settingsState?.language == "vi"
    val showVndEstimates = settingsState?.showEstimatedVnd != false
    val colors = LocalKotNestColors.current
    val rates = ratesState.rates

    // Month Selector State
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Derive computed stats for the selected Month / Year to keep layout super fast
    val reportData = remember(subscriptions, histories, rates, selectedDate, showVndEstimates) {
        val targetMonth = selectedDate.monthValue
        val targetYear = selectedDate.year
        val convertUseCase = ConvertAmountToVndUseCase()

        // 1. History payments (Paid state)
        val paidHistoriesInMonth = histories.filter { history ->
            val localDate = DateUtils.getLocalDateFromEpoch(history.paidDate)
            localDate.monthValue == targetMonth && localDate.year == targetYear
        }

        // Subscriptions corresponding to paid histories (to match actual metadata names)
        val originalSubMap = subscriptions.associateBy { it.id }

        var totalPaidVnd = 0.0
        val categoryPaidVnd = mutableMapOf<Int, Double>()
        val paidItemsDetails = mutableListOf<ReportItem>()

        for (history in paidHistoriesInMonth) {
            val conversion = convertUseCase(history.amount, history.currency, rates)
            val convertedVal = if (showVndEstimates && conversion.isRateAvailable) {
                conversion.estimatedVndAmount ?: history.amount
            } else {
                history.amount
            }
            totalPaidVnd += convertedVal

            val assocSub = originalSubMap[history.subscriptionId]
            val catId = assocSub?.categoryId ?: -1
            categoryPaidVnd[catId] = (categoryPaidVnd[catId] ?: 0.0) + convertedVal

            paidItemsDetails.add(
                ReportItem(
                    id = history.id,
                    subId = history.subscriptionId,
                    name = assocSub?.name ?: "Unknown Dues",
                    amount = history.amount,
                    currency = history.currency,
                    convertedVnd = convertedVal,
                    isPaid = true,
                    dateEpoch = history.paidDate
                )
            )
        }

        // 2. Unpaid subscriptions falling in this month
        val unpaidSubsInMonth = subscriptions.filter { sub ->
            if (sub.isPaused || sub.status == "Paid") return@filter false
            val localDueDate = DateUtils.getLocalDateFromEpoch(sub.nextDueDate)
            localDueDate.monthValue == targetMonth && localDueDate.year == targetYear
        }

        var totalUnpaidVnd = 0.0
        var overdueVnd = 0.0
        var overdueCount = 0
        var upcomingCount = 0
        val unpaidItemsDetails = mutableListOf<ReportItem>()

        for (sub in unpaidSubsInMonth) {
            val conversion = convertUseCase(sub.amount, sub.currency, rates)
            val convertedVal = if (showVndEstimates && conversion.isRateAvailable) {
                conversion.estimatedVndAmount ?: sub.amount
            } else {
                sub.amount
            }
            totalUnpaidVnd += convertedVal

            val catId = sub.categoryId
            categoryPaidVnd[catId] = (categoryPaidVnd[catId] ?: 0.0) + convertedVal

            val isSubOverdue = sub.status == "Overdue" || 
                    DateUtils.getLocalDateFromEpoch(sub.nextDueDate).isBefore(LocalDate.now())

            if (isSubOverdue) {
                overdueVnd += convertedVal
                overdueCount++
            } else {
                upcomingCount++
            }

            unpaidItemsDetails.add(
                ReportItem(
                    id = sub.id,
                    subId = sub.id,
                    name = sub.name,
                    amount = sub.amount,
                    currency = sub.currency,
                    convertedVnd = convertedVal,
                    isPaid = false,
                    isOverdue = isSubOverdue,
                    dateEpoch = sub.nextDueDate
                )
            )
        }

        val grandTotalVnd = totalPaidVnd + totalUnpaidVnd
        val sortedCategories = categoryPaidVnd.entries
            .filter { it.key != -1 }
            .sortedByDescending { it.value }

        val allItemsByCost = (paidItemsDetails + unpaidItemsDetails).sortedByDescending { it.convertedVnd }

        ReportSummary(
            totalPaidVnd = totalPaidVnd,
            totalUnpaidUpcomingVnd = totalUnpaidVnd - overdueVnd,
            totalOverdueVnd = overdueVnd,
            grandTotalVnd = grandTotalVnd,
            paidCount = paidHistoriesInMonth.size,
            upcomingCount = upcomingCount,
            overdueCount = overdueCount,
            categoryDistribution = sortedCategories,
            topItems = allItemsByCost.take(5),
            hasForeignCurrencies = subscriptions.any { !it.currency.equals("VND", ignoreCase = true) }
        )
    }

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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colors.glassWhite,
                            contentColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.border, CircleShape)
                            .testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isVietnamese) "Báo cáo Tháng" else "Monthly Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.primaryText
                        )
                        Text(
                            text = if (isVietnamese) "Phân tích tài chính cá nhân" else "Personal spend intelligence",
                            fontSize = 12.sp,
                            color = colors.secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.exportBackupCsv(context, db) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = colors.primaryAqua.copy(alpha = 0.1f),
                            contentColor = colors.primaryAqua
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.primaryAqua.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share CSV",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Month Navigation Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedDate = selectedDate.minusMonths(1) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.primaryText)
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                            }

                            val monthLabel = selectedDate.month.getDisplayName(TextStyle.FULL, if (isVietnamese) Locale("vi") else Locale.ENGLISH)
                            Text(
                                text = "$monthLabel ${selectedDate.year}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { selectedDate = selectedDate.plusMonths(1) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = colors.primaryText)
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                            }
                        }
                    }
                }

                // 2. Hero Month Summary Card
                item {
                    val formattedGrandTotal = String.format("%,.0f", reportData.grandTotalVnd)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (colors.isLight) 1.5.dp else 1.dp,
                                color = if (colors.isLight) colors.border else colors.cyanAccent.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = (if (isVietnamese) "TỔNG SỐ PHẢI THANH TOÁN" else "TOTAL MONITORED DUE").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.mutedText,
                                letterSpacing = 1.2.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formattedGrandTotal,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.primaryText,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VND",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAqua,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            if (reportData.hasForeignCurrencies && showVndEstimates) {
                                Text(
                                    text = if (isVietnamese) "*(gồm ngoại tệ đã quy đổi)*" else "*(includes converted foreign currencies)*",
                                    fontSize = 11.sp,
                                    color = colors.mutedText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 18.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.border)
                            )

                            // 2-column stats summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isVietnamese) "ĐÃ THANH TOÁN" else "PAID COALITION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.mutedText,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format("%,.0f đ", reportData.totalPaidVnd),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.success
                                    )
                                    Text(
                                        text = "${reportData.paidCount} " + (if (isVietnamese) "khoản" else "unlocked"),
                                        fontSize = 11.sp,
                                        color = colors.secondaryText
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    val remainingVnd = reportData.totalUnpaidUpcomingVnd + reportData.totalOverdueVnd
                                    Text(
                                        text = if (isVietnamese) "CHƯA HOÀN THÀNH" else "REMAINING UNPAID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.mutedText,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format("%,.0f đ", remainingVnd),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (reportData.totalOverdueVnd > 0) colors.danger else colors.primaryAqua
                                    )
                                    Text(
                                        text = "${reportData.upcomingCount + reportData.overdueCount} " + (if (isVietnamese) "khoản chưa trả" else "unpaid dues"),
                                        fontSize = 11.sp,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Mini Paid / Upcoming / Overdue Stats Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = if (isVietnamese) "Quá hạn" else "Overdue",
                            amountVnd = reportData.totalOverdueVnd,
                            count = reportData.overdueCount,
                            color = colors.danger,
                            bgColor = colors.overdueBackground,
                            isVietnamese = isVietnamese,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = if (isVietnamese) "Sắp tới" else "Upcoming",
                            amountVnd = reportData.totalUnpaidUpcomingVnd,
                            count = reportData.upcomingCount,
                            color = colors.primaryAqua,
                            bgColor = colors.upcomingBackground,
                            isVietnamese = isVietnamese,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = if (isVietnamese) "Đã trả" else "Completed",
                            amountVnd = reportData.totalPaidVnd,
                            count = reportData.paidCount,
                            color = colors.success,
                            bgColor = colors.paidBackground,
                            isVietnamese = isVietnamese,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Category Spending Breakdown with Horizontal Progress
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isVietnamese) "PHÂN PHỐI THEO DANH MỤC" else "SPENDING BY CATEGORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.mutedText,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (reportData.categoryDistribution.isEmpty()) {
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
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isVietnamese) "Không có giao dịch nào trong tháng" else "Zero monitored spend this month 🍃",
                                        fontSize = 13.sp,
                                        color = colors.secondaryText
                                    )
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    val catMap = categories.associateBy { it.id }
                                    reportData.categoryDistribution.forEach { entry ->
                                        val cat = catMap[entry.key]
                                        val catName = cat?.name ?: (if (isVietnamese) "Khác" else "Misc")
                                        val catColor = parseColorString(cat?.color ?: "#4CAF50")
                                        val catEmoji = cat?.icon ?: "💳"
                                        val percentage = if (reportData.grandTotalVnd > 0) (entry.value / reportData.grandTotalVnd).toFloat() else 0f

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(catEmoji, fontSize = 16.sp)
                                                    Text(
                                                        text = catName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.primaryText
                                                    )
                                                }
                                                Text(
                                                    text = String.format("%,.0f đ (%.0f%%)", entry.value, percentage * 100),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.secondaryText
                                                )
                                            }
                                            
                                            // horizontal progress representing category weight
                                            LinearProgressIndicator(
                                                progress = { percentage },
                                                color = catColor,
                                                trackColor = colors.border,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Top 5 Most Expensive Items Lists
                item {
                    Text(
                        text = if (isVietnamese) "TOP KHOẢN CHI LỚN NHẤT" else "TOP HIGHEST COST DUES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.2.sp
                    )
                }

                if (reportData.topItems.isEmpty()) {
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
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isVietnamese) "Chưa có dữ liệu" else "No dues identified",
                                    fontSize = 13.sp,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                } else {
                    items(reportData.topItems, key = { "${it.id}_${it.isPaid}" }) { item ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                                .clickable { onNavigateToDetail(item.subId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isPaid) colors.success.copy(alpha = 0.1f) 
                                            else if (item.isOverdue) colors.danger.copy(alpha = 0.1f) 
                                            else colors.primaryAqua.copy(alpha = 0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.isPaid) "✓" else if (item.isOverdue) "!" else "⏱",
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPaid) colors.success else if (item.isOverdue) colors.danger else colors.primaryAqua
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = DateUtils.formatEpoch(item.dateEpoch, if (isVietnamese) "dd/MM/yyyy" else "MMM dd, yyyy"),
                                        fontSize = 11.sp,
                                        color = colors.secondaryText
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format("%,.2f", item.amount) + " " + item.currency,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primaryText
                                    )
                                    if (!item.currency.equals("VND", ignoreCase = true) && showVndEstimates) {
                                        Text(
                                            text = "≈ " + String.format("%,.0f", item.convertedVnd) + " VND",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryAqua
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Spacer at bottom so elements are never cut off by the floating glass bottom bar
                item {
                    Spacer(modifier = Modifier.height(115.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amountVnd: Double,
    count: Int,
    color: Color,
    bgColor: Color,
    isVietnamese: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalKotNestColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.08f)),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.secondaryText
            )
            Text(
                text = String.format("%,.0f đ", amountVnd),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = "$count " + (if (isVietnamese) "khoản" else "dues"),
                fontSize = 10.sp,
                color = colors.mutedText
            )
        }
    }
}

private fun parseColorString(hexStr: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexStr))
    } catch (e: Exception) {
        Color.Gray
    }
}

data class ReportItem(
    val id: Int,
    val subId: Int,
    val name: String,
    val amount: Double,
    val currency: String,
    val convertedVnd: Double,
    val isPaid: Boolean,
    val isOverdue: Boolean = false,
    val dateEpoch: Long
)

data class ReportSummary(
    val totalPaidVnd: Double,
    val totalUnpaidUpcomingVnd: Double,
    val totalOverdueVnd: Double,
    val grandTotalVnd: Double,
    val paidCount: Int,
    val upcomingCount: Int,
    val overdueCount: Int,
    val categoryDistribution: List<Map.Entry<Int, Double>>,
    val topItems: List<ReportItem>,
    val hasForeignCurrencies: Boolean
)
