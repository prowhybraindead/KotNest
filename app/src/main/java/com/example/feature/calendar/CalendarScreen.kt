package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.feature.dashboard.StatusChip
import com.example.feature.dashboard.parseColor
import com.example.ui.viewmodel.DueMateViewModel
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: DueMateViewModel,
    onNavigateToDetail: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    val categoryMap = categories.associateBy { it.id }

    val currentYearMonth = uiState.selectedMonth
    val selectedDate = uiState.selectedDate
    val paymentsByDate = uiState.paymentsByDate
    val cells = uiState.dayCells

    var showBottomSheet by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val colors = LocalKotNestColors.current

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colors.backgroundGradientStart,
            colors.backgroundGradientEnd
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Track", fontWeight = FontWeight.Bold, color = colors.primaryText) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundGradientStart
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Month switcher header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.selectCalendarMonth(currentYearMonth.minusMonths(1)) },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Prev Month",
                            tint = colors.primaryText
                        )
                    }

                    Text(
                        text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " " + currentYearMonth.year,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primaryText
                    )

                    IconButton(
                        onClick = { viewModel.selectCalendarMonth(currentYearMonth.plusMonths(1)) },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = colors.primaryText
                        )
                    }
                }
            }

            // Large main calendar body inside a premium card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 7-column grid from Monday to Sunday
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val headers = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                        headers.forEach { h ->
                            Text(
                                text = h,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                color = colors.mutedText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calendar Days Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        items(
                            count = cells.size,
                            key = { index -> cells[index]?.toString() ?: "empty_$index" }
                        ) { index ->
                            val date = cells[index]
                            if (date == null) {
                                Spacer(modifier = Modifier.aspectRatio(1f))
                            } else {
                                val payments = paymentsByDate[date] ?: emptyList()
                                val isToday = date.isEqual(today)
                                
                                CalendarDayCell(
                                    date = date,
                                    payments = payments,
                                    isToday = isToday,
                                    isSelected = selectedDate == date,
                                    onClick = {
                                        viewModel.selectCalendarDate(date)
                                        showBottomSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Bottom Sheet styled elegantly as KotNest Bottom Panel
    if (showBottomSheet && selectedDate != null) {
        val targetDate = selectedDate!!
        val dayPayments = uiState.duesForSelectedDate
        val totalDue = dayPayments.sumOf { it.amount }

        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false
                viewModel.selectCalendarDate(null)
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = DateUtils.formatEpoch(DateUtils.getEpochFromLocalDate(targetDate), "EEEE, dd MMMM yyyy"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Total Amount Due: ",
                        fontSize = 13.sp,
                        color = colors.secondaryText
                    )
                    Text(
                        text = String.format("%,.0f", totalDue) + " VND",
                        fontSize = 15.sp,
                        color = if (colors.isLight) colors.deepAqua else colors.cyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )

                if (dayPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Your calendar is clear for this day ✨",
                            color = colors.mutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(dayPayments) { sub ->
                            val cat = categoryMap[sub.categoryId]
                            val catColor = parseColor(cat?.color ?: "#4CAF50")
                            
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                    .clickable {
                                        showBottomSheet = false
                                        onNavigateToDetail(sub.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(catColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val emojiText = cat?.icon ?: "💳"
                                        Text(text = emojiText, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sub.name,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = cat?.name ?: "Other",
                                            fontSize = 11.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = String.format("%,.0f", sub.amount) + " " + sub.currency,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primaryText,
                                            fontSize = 13.sp
                                        )
                                        StatusChip(sub.status)
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.markAsPaid(sub.id) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = colors.glassWhite,
                                            contentColor = colors.success
                                        ),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(1.dp, colors.border, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Mark Paid",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    payments: List<Subscription>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKotNestColors.current
    val isLight = colors.isLight

    val hasPayments = payments.isNotEmpty()
    val status = remember(payments) {
        when {
            payments.isEmpty() -> null
            payments.any { it.status == "Overdue" && !it.isPaused } -> "Overdue"
            payments.any { it.status == "Due Today" && !it.isPaused } -> "Due Today"
            payments.any { it.status == "Upcoming" && !it.isPaused } -> "Upcoming"
            payments.any { it.status == "Paid" && !it.isPaused } -> "Paid"
            payments.any { it.isPaused } -> "Paused"
            else -> "Upcoming"
        }
    }

    val indicatorColor = remember(status, isLight) {
        when (status) {
            "Overdue" -> Color(if (isLight) 0xFFF04438 else 0xFFEF4444)
            "Due Today" -> Color(if (isLight) 0xFFF79009 else 0xFFF59E0B)
            "Upcoming" -> Color(if (isLight) 0xFF00AEEF else 0xFF48CAE4)
            "Paid" -> Color(if (isLight) 0xFF12B76A else 0xFF22C55E)
            "Paused" -> Color(if (isLight) 0xFF98A2B3 else 0xFF64748B)
            else -> Color.Transparent
        }
    }

    val cellModifier = modifier
        .aspectRatio(1f)
        .clip(CircleShape)
        .clickable { onClick() }
        .then(
            when {
                isSelected -> Modifier
                    .background(
                        if (isLight) colors.primaryAqua.copy(alpha = 0.2f)
                        else colors.primaryAqua.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .border(2.dp, colors.primaryAqua, CircleShape)
                isToday -> Modifier
                    .border(1.5.dp, if (isLight) colors.deepAqua else colors.cyanAccent, CircleShape)
                else -> Modifier
            }
        )
        .testTag("calendar_cell_${date.dayOfMonth}")

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = when {
                    isSelected -> if (isLight) colors.deepAqua else colors.cyanAccent
                    isToday -> if (isLight) colors.deepAqua else colors.cyanAccent
                    else -> if (isLight) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                }
            )
            
            if (status != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(if (payments.size > 1) 18.dp else 10.dp)
                        .height(3.dp)
                        .background(indicatorColor, RoundedCornerShape(1.5.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

fun getCalendarCells(yearMonth: YearMonth): List<LocalDate?> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val totalDays = yearMonth.lengthOfMonth()
    val firstDayDayOfWeek = firstDayOfMonth.dayOfWeek.value
    
    val cells = mutableListOf<LocalDate?>()
    val paddingDays = firstDayDayOfWeek - 1
    for (i in 0 until paddingDays) {
        cells.add(null)
    }
    for (day in 1..totalDays) {
        cells.add(yearMonth.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}
