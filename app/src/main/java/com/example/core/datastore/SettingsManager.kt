package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "duemate_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val DEFAULT_CURRENCY_KEY = stringPreferencesKey("default_currency")
        private val DEFAULT_REMINDER_TIME_KEY = stringPreferencesKey("default_reminder_time")
        private val DEFAULT_REMINDER_DAYS_KEY = intPreferencesKey("default_reminder_days")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
        private val BACKEND_BASE_URL_KEY = stringPreferencesKey("backend_base_url")
        private val PREFERRED_TARGET_CURRENCY_KEY = stringPreferencesKey("preferred_target_currency")
        private val REFRESH_INTERVAL_HOURS_KEY = intPreferencesKey("refresh_interval_hours")
        private val AUTO_REFRESH_RATES_KEY = booleanPreferencesKey("auto_refresh_rates")
        private val EXCHANGE_RATE_PROVIDER_KEY = stringPreferencesKey("exchange_rate_provider")
        private val SHOW_ESTIMATED_VND_KEY = booleanPreferencesKey("show_estimated_vnd")
        private val SMART_SUGGESTIONS_ENABLED_KEY = booleanPreferencesKey("smart_suggestions_enabled")
        private val LAST_BACKUP_AT_KEY = longPreferencesKey("last_backup_at")
        private val LAST_IMPORT_AT_KEY = longPreferencesKey("last_import_at")
        private val LAST_BACKUP_ITEM_COUNT_KEY = intPreferencesKey("last_backup_item_count")
        private val LAST_BACKUP_FILE_NAME_KEY = stringPreferencesKey("last_backup_file_name")
        private val DAILY_SUMMARY_ENABLED_KEY = booleanPreferencesKey("daily_summary_enabled")
        private val OVERDUE_REMINDERS_ENABLED_KEY = booleanPreferencesKey("overdue_reminders_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            defaultCurrency = preferences[DEFAULT_CURRENCY_KEY] ?: "VND",
            defaultReminderTime = preferences[DEFAULT_REMINDER_TIME_KEY] ?: "09:00",
            defaultReminderDays = preferences[DEFAULT_REMINDER_DAYS_KEY] ?: 3,
            theme = preferences[THEME_KEY] ?: "System",
            language = preferences[LANGUAGE_KEY] ?: "en",
            notificationEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
            appLockEnabled = preferences[APP_LOCK_ENABLED_KEY] ?: false,
            backendBaseUrl = preferences[BACKEND_BASE_URL_KEY] ?: "http://192.168.1.100:3000",
            preferredTargetCurrency = preferences[PREFERRED_TARGET_CURRENCY_KEY] ?: "VND",
            refreshIntervalHours = preferences[REFRESH_INTERVAL_HOURS_KEY] ?: 6,
            autoRefreshRates = preferences[AUTO_REFRESH_RATES_KEY] ?: true,
            exchangeRateProviderName = preferences[EXCHANGE_RATE_PROVIDER_KEY] ?: "ExchangeRate-API Open Access",
            showEstimatedVnd = preferences[SHOW_ESTIMATED_VND_KEY] ?: true,
            smartSuggestionsEnabled = preferences[SMART_SUGGESTIONS_ENABLED_KEY] ?: true,
            lastBackupAt = preferences[LAST_BACKUP_AT_KEY] ?: 0L,
            lastImportAt = preferences[LAST_IMPORT_AT_KEY] ?: 0L,
            lastBackupItemCount = preferences[LAST_BACKUP_ITEM_COUNT_KEY] ?: 0,
            lastBackupFileName = preferences[LAST_BACKUP_FILE_NAME_KEY] ?: "",
            dailySummaryEnabled = preferences[DAILY_SUMMARY_ENABLED_KEY] ?: true,
            overdueRemindersEnabled = preferences[OVERDUE_REMINDERS_ENABLED_KEY] ?: true
        )
    }

    suspend fun updateDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_CURRENCY_KEY] = currency
        }
    }

    suspend fun updateDefaultReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_REMINDER_TIME_KEY] = time
        }
    }

    suspend fun updateDefaultReminderDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_REMINDER_DAYS_KEY] = days
        }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateBackendBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_BASE_URL_KEY] = url
        }
    }

    suspend fun updatePreferredTargetCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_TARGET_CURRENCY_KEY] = currency
        }
    }

    suspend fun updateRefreshIntervalHours(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_HOURS_KEY] = hours
        }
    }

    suspend fun updateAutoRefreshRates(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_REFRESH_RATES_KEY] = enabled
        }
    }

    suspend fun updateExchangeRateProviderName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[EXCHANGE_RATE_PROVIDER_KEY] = name
        }
    }

    suspend fun updateShowEstimatedVnd(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ESTIMATED_VND_KEY] = enabled
        }
    }

    suspend fun updateSmartSuggestionsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_SUGGESTIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateBackupMetadata(lastBackupAt: Long, count: Int, fileName: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_BACKUP_AT_KEY] = lastBackupAt
            preferences[LAST_BACKUP_ITEM_COUNT_KEY] = count
            preferences[LAST_BACKUP_FILE_NAME_KEY] = fileName
        }
    }

    suspend fun updateImportMetadata(lastImportAt: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_IMPORT_AT_KEY] = lastImportAt
        }
    }

    suspend fun updateDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_SUMMARY_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateOverdueRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OVERDUE_REMINDERS_ENABLED_KEY] = enabled
        }
    }
}

data class AppSettings(
    val defaultCurrency: String,
    val defaultReminderTime: String,
    val defaultReminderDays: Int,
    val theme: String, // "Light", "Dark", "System"
    val language: String,
    val notificationEnabled: Boolean,
    val appLockEnabled: Boolean,
    val backendBaseUrl: String,
    val preferredTargetCurrency: String,
    val refreshIntervalHours: Int,
    val autoRefreshRates: Boolean,
    val exchangeRateProviderName: String,
    val showEstimatedVnd: Boolean,
    val smartSuggestionsEnabled: Boolean,
    val lastBackupAt: Long,
    val lastImportAt: Long,
    val lastBackupItemCount: Int,
    val lastBackupFileName: String,
    val dailySummaryEnabled: Boolean,
    val overdueRemindersEnabled: Boolean
)

// Helper extension to handle preference keys safely
private inline fun <T> Flow<Preferences>.data(crossinline transform: suspend (value: Preferences) -> T): Flow<T> {
    return this.map { transform(it) }
}
