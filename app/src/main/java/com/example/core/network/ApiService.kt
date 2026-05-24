package com.example.core.network

import com.example.domain.model.Subscription
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class BackendHealthResponse(
    val status: String = "",
    val service: String = "",
    val time: String = ""
)

data class AiInsightResponse(
    val brandName: String,
    val provider: String,
    val model: String,
    val filtered: Boolean,
    val insight: String,
    val actions: List<String>,
    val generatedAt: Long
)

data class AiModelConfigResponse(
    val brandName: String,
    val rulesVersion: String,
    val defaultProvider: String,
    val defaultModel: String,
    val providers: Map<String, List<String>>
)

data class AiChatMessageDto(
    val role: String,
    val content: String
)

data class AiChatRequest(
    val provider: String? = null,
    val model: String? = null,
    val message: String,
    val history: List<AiChatMessageDto> = emptyList(),
    val focus: String? = null,
    val enableWebSearch: Boolean = true
)

data class AiCitationDto(
    val title: String,
    val url: String,
    val snippet: String,
    val source: String
)

data class AiChatResponse(
    val brandName: String,
    val provider: String,
    val model: String,
    val filtered: Boolean,
    val rulesVersion: String,
    val answer: String,
    val citations: List<AiCitationDto>,
    val generatedAt: Long
)

interface ApiService {
    @GET("/healthz")
    suspend fun healthz(): Response<BackendHealthResponse>

    @GET("/api/subscriptions")
    suspend fun getSubscriptions(
        @Query("device_id") deviceId: String
    ): Response<List<Subscription>>

    @POST("/api/subscriptions/sync")
    suspend fun syncSubscriptions(
        @Body subscriptions: List<Subscription>,
        @Query("device_id") deviceId: String
    ): Response<Void>

    @GET("/api/ai/insights")
    suspend fun getAiInsights(
        @Query("device_id") deviceId: String,
        @Query("focus") focus: String? = null,
        @Query("provider") provider: String? = null,
        @Query("model") model: String? = null
    ): Response<AiInsightResponse>

    @GET("/api/ai/models")
    suspend fun getAiModels(): Response<AiModelConfigResponse>

    @POST("/api/ai/chat")
    suspend fun chatWithAi(
        @Body request: AiChatRequest,
        @Query("device_id") deviceId: String
    ): Response<AiChatResponse>
}

object RetrofitClient {
    private var apiService: ApiService? = null
    private var currentConfigKey: String? = null

    fun getClient(
        baseUrl: String,
        apiToken: String? = null,
        deviceId: String? = null
    ): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val configKey = "${normalizedUrl}|${apiToken ?: ""}|${deviceId ?: ""}"

        if (apiService != null && currentConfigKey == configKey) {
            return apiService!!
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                if (!apiToken.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $apiToken")
                }
                if (!deviceId.isNullOrBlank()) {
                    builder.header("X-Device-Id", deviceId)
                }
                chain.proceed(builder.build())
            }
            .retryOnConnectionFailure(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                )
            )
            .build()

        currentConfigKey = configKey
        apiService = retrofit.create(ApiService::class.java)
        return apiService!!
    }
}
