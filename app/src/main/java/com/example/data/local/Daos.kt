package com.example.data.local

import androidx.room.*
import com.example.domain.model.Category
import com.example.domain.model.PaymentHistory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderRule
import com.example.domain.model.Subscription
import com.example.domain.model.ExchangeRateCache
import com.example.domain.model.ExchangeRateHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
    suspend fun getPaymentMethodById(id: Int): PaymentMethod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethod): Long

    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethod)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextDueDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    fun getSubscriptionById(id: Int): Flow<Subscription?>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionByIdDirect(id: Int): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Int)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
}

@Dao
interface PaymentHistoryDao {
    @Query("SELECT * FROM payment_histories ORDER BY paidDate DESC")
    fun getAllHistory(): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_histories WHERE subscriptionId = :subscriptionId ORDER BY paidDate DESC")
    fun getHistoryForSubscription(subscriptionId: Int): Flow<List<PaymentHistory>>

    @Query("SELECT * FROM payment_histories WHERE subscriptionId = :subscriptionId ORDER BY paidDate DESC")
    suspend fun getHistoryForSubscriptionDirect(subscriptionId: Int): List<PaymentHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PaymentHistory): Long

    @Delete
    suspend fun deleteHistory(history: PaymentHistory)
}

@Dao
interface ReminderRuleDao {
    @Query("SELECT * FROM reminder_rules WHERE subscriptionId = :subscriptionId")
    fun getRulesForSubscription(subscriptionId: Int): Flow<List<ReminderRule>>

    @Query("SELECT * FROM reminder_rules WHERE subscriptionId = :subscriptionId")
    suspend fun getRulesForSubscriptionDirect(subscriptionId: Int): List<ReminderRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ReminderRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ReminderRule>)

    @Query("DELETE FROM reminder_rules WHERE subscriptionId = :subscriptionId")
    suspend fun deleteRulesForSubscription(subscriptionId: Int)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rate_caches ORDER BY baseCurrency ASC")
    fun getAllRatesFlow(): Flow<List<ExchangeRateCache>>

    @Query("SELECT * FROM exchange_rate_caches ORDER BY baseCurrency ASC")
    suspend fun getAllRatesDirect(): List<ExchangeRateCache>

    @Query("SELECT * FROM exchange_rate_caches WHERE id = :id LIMIT 1")
    suspend fun getRateById(id: String): ExchangeRateCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateCache>)

    @Query("DELETE FROM exchange_rate_caches")
    suspend fun clearAllRates()

    @Query("SELECT * FROM exchange_rate_histories WHERE targetCurrency = :targetCurrency ORDER BY date ASC")
    fun getAllHistoryFlow(targetCurrency: String): Flow<List<ExchangeRateHistory>>

    @Query("SELECT * FROM exchange_rate_histories WHERE targetCurrency = :targetCurrency ORDER BY date ASC")
    suspend fun getAllHistoryDirect(targetCurrency: String): List<ExchangeRateHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: List<ExchangeRateHistory>)
}
