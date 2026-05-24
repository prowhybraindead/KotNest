package com.example.data.repository

import com.example.core.database.AppDatabase
import com.example.core.util.DateUtils
import com.example.domain.model.Category
import com.example.domain.model.PaymentHistory
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderRule
import com.example.domain.model.Subscription
import com.example.domain.model.ExchangeRateCache
import com.example.domain.repository.DueMateRepository
import kotlinx.coroutines.flow.Flow

class DueMateRepositoryImpl(
    private val database: AppDatabase
) : DueMateRepository {

    private val subscriptionDao = database.subscriptionDao()
    private val categoryDao = database.categoryDao()
    private val paymentMethodDao = database.paymentMethodDao()
    private val paymentHistoryDao = database.paymentHistoryDao()
    private val reminderRuleDao = database.reminderRuleDao()
    private val exchangeRateDao = database.exchangeRateDao()

    override fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()

    override fun getSubscriptionById(id: Int): Flow<Subscription?> = subscriptionDao.getSubscriptionById(id)

    override suspend fun getSubscriptionByIdDirect(id: Int): Subscription? = subscriptionDao.getSubscriptionByIdDirect(id)

    override suspend fun saveSubscription(subscription: Subscription, reminderRules: List<ReminderRule>): Long {
        val id = subscriptionDao.insertSubscription(subscription)
        val finalId = if (subscription.id == 0) id.toInt() else subscription.id
        
        // Update reminder rules for this subscription
        reminderRuleDao.deleteRulesForSubscription(finalId)
        val rulesWithSubId = reminderRules.map { it.copy(subscriptionId = finalId) }
        reminderRuleDao.insertRules(rulesWithSubId)
        
        return id
    }

    override suspend fun deleteSubscription(id: Int) {
        subscriptionDao.deleteSubscriptionById(id)
        reminderRuleDao.deleteRulesForSubscription(id)
    }

    override suspend fun markAsPaid(subscriptionId: Int, note: String?): Boolean {
        val subscription = subscriptionDao.getSubscriptionByIdDirect(subscriptionId) ?: return false
        
        // 1. Create paid history
        val history = PaymentHistory(
            subscriptionId = subscriptionId,
            paidDate = subscription.nextDueDate,
            amount = subscription.amount,
            currency = subscription.currency,
            note = note ?: "Paid renewal subscription"
        )
        paymentHistoryDao.insertHistory(history)

        // 2. Compute next due date depending on billing cycle
        val nextDueDate = if (subscription.billingCycle == "One-time") {
            subscription.nextDueDate // remains the same
        } else {
            DateUtils.calculateNextDueDate(
                currentEpoch = subscription.nextDueDate,
                billingCycle = subscription.billingCycle,
                customValue = subscription.customCycleValue
            )
        }

        // 3. Update status
        val updatedStatus = if (subscription.billingCycle == "One-time") {
            "Paid"
        } else {
            // Determine if the advanced next due date is already overdue/due today, otherwise "Upcoming"
            val now = System.currentTimeMillis()
            val todayStart = DateUtils.getEpochFromLocalDate(DateUtils.getLocalDateFromEpoch(now))
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
        subscriptionDao.insertSubscription(updatedSub)
        return true
    }

    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)

    override suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    override suspend fun deleteCategory(id: Int) {
        val cat = categoryDao.getCategoryById(id)
        if (cat != null) {
            categoryDao.deleteCategory(cat)
        }
    }

    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> = paymentMethodDao.getAllPaymentMethods()

    override suspend fun getPaymentMethodById(id: Int): PaymentMethod? = paymentMethodDao.getPaymentMethodById(id)

    override suspend fun savePaymentMethod(paymentMethod: PaymentMethod): Long = paymentMethodDao.insertPaymentMethod(paymentMethod)

    override suspend fun deletePaymentMethod(id: Int) {
        val method = paymentMethodDao.getPaymentMethodById(id)
        if (method != null) {
            paymentMethodDao.deletePaymentMethod(method)
        }
    }

    override fun getAllHistory(): Flow<List<PaymentHistory>> = paymentHistoryDao.getAllHistory()

    override fun getHistoryForSubscription(subscriptionId: Int): Flow<List<PaymentHistory>> = paymentHistoryDao.getHistoryForSubscription(subscriptionId)

    override suspend fun addHistory(history: PaymentHistory): Long = paymentHistoryDao.insertHistory(history)

    override fun getReminderRulesForSubscription(subscriptionId: Int): Flow<List<ReminderRule>> = reminderRuleDao.getRulesForSubscription(subscriptionId)

    override fun getAllRatesFlow(): Flow<List<com.example.domain.model.ExchangeRateCache>> = exchangeRateDao.getAllRatesFlow()

    override suspend fun getAllRatesDirect(): List<com.example.domain.model.ExchangeRateCache> = exchangeRateDao.getAllRatesDirect()

    override suspend fun saveRates(rates: List<com.example.domain.model.ExchangeRateCache>) {
        exchangeRateDao.insertRates(rates)
    }

    override suspend fun clearAllRates() {
        exchangeRateDao.clearAllRates()
    }

    override fun getAllHistoryFlow(targetCurrency: String): Flow<List<com.example.domain.model.ExchangeRateHistory>> =
        exchangeRateDao.getAllHistoryFlow(targetCurrency)

    override suspend fun getAllHistoryDirect(targetCurrency: String): List<com.example.domain.model.ExchangeRateHistory> =
        exchangeRateDao.getAllHistoryDirect(targetCurrency)

    override suspend fun saveHistory(history: List<com.example.domain.model.ExchangeRateHistory>) {
        exchangeRateDao.insertHistory(history)
    }
}
