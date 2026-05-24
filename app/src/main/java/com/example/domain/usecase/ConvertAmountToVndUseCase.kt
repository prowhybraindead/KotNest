package com.example.domain.usecase

import com.example.domain.model.ExchangeRateCache

class ConvertAmountToVndUseCase {
    operator fun invoke(
        amount: Double,
        currency: String,
        rates: List<ExchangeRateCache>
    ): ConversionResult {
        if (currency.equals("VND", ignoreCase = true)) {
            return ConversionResult(
                originalAmount = amount,
                originalCurrency = "VND",
                estimatedVndAmount = amount,
                rateUsed = 1.0,
                lastUpdated = System.currentTimeMillis(),
                isRateAvailable = true
            )
        }
        
        val rateCache = rates.firstOrNull { it.baseCurrency.equals(currency, ignoreCase = true) }
        return if (rateCache != null) {
            ConversionResult(
                originalAmount = amount,
                originalCurrency = currency,
                estimatedVndAmount = amount * rateCache.rate,
                rateUsed = rateCache.rate,
                lastUpdated = rateCache.fetchedAt,
                isRateAvailable = true
            )
        } else {
            ConversionResult(
                originalAmount = amount,
                originalCurrency = currency,
                estimatedVndAmount = null,
                rateUsed = null,
                lastUpdated = null,
                isRateAvailable = false
            )
        }
    }
}

data class ConversionResult(
    val originalAmount: Double,
    val originalCurrency: String,
    val estimatedVndAmount: Double?,
    val rateUsed: Double?,
    val lastUpdated: Long?,
    val isRateAvailable: Boolean
)
