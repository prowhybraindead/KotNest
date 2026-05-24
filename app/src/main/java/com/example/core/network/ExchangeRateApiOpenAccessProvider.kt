package com.example.core.network

import com.example.domain.model.ExchangeRateCache
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ErApiResponse(
    val result: String,
    val base_code: String,
    val time_last_update_utc: String?,
    val rates: Map<String, Double>
)

interface ErOpenAccessApi {
    @GET("v6/latest/USD")
    suspend fun getLatestUsdRates(): ErApiResponse
}

class ExchangeRateApiOpenAccessProvider : ExchangeRateProvider {
    override val name: String = "ExchangeRate-API Open Access"

    private val api: ErOpenAccessApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        retrofit.create(ErOpenAccessApi::class.java)
    }

    override suspend fun getExchangeRates(
        baseCurrencies: List<String>,
        targetCurrency: String
    ): List<ExchangeRateCache> {
        val response = api.getLatestUsdRates()
        if (response.result != "success") {
            throw Exception("API returned unsuccessful result")
        }
        
        val rates = response.rates
        val targetToUsdRate = rates[targetCurrency] ?: throw Exception("Target currency $targetCurrency not found in API rates")

        val fetchedTime = System.currentTimeMillis()
        val sourceDate = response.time_last_update_utc?.substringBefore(" +") ?: "Latest"

        val resultList = mutableListOf<ExchangeRateCache>()
        for (base in baseCurrencies) {
            val baseToUsdRate = rates[base] ?: continue
            if (baseToUsdRate <= 0.0) continue
            val finalRate = targetToUsdRate / baseToUsdRate
            resultList.add(
                ExchangeRateCache(
                    id = "${base}_${targetCurrency}",
                    baseCurrency = base,
                    targetCurrency = targetCurrency,
                    rate = finalRate,
                    provider = name,
                    fetchedAt = fetchedTime,
                    sourceDate = sourceDate
                )
            )
        }
        return resultList
    }
}
