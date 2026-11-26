package com.example.data.model

enum class WallpaperPreset(val id: String, val isAlwaysDark: Boolean = false) {
  NORDIC_PEAKS("nordic_peaks", isAlwaysDark = false),
  CYBER_MESH("cyber_mesh", isAlwaysDark = true),
  MATCHA_AURA("matcha_aura", isAlwaysDark = false),
  MIDNIGHT_VAPOR("midnight_vapor", isAlwaysDark = true),
  FOREST_HORIZON("forest_horizon", isAlwaysDark = false),
  AURORA_GLASS("aurora_glass", isAlwaysDark = true),
  SUNSET_DUNES("sunset_dunes", isAlwaysDark = true),
  CUSTOM_PHOTO("custom_photo", isAlwaysDark = true);

  companion object {
    fun fromId(id: String?): WallpaperPreset =
      entries.firstOrNull { it.id == id } ?: NORDIC_PEAKS
  }
}

data class WallpaperConfig(
  val preset: WallpaperPreset = WallpaperPreset.NORDIC_PEAKS,
  val hasCustomPhoto: Boolean = false,
  val dimPercent: Int = 20,
  val photoVersion: Long = 0L,
)

enum class TemperatureUnit(val id: String) {
  CELSIUS("c"),
  FAHRENHEIT("f");

  companion object {
    fun fromId(id: String?): TemperatureUnit =
      entries.firstOrNull { it.id == id } ?: CELSIUS
  }
}

data class WeatherLocation(
  val cityName: String,
  val latitude: Double,
  val longitude: Double,
)

data class WeatherSnapshot(
  val cityName: String,
  val temperatureC: Double,
  val feelsLikeC: Double,
  val highC: Double,
  val lowC: Double,
  val humidityPercent: Int,
  val windKmh: Int,
  val weatherCode: Int,
  val isDay: Boolean,
  val updatedAtEpochMs: Long,
)
