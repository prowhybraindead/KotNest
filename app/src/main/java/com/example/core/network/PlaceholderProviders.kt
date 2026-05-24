package com.example.core.network

import com.example.domain.model.ExchangeRateCache

class ExchangeRateApiProvider : ExchangeRateProvider {
    override val name: String = "ExchangeRate-API (Placeholder)"

    override suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache> {
        return baseCurrencies.map { base ->
            ExchangeRateCache(
                id = "${base}_${targetCurrency}",
                baseCurrency = base,
                targetCurrency = targetCurrency,
                rate = when (base) {
                    "USD" -> 25440.0
                    "EUR" -> 27500.0
                    "SGD" -> 18800.0
                    "AUD" -> 16700.0
                    "JPY" -> 162.0
                    "KRW" -> 18.2
                    "GBP" -> 32100.0
                    "CNY" -> 3500.0
                    "THB" -> 690.0
                    else -> 1.0
                },
                provider = name,
                sourceDate = "2024-05-24",
                fetchedAt = System.currentTimeMillis()
            )
        }
    }
}

class CustomBackendExchangeRateProvider : ExchangeRateProvider {
    override val name: String = "Custom Backend API (Placeholder)"

    override suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache> {
        return baseCurrencies.map { base ->
            ExchangeRateCache(
                id = "${base}_${targetCurrency}",
                baseCurrency = base,
                targetCurrency = targetCurrency,
                rate = when (base) {
                    "USD" -> 25450.0
                    "EUR" -> 27600.0
                    "SGD" -> 18900.0
                    "AUD" -> 16800.0
                    "JPY" -> 163.0
                    "KRW" -> 18.5
                    "GBP" -> 32300.0
                    "CNY" -> 3550.0
                    "THB" -> 695.0
                    else -> 1.0
                },
                provider = name,
                sourceDate = "2024-05-24",
                fetchedAt = System.currentTimeMillis()
            )
        }
    }
}
