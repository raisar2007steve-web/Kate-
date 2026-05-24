package com.example.api

import retrofit2.Retrofit
import androidx.compose.material3.Icon
import com.squareup.moshi.Moshi
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

data class OpenMeteoResponse(
    val current_weather: CurrentWeather?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)

data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsArticleDto>
)

data class NewsArticleDto(
    val title: String?,
    val description: String?,
    val url: String?,
    val urlToImage: String?,
    val source: NewsSourceDto?
)

data class NewsSourceDto(
    val name: String?
)

interface PublicApiService {
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current_weather: Boolean = true
    ): OpenMeteoResponse

    @GET
    suspend fun getNewsHeadlines(@Url url: String = "https://saurav.tech/NewsAPI/top-headlines/category/general/us.json"): NewsApiResponse
}

object PublicRetrofitClient {
    val service: PublicApiService by lazy {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://example.com/") // overridden by absolute URLs
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PublicApiService::class.java)
    }
}
