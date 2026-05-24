package com.example.core.network

import com.example.domain.model.ExchangeRateCache
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
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

interface FrankfurterApi {
    @GET("latest")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): FrankfurterResponse
}

class FrankfurterExchangeRateProvider : ExchangeRateProvider {
    override val name: String = "Frankfurter API"

    private val api: FrankfurterApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        retrofit.create(FrankfurterApi::class.java)
    }

    override suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache> = coroutineScope {
        baseCurrencies.map { base ->
            async {
                try {
                    val response = api.getLatestRates(base = base, symbols = targetCurrency)
                    val rateValue = response.rates[targetCurrency]
                    if (rateValue != null) {
                        ExchangeRateCache(
                            id = "${base}_${targetCurrency}",
                            baseCurrency = base,
                            targetCurrency = targetCurrency,
                            rate = rateValue,
                            provider = name,
                            sourceDate = response.date,
                            fetchedAt = System.currentTimeMillis()
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
