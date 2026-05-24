package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val currency: String = "VND",
    val categoryId: Int, // references Category.id
    val billingCycle: String, // "One-time", "Weekly", "Monthly", "Yearly", "Every X days", "Every X months"
    val customCycleValue: Int = 1,
    val nextDueDate: Long, // Epoch milliseconds
    val paymentMethodId: Int? = null, // references PaymentMethod.id
    val isAutoRenew: Boolean = true,
    val status: String = "Upcoming", // "Upcoming", "Due Today", "Paid", "Overdue", "Paused"
    val importance: String = "Medium", // "Low", "Medium", "High"
    val note: String? = null,
    val managementUrl: String? = null,
    val isPaused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String, // Icon identifier name (e.g. "cloud", "movie", etc.)
    val color: String, // Hex color string (e.g. "#FF5722")
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Card", "Cash", "Bank Transfer", "PayPal", "Other"
    val lastFourDigits: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_histories")
data class PaymentHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionId: Int, // references Subscription.id
    val paidDate: Long, // Epoch milliseconds
    val amount: Double,
    val currency: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminder_rules")
data class ReminderRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionId: Int, // references Subscription.id
    val daysBefore: Int, // e.g. 0 (today), 1, 3, 7
    val reminderTime: String = "09:00", // "HH:mm" formatted
    val enabled: Boolean = true
)

@Entity(tableName = "exchange_rate_caches")
data class ExchangeRateCache(
    @PrimaryKey val id: String, // Format: "${baseCurrency}_${targetCurrency}"
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val provider: String,
    val fetchedAt: Long = System.currentTimeMillis(),
    val sourceDate: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "exchange_rate_histories",
    primaryKeys = ["currencyCode", "targetCurrency", "date"]
)
data class ExchangeRateHistory(
    val currencyCode: String,
    val targetCurrency: String,
    val date: String, // "yyyy-MM-dd"
    val rateToVnd: Double,
    val provider: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
