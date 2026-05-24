package com.example.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.BuildConfig
import com.example.core.database.AppDatabase
import com.example.core.network.RetrofitClient
import com.example.domain.model.ReminderRule
import java.util.concurrent.TimeUnit

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val subscriptionDao = db.subscriptionDao()
        val reminderRuleDao = db.reminderRuleDao()
        val prefs = applicationContext.getSharedPreferences("cloud_sync_meta", Context.MODE_PRIVATE)

        return try {
            val api = RetrofitClient.getClient(
                baseUrl = BuildConfig.BACKEND_BASE_URL,
                apiToken = BuildConfig.BACKEND_API_TOKEN.ifBlank { null },
                deviceId = BuildConfig.BACKEND_DEVICE_ID.ifBlank { null }
            )

            val localSubscriptions = subscriptionDao.getAllSubscriptionsDirect()
            val pushResponse = api.syncSubscriptions(
                subscriptions = localSubscriptions,
                deviceId = BuildConfig.BACKEND_DEVICE_ID
            )

            if (!pushResponse.isSuccessful) {
                prefs.edit().putString("last_error", "push_failed_${pushResponse.code()}").apply()
                return if (pushResponse.code() in 500..599) Result.retry() else Result.failure()
            }

            // Pull only when local is empty to keep local DB as source-of-truth,
            // while still allowing bootstrap restore from cloud.
            if (localSubscriptions.isEmpty()) {
                val pullResponse = api.getSubscriptions(deviceId = BuildConfig.BACKEND_DEVICE_ID)
                if (pullResponse.isSuccessful) {
                    val remoteSubs = pullResponse.body().orEmpty()
                    for (sub in remoteSubs) {
                        subscriptionDao.insertSubscription(sub)
                        val existingRules = reminderRuleDao.getRulesForSubscriptionDirect(sub.id)
                        if (existingRules.isEmpty()) {
                            reminderRuleDao.insertRules(
                                listOf(
                                    ReminderRule(subscriptionId = sub.id, daysBefore = 0, reminderTime = "09:00", enabled = true),
                                    ReminderRule(subscriptionId = sub.id, daysBefore = 3, reminderTime = "09:00", enabled = true)
                                )
                            )
                        }
                    }
                }
            }

            prefs.edit()
                .putLong("last_success_at", System.currentTimeMillis())
                .remove("last_error")
                .apply()

            Result.success()
        } catch (e: Exception) {
            prefs.edit().putString("last_error", e.message ?: "sync_exception").apply()
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "KotNestCloudAutoSync"
        private const val ONE_SHOT_WORK_NAME = "KotNestCloudAutoSyncNow"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
