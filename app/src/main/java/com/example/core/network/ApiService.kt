package com.example.core.network

import com.example.domain.model.Subscription
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("api/subscriptions")
    suspend fun getSubscriptions(): Response<List<Subscription>>

    @POST("api/subscriptions/sync")
    suspend fun syncSubscriptions(@Body subscriptions: List<Subscription>): Response<Void>
}

object RetrofitClient {
    private var apiService: ApiService? = null
    private var currentBaseUrl: String? = null

    fun getClient(baseUrl: String): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        if (apiService != null && currentBaseUrl == normalizedUrl) {
            return apiService!!
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        currentBaseUrl = normalizedUrl
        apiService = retrofit.create(ApiService::class.java)
        return apiService!!
    }
}
