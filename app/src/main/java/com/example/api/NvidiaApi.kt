package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NvidiaChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<NvidiaChatMessage>,
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

@JsonClass(generateAdapter = true)
data class NvidiaChatMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class NvidiaChatResponse(
    @Json(name = "choices") val choices: List<NvidiaChatChoice>?
)

@JsonClass(generateAdapter = true)
data class NvidiaChatChoice(
    @Json(name = "message") val message: NvidiaChatMessage?
)

interface NvidiaApiService {
    @POST("chat/completions")
    suspend fun generateContent(
        @Header("Authorization") authorization: String,
        @Body request: NvidiaChatRequest
    ): NvidiaChatResponse
}

object NvidiaRetrofitClient {
    private const val BASE_URL = "https://integrate.api.nvidia.com/v1/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor(headerInterceptor)
            .build()
    }

    val service: NvidiaApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(NvidiaApiService::class.java)
    }
}
