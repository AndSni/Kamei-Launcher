package com.asnidev.kamei

import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.ui.weatherLine
import com.asnidev.kamei.weather.Condition
import com.asnidev.kamei.weather.Forecast
import com.asnidev.kamei.weather.PrecipStep
import com.asnidev.kamei.weather.WeatherState
import com.asnidev.kamei.weather.WeatherStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WeatherLineTest {

    private val now = LocalDateTime.of(2026, 10, 14, 11, 0)
    private val t0 = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val prefs = Prefs(weatherOn = true, translateTo = "en")

    private fun line(rates: List<Double>, cond: Condition = Condition.CLOUDY) = weatherLine(
        WeatherState(WeatherStatus.OK, Forecast(t0, "test", 10.2, cond, false, rates.mapIndexed { i, r -> PrecipStep(t0 + i * 300_000L, r) }, emptyList())),
        prefs, now,
    )

    @Test
    fun rainComingGoesOnItsOwnLine() {
        val l = line(List(6) { 0.0 } + List(6) { 1.0 })
        assertEquals("10°C  曇り", l.words.display)
        assertEquals("あと30分で雨", l.coming!!.display)
        assertEquals("Rain in 30 min", l.coming!!.translated)
    }

    @Test
    fun rainingNowSaysWhenItStops() {
        val l = line(List(4) { 1.0 } + List(8) { 0.0 })
        assertEquals("10°C  雨", l.words.display)
        assertEquals("あと20分で雨がやみます", l.coming!!.display)
    }

    @Test
    fun dryIsOneLine() {
        val l = line(List(24) { 0.0 }, Condition.CLEAR)
        assertEquals("10°C  晴れ", l.words.display)
        assertNull(l.coming)
    }
}
