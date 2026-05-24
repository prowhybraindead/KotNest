package com.example.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.core.database.AppDatabase
import com.example.core.util.DateUtils
import com.example.domain.model.PaymentHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_AS_PAID = "com.example.ACTION_MARK_AS_PAID"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val subId = intent.getIntExtra(EXTRA_SUBSCRIPTION_ID, -1)
        if (subId == -1) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_MARK_AS_PAID -> {
                notificationManager.cancel(subId)

                GlobalScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context.applicationContext, GlobalScope)
                    val subDao = db.subscriptionDao()
                    val historyDao = db.paymentHistoryDao()

                    val subscription = subDao.getSubscriptionByIdDirect(subId)
                    if (subscription != null) {
                        // Idempotency: verify no existing history item for this exact paid date
                        val existingHistories = historyDao.getHistoryForSubscriptionDirect(subId)
                        val isAlreadyPaid = existingHistories.any { it.paidDate == subscription.nextDueDate }

                        if (!isAlreadyPaid) {
                            val history = PaymentHistory(
                                subscriptionId = subId,
                                paidDate = subscription.nextDueDate,
                                amount = subscription.amount,
                                currency = subscription.currency,
                                note = "Paid renewal subscription (via Notification)"
                            )
                            historyDao.insertHistory(history)

                            val nextDueDate = if (subscription.billingCycle == "One-time") {
                                subscription.nextDueDate
                            } else {
                                DateUtils.calculateNextDueDate(
                                    currentEpoch = subscription.nextDueDate,
                                    billingCycle = subscription.billingCycle,
                                    customValue = subscription.customCycleValue
                                )
                            }

                            val updatedStatus = if (subscription.billingCycle == "One-time") {
                                "Paid"
                            } else {
                                val now = System.currentTimeMillis()
                                val dueLocalDate = DateUtils.getLocalDateFromEpoch(nextDueDate)
                                val todayLocalDate = DateUtils.getLocalDateFromEpoch(now)

                                if (dueLocalDate.isBefore(todayLocalDate)) {
                                    "Overdue"
                                } else if (dueLocalDate.isEqual(todayLocalDate)) {
                                    "Due Today"
                                } else {
                                    "Upcoming"
                                }
                            }

                            val updatedSub = subscription.copy(
                                nextDueDate = nextDueDate,
                                status = if (subscription.isPaused) "Paused" else updatedStatus,
                                updatedAt = System.currentTimeMillis()
                            )
                            subDao.insertSubscription(updatedSub)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${subscription.name} marked as paid!", Toast.LENGTH_SHORT).show()
                            }

                            // Trigger widget & background update
                            WidgetUpdateWorker.enqueue(context.applicationContext)
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "${subscription.name} was already paid.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            ACTION_SNOOZE -> {
                notificationManager.cancel(subId)

                val prefs = context.getSharedPreferences("kotnest_snooze", Context.MODE_PRIVATE)
                prefs.edit().putLong("snooze_${subId}", System.currentTimeMillis() + 24 * 60 * 60 * 1000).apply()

                Toast.makeText(context, "Snoozed ${subId} reminder for 1 day", Toast.LENGTH_SHORT).show()
                WidgetUpdateWorker.enqueue(context.applicationContext)
            }
        }
    }
}
