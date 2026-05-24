package com.example.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val subscriptions = db.subscriptionDao().getAllSubscriptions().first()
            val notificationHelper = NotificationHelper(applicationContext)
            val snoozePrefs = applicationContext.getSharedPreferences("kotnest_snooze", Context.MODE_PRIVATE)

            val today = LocalDate.now()
            var dueTodayCount = 0
            var upcomingCount = 0
            var overdueCount = 0

            for (subscription in subscriptions) {
                if (subscription.isPaused || subscription.status == "Paid") continue

                val snoozeUntil = snoozePrefs.getLong("snooze_${subscription.id}", 0L)
                if (snoozeUntil > System.currentTimeMillis()) continue

                val dueLocalDate = Instant.ofEpochMilli(subscription.nextDueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                
                val daysDiff = ChronoUnit.DAYS.between(today, dueLocalDate)
                val amountText = "${String.format("%,.0f", subscription.amount)} ${subscription.currency}"

                when {
                    daysDiff == 7L -> {
                        upcomingCount++
                        notificationHelper.sendUpcomingNotification(subscription, 7, amountText)
                    }
                    daysDiff == 3L -> {
                        upcomingCount++
                        notificationHelper.sendUpcomingNotification(subscription, 3, amountText)
                    }
                    daysDiff == 1L -> {
                        upcomingCount++
                        notificationHelper.sendUpcomingNotification(subscription, 1, amountText)
                    }
                    daysDiff == 0L -> {
                        dueTodayCount++
                        notificationHelper.sendDueTodayNotification(subscription, amountText)
                    }
                    daysDiff < 0L -> {
                        overdueCount++
                        notificationHelper.sendOverdueNotification(subscription, kotlin.math.abs(daysDiff).toInt(), amountText)
                    }
                }
            }

            if (dueTodayCount > 0 || upcomingCount > 0 || overdueCount > 0) {
                val body = "Due today: $dueTodayCount | Upcoming alerts: $upcomingCount | Overdue: $overdueCount"
                notificationHelper.sendDailySummaryNotification(
                    dueTodayCount,
                    upcomingCount,
                    overdueCount,
                    body
                )
            }

            // Also dynamically update statuses for recurring subscriptions that passed their due date
            for (subscription in subscriptions) {
                if (subscription.isPaused) continue
                val dueLocalDate = Instant.ofEpochMilli(subscription.nextDueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val newStatus = when {
                    dueLocalDate.isBefore(today) -> "Overdue"
                    dueLocalDate.isEqual(today) -> "Due Today"
                    else -> "Upcoming"
                }

                if (subscription.status != newStatus && subscription.status != "Paid") {
                    val updatedSub = subscription.copy(status = newStatus, updatedAt = System.currentTimeMillis())
                    db.subscriptionDao().insertSubscription(updatedSub)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
