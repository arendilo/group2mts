package com.example.group2mts.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LiveWeather(
    val tempC: Double,
    val humidityPct: Int,
    val condition: String
)

object WeatherService {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp)

    @Serializable
    private data class OpenMeteoResponse(
        val current: CurrentBlock? = null
    )

    @Serializable
    private data class CurrentBlock(
        @SerialName("temperature_2m") val temperature2m: Double,
        @SerialName("relative_humidity_2m") val relativeHumidity2m: Int,
        @SerialName("weather_code") val weatherCode: Int
    )

    /**
     * Fetches live current weather from Open-Meteo (no API key or location permission needed).
     * Defaults to South Tangerang / Jakarta coordinates (-6.2886, 106.7179).
     */
    suspend fun fetchCurrentWeather(
        latitude: Double = -6.2886,
        longitude: Double = 106.7179
    ): LiveWeather {
        val url =
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code" +
                    "&timezone=auto"

        val raw = client.get(url).bodyAsText()
        val parsed = json.decodeFromString<OpenMeteoResponse>(raw)
        val current = parsed.current ?: error("Missing current weather block")

        return LiveWeather(
            tempC = current.temperature2m,
            humidityPct = current.relativeHumidity2m,
            condition = mapWmoCode(current.weatherCode)
        )
    }

    private fun mapWmoCode(code: Int): String = when (code) {
        0 -> "Sunny"
        1, 2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rainy"
        in 71..77, 85, 86 -> "Snowy"
        in 95..99 -> "Thunderstorm"
        else -> "Cloudy"
    }
}