package com.asnidev.kamei

import com.asnidev.kamei.weather.Condition
import com.asnidev.kamei.weather.Forecast
import com.asnidev.kamei.weather.Hour
import com.asnidev.kamei.weather.Outlook
import com.asnidev.kamei.weather.PrecipStep
import com.asnidev.kamei.weather.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {

    private val t0 = 1_800_000_000_000L
    private val min = 60_000L

    private fun forecast(rates: List<Double>, stepMin: Int = 5, temp: Double = 8.0, cond: Condition = Condition.CLOUDY, hours: List<Hour> = emptyList()) =
        Forecast(t0, "test", temp, cond, false, rates.mapIndexed { i, r -> PrecipStep(t0 + i * stepMin * min, r) }, hours)

    @Test
    fun dryStaysDry() {
        assertEquals(Outlook.Dry, Weather.outlook(forecast(List(24) { 0.0 }), t0 + 2 * min))
    }

    @Test
    fun rainAheadCountsMinutes() {
        val o = Weather.outlook(forecast(List(6) { 0.0 } + List(6) { 1.2 }), t0 + 2 * min)
        assertEquals(Outlook.Soon(28, false), o)
    }

    @Test
    fun rainNowSaysWhenItStops() {
        val o = Weather.outlook(forecast(List(4) { 0.8 } + List(8) { 0.0 }), t0 + 1 * min)
        assertEquals(Outlook.Now(19, false), o)
    }

    @Test
    fun coldMeansSnow() {
        val o = Weather.outlook(forecast(List(3) { 0.0 } + List(3) { 0.5 }, temp = -2.0), t0)
        assertTrue(o.snow)
    }

    @Test
    fun staleStepsFallBackToHours() {
        val hours = listOf(Hour(t0 + 3 * 60 * min, 7.0, Condition.RAIN, 1.5, false))
        val o = Weather.outlook(forecast(List(4) { 0.0 }, hours = hours), t0 + 90 * min)
        assertEquals(Outlook.Later(t0 + 3 * 60 * min, false), o)
    }

    @Test
    fun bluenessGrowsAsRainNears() {
        val far = Weather.blueness(Outlook.Soon(100, false), 120)
        val near = Weather.blueness(Outlook.Soon(10, false), 120)
        assertTrue(near > far)
        assertEquals(1f, Weather.blueness(Outlook.Now(null, false), 120))
        assertEquals(0f, Weather.blueness(Outlook.Soon(150, false), 120))
        assertEquals(0f, Weather.blueness(Outlook.Dry, 120))
    }

    @Test
    fun parsesMet() {
        val json = """{"properties":{"timeseries":[
            {"time":"2027-01-15T08:00:00Z","data":{"instant":{"details":{"air_temperature":5.9}},
              "next_1_hours":{"summary":{"symbol_code":"cloudy"},"details":{"precipitation_amount":0.0}}}},
            {"time":"2027-01-15T09:00:00Z","data":{"instant":{"details":{"air_temperature":6.4}},
              "next_1_hours":{"summary":{"symbol_code":"lightrainshowers_day"},"details":{"precipitation_amount":0.4}}}}]}}"""
        val f = Weather.parseMet(json, java.time.Instant.parse("2027-01-15T08:10:00Z").toEpochMilli())
        assertEquals(5.9, f.tempC!!, 0.01)
        assertEquals(Condition.CLOUDY, f.condition)
        assertEquals(Condition.SHOWERS, f.hours[1].condition)
        assertEquals(2, f.steps.size)
    }

    @Test
    fun nowcastWithoutCoverageIsNull() {
        val json = """{"properties":{"meta":{"radar_coverage":"no coverage"},"timeseries":[]}}"""
        assertNull(Weather.parseNowcast(json))
    }

    @Test
    fun openMeteoQuarterHoursBecomeRates() {
        val json = """{"current":{"temperature_2m":6.2,"weather_code":61,"is_day":1},
            "minutely_15":{"time":[1800000000,1800000900],"precipitation":[0.0,0.3]},
            "hourly":{"time":[1800000000],"temperature_2m":[6.2],"weather_code":[61],"precipitation":[0.4]}}"""
        val f = Weather.parseOpenMeteo(json, 1_800_000_000_000L)
        assertEquals(Condition.RAIN, f.condition)
        assertEquals(1.2, f.steps[1].mmPerHour, 0.001)
    }

    @Test
    fun cacheRoundTrips() {
        val f = forecast(listOf(0.0, 1.0), hours = listOf(Hour(t0, 4.0, Condition.SNOW, 0.3, true)))
        assertEquals(f, Weather.read(Weather.write(f)))
    }

    @Test
    fun metSymbols() {
        assertEquals(Condition.THUNDER, Weather.metCondition("heavyrainshowersandthunder_night"))
        assertEquals(Condition.PARTLY_CLOUDY, Weather.metCondition("fair_day"))
        assertEquals(Condition.SLEET, Weather.metCondition("lightsleet"))
        assertEquals(Condition.CLEAR, Weather.metCondition("clearsky_polartwilight"))
    }
}
