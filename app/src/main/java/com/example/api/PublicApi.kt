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

data class OpenWeatherResponse(
    val main: OpenWeatherMain,
    val weather: List<OpenWeatherCondition>,
    val wind: OpenWeatherWind,
    val name: String
)

data class OpenWeatherMain(
    val temp: Double,
    val humidity: Int,
    val temp_max: Double,
    val temp_min: Double
)

data class OpenWeatherCondition(
    val main: String,
    val description: String,
    val icon: String
)

data class OpenWeatherWind(
    val speed: Double
)

data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherForecastItem>,
    val city: OpenWeatherForecastCity
)

data class OpenWeatherForecastItem(
    val dt: Long,
    val main: OpenWeatherMain,
    val weather: List<OpenWeatherCondition>,
    val wind: OpenWeatherWind,
    val dt_txt: String
)

data class OpenWeatherForecastCity(
    val name: String,
    val country: String
)

interface PublicApiService {
    @GET("https://api.openweathermap.org/data/2.5/weather")
    suspend fun getOpenWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // Or imperial
    ): OpenWeatherResponse

    @GET("https://api.openweathermap.org/data/2.5/forecast")
    suspend fun getOpenWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherForecastResponse

    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current_weather: Boolean = true
    ): OpenMeteoResponse

    @GET
    suspend fun getNewsHeadlines(@Url url: String = "https://saurav.tech/NewsAPI/top-headlines/category/general/us.json"): NewsApiResponse

    @GET
    suspend fun getPipedTrending(@Url url: String): List<PipedTrendingItem>
}

data class PipedTrendingItem(
    val title: String? = null,
    val url: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val shortDescription: String? = null,
    val duration: Long? = 0,
    val views: Long? = 0,
    val uploaded: Long? = 0
)

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
