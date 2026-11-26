package com.example.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherSnapshot
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Real live weather client powered by Open-Meteo (free, no API key required) over OkHttp,
 * with on-demand coarse LocationManager lookup and manual city geocoding fallback.
 */
class WeatherRepository(
  private val context: Context,
) {

  private val client: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(6_000L, TimeUnit.MILLISECONDS)
      .readTimeout(6_000L, TimeUnit.MILLISECONDS)
      .callTimeout(8_000L, TimeUnit.MILLISECONDS)
      .build()

  fun hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED

  @SuppressLint("MissingPermission")
  suspend fun resolveDeviceCoarseLocation(): WeatherLocation? =
    withContext(Dispatchers.IO) {
      if (!hasLocationPermission()) return@withContext null

      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext null

      val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
      var bestLocation: Location? = null
      for (provider in providers) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (bestLocation == null || loc.time > bestLocation.time) {
          bestLocation = loc
        }
      }

      val resolved = bestLocation ?: return@withContext null
      val city = reverseGeocodeCity(resolved.latitude, resolved.longitude) ?: "Current Location"
      WeatherLocation(
        cityName = city,
        latitude = resolved.latitude,
        longitude = resolved.longitude,
      )
    }

  suspend fun geocodeCity(rawCity: String): WeatherLocation? =
    withContext(Dispatchers.IO) {
      val query = rawCity.trim()
      if (query.length < 2 || !isNetworkAvailable()) return@withContext null

      val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
      val url =
        "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"

      val request =
        Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .header("User-Agent", "NorLauncher/1.0 (Android)")
          .build()

      runCatching {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@use null
          val body = response.body?.string().orEmpty()
          val root = JSONObject(body)
          val results = root.optJSONArray("results") ?: return@use null
          if (results.length() == 0) return@use null
          val first = results.getJSONObject(0)
          val name = first.optString("name").trim()
          val country = first.optString("country_code").trim()
          val displayName =
            if (country.isNotEmpty()) "$name, $country" else name.ifEmpty { query }
          WeatherLocation(
            cityName = displayName,
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
          )
        }
      }.getOrNull()
    }

  suspend fun fetchCurrentWeather(location: WeatherLocation): WeatherSnapshot? =
    withContext(Dispatchers.IO) {
      if (!isNetworkAvailable()) return@withContext null

      val url =
        String.format(
          Locale.US,
          "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto",
          location.latitude,
          location.longitude,
        )

      val request =
        Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .header("User-Agent", "NorLauncher/1.0 (Android)")
          .build()

      runCatching {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@use null
          val body = response.body?.string().orEmpty()
          val root = JSONObject(body)
          val current = root.getJSONObject("current")
          val daily = root.optJSONObject("daily")
          val maxArr = daily?.optJSONArray("temperature_2m_max")
          val minArr = daily?.optJSONArray("temperature_2m_min")

          val tempC = current.getDouble("temperature_2m")
          val feelsC = current.optDouble("apparent_temperature", tempC)
          val highC = maxArr?.optDouble(0, tempC) ?: tempC
          val lowC = minArr?.optDouble(0, tempC) ?: tempC
          val humidity = current.optInt("relative_humidity_2m", 0)
          val windKmh = current.optDouble("wind_speed_10m", 0.0).roundToInt()
          val weatherCode = current.optInt("weather_code", 0)
          val isDay = current.optInt("is_day", 1) == 1

          WeatherSnapshot(
            cityName = location.cityName,
            temperatureC = tempC,
            feelsLikeC = feelsC,
            highC = highC,
            lowC = lowC,
            humidityPercent = humidity,
            windKmh = windKmh,
            weatherCode = weatherCode,
            isDay = isDay,
            updatedAtEpochMs = System.currentTimeMillis(),
          )
        }
      }.getOrNull()
    }

  @Suppress("DEPRECATION")
  private suspend fun reverseGeocodeCity(lat: Double, lon: Double): String? {
    if (!Geocoder.isPresent()) return null
    return runCatching {
      val geocoder = Geocoder(context, Locale.getDefault())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        withTimeoutOrNull(3_000L) {
          suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(
              lat,
              lon,
              1,
              Geocoder.GeocodeListener { addresses ->
                val addr = addresses.firstOrNull()
                val name = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                if (cont.isActive) cont.resume(name)
              },
            )
          }
        }
      } else {
        val list = geocoder.getFromLocation(lat, lon, 1)
        val addr = list?.firstOrNull()
        addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
      }
    }.getOrNull()
  }

  private fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return true
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  companion object {
    fun describeWmoCode(code: Int): String =
      when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Storm & hail"
        else -> "Partly cloudy"
      }
  }
}
