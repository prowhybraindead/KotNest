package com.example.core.network

import com.example.domain.model.ExchangeRateCache

interface ExchangeRateProvider {
    val name: String
    suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache>
}
