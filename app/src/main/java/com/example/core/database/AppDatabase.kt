package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.*
import com.example.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Subscription::class,
        Category::class,
        PaymentMethod::class,
        PaymentHistory::class,
        ReminderRule::class,
        ExchangeRateCache::class,
        ExchangeRateHistory::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao
    abstract fun reminderRuleDao(): ReminderRuleDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "duemate_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            try {
                val now = System.currentTimeMillis()
                // Insert categories with nice emojis as icon fields
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (1, 'Cloud / Storage', '☁️', '#2196F3', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (2, 'Entertainment', '🎬', '#E91E63', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (3, 'Education', '🎓', '#9C27B0', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (4, 'Server / Tech', '🖥️', '#607D8B', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (5, 'Internet / Phone', '📶', '#00BCD4', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (6, 'Work', '💼', '#FF9800', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (7, 'Personal', '👤', '#4CAF50', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO categories (id, name, icon, color, createdAt, updatedAt) VALUES (8, 'Other', '📦', '#9E9E9E', $now, $now)")

                // Insert default payment methods
                db.execSQL("INSERT OR IGNORE INTO payment_methods (id, name, type, lastFourDigits, note, createdAt, updatedAt) VALUES (1, 'Cash', 'Cash', NULL, 'Cash wallet', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO payment_methods (id, name, type, lastFourDigits, note, createdAt, updatedAt) VALUES (2, 'Default Credit Card', 'Card', '1234', 'Credit card', $now, $now)")
                db.execSQL("INSERT OR IGNORE INTO payment_methods (id, name, type, lastFourDigits, note, createdAt, updatedAt) VALUES (3, 'Bank Account', 'Bank Transfer', NULL, 'Primary bank account', $now, $now)")

                // Insert pre-seeded 7-day rate history for charts
                val currencies = listOf(
                    "USD" to listOf(25400.0, 25410.0, 25430.0, 25420.0, 25450.0, 25440.0, 25445.0),
                    "EUR" to listOf(27500.0, 27520.0, 27480.0, 27550.0, 27600.0, 27530.0, 27570.0),
                    "SGD" to listOf(18800.0, 18780.0, 18820.0, 18850.0, 18810.0, 18830.0, 18840.0),
                    "AUD" to listOf(16700.0, 16720.0, 16680.0, 16710.0, 16750.0, 16730.0, 16740.0),
                    "JPY" to listOf(161.0, 161.5, 161.2, 161.8, 162.1, 161.9, 162.0),
                    "KRW" to listOf(18.2, 18.1, 18.3, 18.4, 18.2, 18.5, 18.4),
                    "GBP" to listOf(32100.0, 32150.0, 32080.0, 32200.0, 32250.0, 32180.0, 32220.0),
                    "CNY" to listOf(3500.0, 3505.0, 3498.0, 3512.0, 3508.0, 3515.0, 3510.0),
                    "THB" to listOf(690.0, 688.0, 692.0, 695.0, 691.0, 693.0, 694.0)
                )
                val dates = listOf("2026-05-17", "2026-05-18", "2026-05-19", "2026-05-20", "2026-05-21", "2026-05-22", "2026-05-23")
                currencies.forEach { (code, ratesList) ->
                    dates.forEachIndexed { index, date ->
                        val r = ratesList[index]
                        db.execSQL("INSERT OR IGNORE INTO exchange_rate_histories (currencyCode, targetCurrency, date, rateToVnd, provider, createdAt, updatedAt) VALUES ('$code', 'VND', '$date', $r, 'Pre-seed', $now, $now)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
