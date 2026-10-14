package com.asnidev.kamei.weather

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

enum class Condition { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, HEAVY_RAIN, SHOWERS, SLEET, SNOW, THUNDER }

/** Precipitation from [time] until the next step, in mm per hour. */
data class PrecipStep(val time: Long, val mmPerHour: Double)

data class Hour(val time: Long, val tempC: Double, val condition: Condition, val mm: Double, val night: Boolean)

data class Forecast(
    val fetchedAt: Long,
    /** Who it came from, for the settings page ("Yr + radar", "Open-Meteo"…). */
    val source: String,
    val tempC: Double?,
    val condition: Condition,
    val night: Boolean,
    /** Fine-grained precipitation for the next couple of hours (5 or 15 minute steps), oldest first. */
    val steps: List<PrecipStep>,
    /** Hourly forecast for the next half day. */
    val hours: List<Hour>,
)

/** What the weather line should say about rain or snow, the most useful thing first. */
sealed interface Outlook {
    val snow: Boolean

    data class Now(val stopsInMin: Int?, override val snow: Boolean) : Outlook
    data class Soon(val inMin: Int, override val snow: Boolean) : Outlook
    data class Later(val at: Long, override val snow: Boolean) : Outlook
    data object Dry : Outlook {
        override val snow = false
    }
}

object Weather {

    /** Below this it isn't worth an umbrella (a light drizzle is about 0.2 mm/h). */
    const val WET = 0.1

    fun outlook(f: Forecast, now: Long): Outlook {
        val snow = f.condition == Condition.SNOW || f.condition == Condition.SLEET || (f.tempC != null && f.tempC <= 0.5)
        val steps = f.steps
        if (steps.isNotEmpty()) {
            val step = if (steps.size >= 2) steps[1].time - steps[0].time else HOUR
            // The step covering now; none once the steps have all run out (stale data falls back to the hours).
            val current = steps.indexOfLast { it.time <= now }.takeIf { it >= 0 && now < steps[it].time + step }
            if (current != null && steps[current].mmPerHour >= WET) {
                val dry = steps.drop(current + 1).firstOrNull { it.mmPerHour < WET }
                return Outlook.Now(dry?.let { minutes(now, it.time) }, snow)
            }
            steps.firstOrNull { it.time > now && it.mmPerHour >= WET }?.let { return Outlook.Soon(minutes(now, it.time), snow) }
        }
        val horizon = now + 6 * HOUR
        val firstHour = f.hours.firstOrNull { it.time + HOUR > now }
        if (steps.isEmpty() && firstHour != null && firstHour.time <= now && firstHour.mm >= WET) return Outlook.Now(null, snow)
        f.hours.firstOrNull { it.time > now && it.time <= horizon && it.mm >= 0.2 }?.let { return Outlook.Later(it.time, snow) }
        return Outlook.Dry
    }

    /**
     * How blue the weather line is, 0..1: full while it rains, fading out as the
     * start of rain lies further ahead, gone past [windowMin] minutes.
     */
    fun blueness(o: Outlook, windowMin: Int): Float = when (o) {
        is Outlook.Now -> 1f
        is Outlook.Soon -> if (o.inMin >= windowMin) 0f else (0.25f + 0.75f * (1f - o.inMin.toFloat() / windowMin))
        else -> 0f
    }

    private fun minutes(now: Long, at: Long) = ((at - now + 59_999) / 60_000).toInt().coerceAtLeast(1)

    const val HOUR = 3_600_000L

    // ---- MET Norway (Yr) ----

    /** Locationforecast 2.0 "compact". */
    fun parseMet(json: String, fetchedAt: Long): Forecast {
        val series = JSONObject(json).getJSONObject("properties").getJSONArray("timeseries")
        val hours = ArrayList<Hour>()
        var tempNow: Double? = null
        var condNow = Condition.CLOUDY
        var nightNow = false
        for (i in 0 until series.length()) {
            val ts = series.getJSONObject(i)
            val time = Instant.parse(ts.getString("time")).toEpochMilli()
            val data = ts.getJSONObject("data")
            val temp = data.getJSONObject("instant").getJSONObject("details").optDouble("air_temperature", Double.NaN)
            val next = data.optJSONObject("next_1_hours") ?: continue
            val symbol = next.optJSONObject("summary")?.optString("symbol_code").orEmpty()
            val mm = next.optJSONObject("details")?.optDouble("precipitation_amount", 0.0) ?: 0.0
            val hour = Hour(time, temp, metCondition(symbol), mm, symbol.endsWith("_night"))
            if (time + HOUR <= fetchedAt) continue
            if (tempNow == null) { tempNow = temp.takeIf { !it.isNaN() }; condNow = hour.condition; nightNow = hour.night }
            if (hours.size < 12) hours += hour
        }
        return Forecast(fetchedAt, "Yr", tempNow, condNow, nightNow, hours.map { PrecipStep(it.time, it.mm) }, hours)
    }

