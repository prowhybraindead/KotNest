package com.example.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.util.DateUtils
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
            // Instantiate AppDatabase directly in background worker thread safely
            val db = AppDatabase.getDatabase(applicationContext, kotlinx.coroutines.GlobalScope)
            val subscriptions = db.subscriptionDao().getAllSubscriptions().first()
            val notificationHelper = NotificationHelper(applicationContext)

            val today = LocalDate.now()
            var dueTodayCount = 0

            for (subscription in subscriptions) {
                if (subscription.isPaused || subscription.status == "Paid") continue

                val dueLocalDate = Instant.ofEpochMilli(subscription.nextDueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                
                val daysDiff = ChronoUnit.DAYS.between(today, dueLocalDate)

                when {
                    daysDiff == 7L -> {
                        notificationHelper.sendReminderNotification(
                            subscription,
                            "Renewal Reminder (7 Days)",
                            "${subscription.name} will renew in 7 days for ${subscription.amount} ${subscription.currency}."
                        )
                    }
                    daysDiff == 3L -> {
                        notificationHelper.sendReminderNotification(
                            subscription,
                            "Renewal Reminder (3 Days)",
                            "${subscription.name} will renew in 3 days for ${subscription.amount} ${subscription.currency}."
                        )
                    }
                    daysDiff == 1L -> {
                        notificationHelper.sendReminderNotification(
                            subscription,
                            "Renewal Tomorrow",
                            "${subscription.name} will renew tomorrow for ${subscription.amount} ${subscription.currency}."
                        )
                    }
                    daysDiff == 0L -> {
                        dueTodayCount++
                        notificationHelper.sendReminderNotification(
                            subscription,
                            "Payment Due Today",
                            "${subscription.name} payment of ${subscription.amount} ${subscription.currency} is due today!"
                        )
                    }
                    daysDiff < 0L -> {
                        notificationHelper.sendReminderNotification(
                            subscription,
                            "Payment Overdue",
                            "${subscription.name} is overdue by ${Math.abs(daysDiff)} days!"
                        )
                    }
                }
            }

            if (dueTodayCount > 1) {
                notificationHelper.sendSummaryNotification(
                    dueTodayCount,
                    "You have $dueTodayCount payments due today."
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
