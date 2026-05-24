package com.example.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.database.AppDatabase
import com.example.domain.usecase.GetDashboardSummaryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class KotNestWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val subDao = db.subscriptionDao()
                    val rateDao = db.exchangeRateDao()

                    val subsList = subDao.getAllSubscriptions().firstOrNull() ?: emptyList()
                    val ratesList = rateDao.getAllRatesDirect()

                    val summaryUseCase = GetDashboardSummaryUseCase()
                    val summary = summaryUseCase(subsList, ratesList, showEstimatedVnd = true)

                    val views = RemoteViews(context.packageName, R.layout.kotnest_widget_layout)

                    val vndTotalText = String.format("%,.0f VND", summary.currentMonthTotal)
                    views.setTextViewText(R.id.widget_month_total, "This month: $vndTotalText")

                    val closest = summary.closestUpcoming
                    if (closest != null) {
                        val daysLeft = ((closest.nextDueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                        val dueText = if (daysLeft == 0) {
                            "${closest.name} (Due today!)"
                        } else {
                            "${closest.name} (in $daysLeft d)"
                        }
                        views.setTextViewText(R.id.widget_next_due_value, dueText)
                    } else {
                        views.setTextViewText(R.id.widget_next_due_value, "No upcoming dues")
                    }

                    // Count dues today
                    val todayCountText = "Today: ${summary.dueNextSevenDays.count { ((it.nextDueDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)) <= 0 }}"
                    views.setTextViewText(R.id.widget_today_countText, todayCountText)
                    views.setTextViewText(R.id.widget_overdue_countText, "Overdue: ${summary.overdueCount}")

                    val tapIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        tapIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_main_container, pendingIntent)

                    for (appWidgetId in appWidgetIds) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, KotNestWidgetProvider::class.java)
            )
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }
    }
}
