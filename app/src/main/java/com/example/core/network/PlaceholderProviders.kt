package com.example.core.network

import com.example.domain.model.ExchangeRateCache
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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

@JsonClass(generateAdapter = true)
data class BackendExchangeRateResponse(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val sourceDate: String,
    val provider: String,
    val cached: Boolean
)

interface CustomBackendExchangeRateApi {
    @GET("api/exchange-rate")
    suspend fun getExchangeRate(
        @Query("base") base: String,
        @Query("target") target: String
    ): BackendExchangeRateResponse
}

class CustomBackendExchangeRateProvider : ExchangeRateProvider {
    override val name: String = "Custom Backend API"

    private val backendBaseUrl: String

    constructor(baseUrl: String) {
        backendBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private val api: CustomBackendExchangeRateApi by lazy {
        if (backendBaseUrl.isBlank()) {
            throw IllegalArgumentException("Backend base URL is empty.")
        }
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(backendBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        retrofit.create(CustomBackendExchangeRateApi::class.java)
    }

    override suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache> = coroutineScope {
        baseCurrencies.map { base ->
            async {
                try {
                    val response = api.getExchangeRate(base = base, target = targetCurrency)
                    ExchangeRateCache(
                        id = "${base}_${targetCurrency}",
                        baseCurrency = response.baseCurrency,
                        targetCurrency = response.targetCurrency,
                        rate = response.rate,
                        provider = response.provider,
                        sourceDate = response.sourceDate,
                        fetchedAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
