package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.PaymentHistory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderRule
import com.example.domain.model.Subscription
import com.example.domain.model.ExchangeRateCache
import kotlinx.coroutines.flow.Flow

interface DueMateRepository {
    // Subscriptions
    fun getAllSubscriptions(): Flow<List<Subscription>>
    fun getSubscriptionById(id: Int): Flow<Subscription?>
    suspend fun getSubscriptionByIdDirect(id: Int): Subscription?
    suspend fun saveSubscription(subscription: Subscription, reminderRules: List<ReminderRule>): Long
    suspend fun deleteSubscription(id: Int)
    suspend fun markAsPaid(subscriptionId: Int, note: String?): Boolean

    // Categories
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun deleteCategory(id: Int)

    // Payment Methods
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun getPaymentMethodById(id: Int): PaymentMethod?
    suspend fun savePaymentMethod(paymentMethod: PaymentMethod): Long
    suspend fun deletePaymentMethod(id: Int)

    // Histories
    fun getAllHistory(): Flow<List<PaymentHistory>>
    fun getHistoryForSubscription(subscriptionId: Int): Flow<List<PaymentHistory>>
    suspend fun addHistory(history: PaymentHistory): Long

    // Reminder Rules
    fun getReminderRulesForSubscription(subscriptionId: Int): Flow<List<ReminderRule>>

    // Currency Rates Caching
    fun getAllRatesFlow(): Flow<List<ExchangeRateCache>>
    suspend fun getAllRatesDirect(): List<ExchangeRateCache>
    suspend fun saveRates(rates: List<ExchangeRateCache>)
    suspend fun clearAllRates()

    // Currency Rates History
    fun getAllHistoryFlow(targetCurrency: String = "VND"): Flow<List<com.example.domain.model.ExchangeRateHistory>>
    suspend fun getAllHistoryDirect(targetCurrency: String = "VND"): List<com.example.domain.model.ExchangeRateHistory>
    suspend fun saveHistory(history: List<com.example.domain.model.ExchangeRateHistory>)
}
