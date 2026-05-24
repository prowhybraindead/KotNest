package com.example.domain.usecase

import com.example.domain.model.Subscription
import com.example.domain.model.ExchangeRateCache
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GetTodayDueCountUseCase {
    operator fun invoke(subs: List<Subscription>): Int {
        val today = LocalDate.now()
        return subs.count { sub ->
            if (sub.isPaused || sub.status == "Paid") return@count false
            val dueLocalDate = Instant.ofEpochMilli(sub.nextDueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dueLocalDate.isEqual(today) || sub.status == "Due Today"
        }
    }
}

class GetOverdueCountUseCase {
    operator fun invoke(subs: List<Subscription>): Int {
        val today = LocalDate.now()
        return subs.count { sub ->
            if (sub.isPaused || sub.status == "Paid") return@count false
            val dueLocalDate = Instant.ofEpochMilli(sub.nextDueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dueLocalDate.isBefore(today) || sub.status == "Overdue"
        }
    }
}

class GetNextDueUseCase {
    operator fun invoke(subs: List<Subscription>): Subscription? {
        val today = LocalDate.now()
        val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return subs
            .filter { !it.isPaused && it.status != "Paid" && it.nextDueDate >= startOfToday }
            .minByOrNull { it.nextDueDate }
    }
}

class GetMonthlyEstimatedTotalUseCase {
    private val convertUseCase = ConvertAmountToVndUseCase()
    
    operator fun invoke(
        subs: List<Subscription>,
        rates: List<ExchangeRateCache>,
        showEstimatedVnd: Boolean = true
    ): Double {
        val now = LocalDate.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        return subs.sumOf { sub ->
            val dueLocalDate = Instant.ofEpochMilli(sub.nextDueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                
            if (dueLocalDate.monthValue == currentMonth && dueLocalDate.year == currentYear && !sub.isPaused && sub.status != "Paid") {
                val conversion = convertUseCase(sub.amount, sub.currency, rates)
                if (showEstimatedVnd && conversion.isRateAvailable) {
                    conversion.estimatedVndAmount ?: sub.amount
                } else {
                    sub.amount
                }
            } else {
                0.0
            }
        }
    }
}

class GetDashboardSummaryUseCase {
    private val getTodayDueCount = GetTodayDueCountUseCase()
    private val getOverdueCount = GetOverdueCountUseCase()
    private val getNextDue = GetNextDueUseCase()
    private val getMonthlyEstimatedTotal = GetMonthlyEstimatedTotalUseCase()

    operator fun invoke(
        subs: List<Subscription>,
        rates: List<ExchangeRateCache>,
        showEstimatedVnd: Boolean = true
    ): DashboardSummary {
        val today = LocalDate.now()
        val startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sevenDaysLater = today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val monthlyTotal = getMonthlyEstimatedTotal(subs, rates, showEstimatedVnd)
        val upcomingCount = subs.count { !it.isPaused && it.status == "Upcoming" }
        val overdueCount = getOverdueCount(subs)
        val closestUpcoming = getNextDue(subs)
        
        val dueNextSevenDays = subs
            .filter { !it.isPaused && it.status != "Paid" && it.nextDueDate in startOfToday..sevenDaysLater }
            .sortedBy { it.nextDueDate }

        val hasForeignCurrency = subs.any { !it.isPaused && !it.currency.equals("VND", ignoreCase = true) }

        return DashboardSummary(
            currentMonthTotal = monthlyTotal,
            upcomingCount = upcomingCount,
            overdueCount = overdueCount,
            closestUpcoming = closestUpcoming,
            dueNextSevenDays = dueNextSevenDays,
            hasForeignCurrency = hasForeignCurrency
        )
    }
}

data class DashboardSummary(
    val currentMonthTotal: Double,
    val upcomingCount: Int,
    val overdueCount: Int,
    val closestUpcoming: Subscription?,
    val dueNextSevenDays: List<Subscription>,
    val hasForeignCurrency: Boolean
)
