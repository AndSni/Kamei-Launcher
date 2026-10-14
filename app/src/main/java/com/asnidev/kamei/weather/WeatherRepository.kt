package com.asnidev.kamei.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.asnidev.kamei.BuildConfig
import com.asnidev.kamei.data.LocationMode
import com.asnidev.kamei.data.Place
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.WeatherSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

enum class WeatherStatus { OFF, NO_LOCATION, UPDATING, OK, ERROR }

data class WeatherState(val status: WeatherStatus, val forecast: Forecast? = null)

/**
 * Fetches the forecast only when weather is switched on, at most every
 * [MIN_INTERVAL] (more often while rain is close), and only from the service the
 * user chose. Coordinates are rounded to two decimals (about 1 km) before they
 * leave the phone. Nothing else is sent: no account, no identifier.
 */
class WeatherRepository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "weather.json")
    private val _state = MutableStateFlow(WeatherState(WeatherStatus.OFF, runCatching { Weather.read(cacheFile.readText()) }.getOrNull()))
    val state: StateFlow<WeatherState> = _state
    private val mutex = Mutex()

    suspend fun refresh(prefs: Prefs, force: Boolean = false) = mutex.withLock {
        if (!prefs.weatherOn) {
            _state.value = WeatherState(WeatherStatus.OFF, null)
            runCatching { cacheFile.delete() }
            return@withLock
        }
        val old = _state.value.forecast
        val now = System.currentTimeMillis()
        if (!force && old != null && now - old.fetchedAt < interval(old, now)) {
            _state.value = WeatherState(WeatherStatus.OK, old)
            return@withLock
        }
        val place = location(prefs)
        if (place == null) {
            _state.value = WeatherState(WeatherStatus.NO_LOCATION, old)
            return@withLock
        }
        _state.value = WeatherState(WeatherStatus.UPDATING, old)
        val fresh = withContext(Dispatchers.IO) {
            runCatching { fetch(prefs.weatherSource, place.first, place.second, now) }
                .onFailure { Log.w(TAG, "weather fetch failed", it) }.getOrNull()
        }
        if (fresh != null) {
            runCatching { cacheFile.writeText(Weather.write(fresh)) }
            _state.value = WeatherState(WeatherStatus.OK, fresh)
        } else {
            _state.value = WeatherState(WeatherStatus.ERROR, old)
        }
    }

    /** Rain within the next two hours: look again every 10 minutes, else every 30. */
    private fun interval(f: Forecast, now: Long): Long {
        val o = Weather.outlook(f, now)
        return if (o is Outlook.Now || (o is Outlook.Soon && o.inMin < 120)) 10 * 60_000L else MIN_INTERVAL
    }

    private fun fetch(source: WeatherSource, lat: Double, lon: Double, now: Long): Forecast = when (source) {
        WeatherSource.OPEN_METEO -> Weather.parseOpenMeteo(get(openMeteoUrl(lat, lon)), now)
        WeatherSource.MET -> {
            val f = Weather.parseMet(get(metUrl("locationforecast/2.0/compact", lat, lon)), now)
            nowcast(lat, lon)?.let { f.copy(steps = it, source = "Yr + radar") } ?: f
        }
        WeatherSource.AUTO -> {
            val f = Weather.parseMet(get(metUrl("locationforecast/2.0/compact", lat, lon)), now)
            nowcast(lat, lon)?.let { f.copy(steps = it, source = "Yr + radar") }
                ?: runCatching { Weather.parseOpenMeteo(get(openMeteoUrl(lat, lon)), now) }.getOrNull()
                    ?.takeIf { it.steps.isNotEmpty() }?.let { f.copy(steps = it.steps, source = "Yr + Open-Meteo") }
                ?: f
        }
    }

    /** Yr's radar nowcast covers the Nordic countries only; elsewhere it answers 404 or has no coverage. */
    private fun nowcast(lat: Double, lon: Double): List<PrecipStep>? =
        runCatching { Weather.parseNowcast(get(metUrl("nowcast/2.0/complete", lat, lon))) }.getOrNull()

    private fun metUrl(path: String, lat: Double, lon: Double) =
        "https://api.met.no/weatherapi/$path?lat=${round(lat)}&lon=${round(lon)}"

    private fun openMeteoUrl(lat: Double, lon: Double) =
        "https://api.open-meteo.com/v1/forecast?latitude=${round(lat)}&longitude=${round(lon)}" +
            "&current=temperature_2m,weather_code,is_day&minutely_15=precipitation&forecast_minutely_15=12" +
            "&hourly=temperature_2m,weather_code,precipitation&forecast_hours=12&timeformat=unixtime&timezone=GMT"

    private fun round(v: Double) = String.format(Locale.ROOT, "%.2f", v)

    /** Place search for a fixed location (Open-Meteo's geocoder; only the typed text is sent). */
    suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val url = "https://geocoding-api.open-meteo.com/v1/search?count=8&format=json&language=en&name=" +
                URLEncoder.encode(query.trim(), "UTF-8")
            val results = JSONObject(get(url)).optJSONArray("results") ?: return@runCatching emptyList()
            (0 until results.length()).map { i ->
                val r = results.getJSONObject(i)
                val name = listOf(r.optString("name"), r.optString("admin1"), r.optString("country"))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                Place(name, r.getDouble("latitude"), r.getDouble("longitude"))
            }
        }.onFailure { Log.w(TAG, "place search failed", it) }.getOrDefault(emptyList())
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            // MET Norway's terms ask every app to identify itself.
            c.setRequestProperty("User-Agent", "Kamei/${BuildConfig.VERSION_NAME} github.com/AndSni")
            c.setRequestProperty("Accept", "application/json")
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode} for ${url.substringBefore('?')}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    // ---- location ----

    fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private suspend fun location(prefs: Prefs): Pair<Double, Double>? = when (prefs.locationMode) {
        LocationMode.FIXED -> prefs.place?.let { it.lat to it.lon }
        LocationMode.DEVICE -> deviceLocation()?.let { it.latitude to it.longitude }
    }

    /** The freshest approximate fix the phone already has; asks for a new one when that's over 3 hours old. */
    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Location? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 3 * Weather.HOUR) return last
        val provider = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).firstOrNull { it in providers }
            ?: return last
        if (Build.VERSION.SDK_INT < 30) return last
        val fresh = withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
        return fresh ?: last
    }

    private companion object {
        const val TAG = "Weather"
        const val MIN_INTERVAL = 30 * 60_000L
    }
}
