package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.domain.model.Subscription

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_DUE_REMINDERS = "kotnest_due_reminders"
        const val CHANNEL_DAILY_SUMMARY = "kotnest_daily_summary"
        const val CHANNEL_BACKUP = "kotnest_backup"
        const val CHANNEL_GENERAL = "kotnest_general"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Due Reminders (High importance)
            val dueChannel = NotificationChannel(
                CHANNEL_DUE_REMINDERS,
                "Due Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming, due today, and overdue dues."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(dueChannel)

            // 2. Daily Summary (Default importance)
            val summaryChannel = NotificationChannel(
                CHANNEL_DAILY_SUMMARY,
                "Daily Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning summary of your dues."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(summaryChannel)

            // 3. Backup Reminders (Low/Default importance)
            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "Backup Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reminders to backup your KotNest data."
            }
            notificationManager.createNotificationChannel(backupChannel)

            // 4. General (Default importance)
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications."
            }
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun getPendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    fun sendUpcomingNotification(sub: Subscription, daysLeft: Int, amountText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", sub.id)
        }
        val pendingIntent = PendingIntent.getActivity(context, sub.id, intent, getPendingIntentFlags())

        val markAsPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_PAID
            putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
        }
        val markAsPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id * 10 + 1,
            markAsPaidIntent,
            getPendingIntentFlags()
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id * 10 + 2,
            snoozeIntent,
            getPendingIntentFlags()
        )

        val body = "${sub.name} renews in $daysLeft days • $amountText"

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_REMINDERS)
            .setSmallIcon(com.example.R.drawable.ic_kotnest_icon)
            .setContentTitle("KotNest reminder")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(com.example.R.drawable.ic_kotnest_icon, "Mark as Paid", markAsPaidPendingIntent)
            .addAction(com.example.R.drawable.ic_kotnest_icon, "Snooze 1 Day", snoozePendingIntent)
            .build()

        notificationManager.notify(sub.id, notification)
    }

    fun sendDueTodayNotification(sub: Subscription, amountText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", sub.id)
        }
        val pendingIntent = PendingIntent.getActivity(context, sub.id, intent, getPendingIntentFlags())

        val markAsPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_PAID
            putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
        }
        val markAsPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id * 10 + 3,
            markAsPaidIntent,
            getPendingIntentFlags()
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id * 10 + 4,
            snoozeIntent,
            getPendingIntentFlags()
        )

        val body = "${sub.name} is due today • $amountText"

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_REMINDERS)
            .setSmallIcon(com.example.R.drawable.ic_kotnest_icon)
            .setContentTitle("Due today")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(com.example.R.drawable.ic_kotnest_icon, "Mark as Paid", markAsPaidPendingIntent)
            .addAction(com.example.R.drawable.ic_kotnest_icon, "Snooze 1 Day", snoozePendingIntent)
            .build()

        notificationManager.notify(sub.id, notification)
    }

    fun sendOverdueNotification(sub: Subscription, daysOverdue: Int, amountText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", sub.id)
        }
        val pendingIntent = PendingIntent.getActivity(context, sub.id, intent, getPendingIntentFlags())

        val markAsPaidIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_PAID
            putExtra(NotificationActionReceiver.EXTRA_SUBSCRIPTION_ID, sub.id)
        }
        val markAsPaidPendingIntent = PendingIntent.getBroadcast(
            context,
            sub.id * 10 + 5,
            markAsPaidIntent,
            getPendingIntentFlags()
        )

        val body = "${sub.name} is overdue by $daysOverdue days • $amountText"

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_REMINDERS)
            .setSmallIcon(com.example.R.drawable.ic_kotnest_icon)
            .setContentTitle("Overdue payment")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(com.example.R.drawable.ic_kotnest_icon, "Mark as Paid", markAsPaidPendingIntent)
            .build()

        notificationManager.notify(sub.id, notification)
    }

    fun sendDailySummaryNotification(todayCount: Int, upcomingCount: Int, overdueCount: Int, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 9999, intent, getPendingIntentFlags())

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(com.example.R.drawable.ic_kotnest_icon)
            .setContentTitle("Today in KotNest")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9999, notification)
    }

    fun sendGenericNotification(title: String, body: String, notificationId: Int = 10001) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, getPendingIntentFlags())

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(com.example.R.drawable.ic_kotnest_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