    /** Nowcast 2.0 "complete": 5-minute precipitation for two hours, or null without radar coverage. */
    fun parseNowcast(json: String): List<PrecipStep>? {
        val props = JSONObject(json).getJSONObject("properties")
        val coverage = props.optJSONObject("meta")?.optString("radar_coverage")
        if (coverage != null && coverage != "ok") return null
        val series = props.getJSONArray("timeseries")
        val out = ArrayList<PrecipStep>()
        for (i in 0 until series.length()) {
            val ts = series.getJSONObject(i)
            val rate = ts.getJSONObject("data").getJSONObject("instant").getJSONObject("details").optDouble("precipitation_rate", Double.NaN)
            if (rate.isNaN()) return null
            out += PrecipStep(Instant.parse(ts.getString("time")).toEpochMilli(), rate)
        }
        return out.takeIf { it.isNotEmpty() }
    }

    fun metCondition(symbol: String): Condition {
        val s = symbol.substringBefore('_')
        return when {
            "thunder" in s -> Condition.THUNDER
            "sleet" in s -> Condition.SLEET
            "snow" in s -> Condition.SNOW
            "showers" in s -> Condition.SHOWERS
            s == "heavyrain" -> Condition.HEAVY_RAIN
            s == "lightrain" -> Condition.DRIZZLE
            "rain" in s -> Condition.RAIN
            s == "fog" -> Condition.FOG
            s == "cloudy" -> Condition.CLOUDY
            s == "partlycloudy" || s == "fair" -> Condition.PARTLY_CLOUDY
            s == "clearsky" -> Condition.CLEAR
            else -> Condition.CLOUDY
        }
    }

    // ---- Open-Meteo ----

    /** /v1/forecast with current, minutely_15 and hourly, timeformat=unixtime. */
    fun parseOpenMeteo(json: String, fetchedAt: Long): Forecast {
        val o = JSONObject(json)
        val current = o.optJSONObject("current")
        val night = current?.optInt("is_day", 1) == 0
        val hourly = o.optJSONObject("hourly")
        val hours = ArrayList<Hour>()
        if (hourly != null) {
            val t = hourly.getJSONArray("time")
            val temp = hourly.optJSONArray("temperature_2m")
            val code = hourly.optJSONArray("weather_code")
            val mm = hourly.optJSONArray("precipitation")
            for (i in 0 until t.length()) {
                val time = t.getLong(i) * 1000
                if (time + HOUR <= fetchedAt) continue
                if (hours.size >= 12) break
                hours += Hour(time, temp.num(i), wmoCondition(code?.optInt(i) ?: 3), mm.num(i).takeIf { !it.isNaN() } ?: 0.0, false)
            }
        }
        val steps = ArrayList<PrecipStep>()
        o.optJSONObject("minutely_15")?.let { m ->
            val t = m.getJSONArray("time")
            val mm = m.optJSONArray("precipitation")
            // Millimetres per quarter hour, four of them to the hour.
            for (i in 0 until t.length()) steps += PrecipStep(t.getLong(i) * 1000, (mm.num(i).takeIf { !it.isNaN() } ?: 0.0) * 4)
        }
        return Forecast(
            fetchedAt = fetchedAt,
            source = "Open-Meteo",
            tempC = current?.optDouble("temperature_2m", Double.NaN)?.takeIf { !it.isNaN() },
            condition = wmoCondition(current?.optInt("weather_code", 3) ?: 3),
            night = night,
            steps = steps,
            hours = hours,
        )
    }

    fun wmoCondition(code: Int): Condition = when (code) {
        0 -> Condition.CLEAR
        1, 2 -> Condition.PARTLY_CLOUDY
        3 -> Condition.CLOUDY
        45, 48 -> Condition.FOG
        51, 53, 55, 56, 57 -> Condition.DRIZZLE
        61, 63 -> Condition.RAIN
        65 -> Condition.HEAVY_RAIN
        66, 67 -> Condition.SLEET
        71, 73, 75, 77, 85, 86 -> Condition.SNOW
        80, 81, 82 -> Condition.SHOWERS
        95, 96, 99 -> Condition.THUNDER
        else -> Condition.CLOUDY
    }

    private fun JSONArray?.num(i: Int): Double = this?.optDouble(i, Double.NaN) ?: Double.NaN

    // ---- cache ----

    fun write(f: Forecast): String = JSONObject().apply {
        put("fetchedAt", f.fetchedAt)
        put("source", f.source)
        f.tempC?.let { put("tempC", it) }
        put("condition", f.condition.name)
        put("night", f.night)
        put("steps", JSONArray().apply { f.steps.forEach { put(JSONArray().put(it.time).put(it.mmPerHour)) } })
        put("hours", JSONArray().apply {
            f.hours.forEach { put(JSONArray().put(it.time).put(it.tempC).put(it.condition.name).put(it.mm).put(it.night)) }
        })
    }.toString()

    fun read(json: String): Forecast? = runCatching {
        val o = JSONObject(json)
        val steps = o.getJSONArray("steps")
        val hours = o.getJSONArray("hours")
        Forecast(
            fetchedAt = o.getLong("fetchedAt"),
            source = o.optString("source"),
            tempC = o.optDouble("tempC", Double.NaN).takeIf { !it.isNaN() },
            condition = Condition.valueOf(o.getString("condition")),
            night = o.optBoolean("night"),
            steps = (0 until steps.length()).map { steps.getJSONArray(it).let { a -> PrecipStep(a.getLong(0), a.getDouble(1)) } },
            hours = (0 until hours.length()).map {
                hours.getJSONArray(it).let { a -> Hour(a.getLong(0), a.getDouble(1), Condition.valueOf(a.getString(2)), a.getDouble(3), a.getBoolean(4)) }
            },
        )
    }.getOrNull()
}
