package com.asnidev.kamei

import com.asnidev.kamei.lang.JaReading
import com.asnidev.kamei.lang.Phrase
import com.asnidev.kamei.system.Alerts
import com.asnidev.kamei.system.Battery
import com.asnidev.kamei.system.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsTest {

    @Test
    fun batteryShowsFrom25AndBlinksAt5() {
        assertNull(Alerts.battery(Battery(26, false)))
        assertNull(Alerts.battery(Battery(10, true)))
        val at25 = Alerts.battery(Battery(25, false))!!
        assertEquals(0f, at25.heat, 0.001f)
        assertFalse(at25.blink)
        assertEquals(1f, Alerts.battery(Battery(6, false))!!.heat, 0.001f)
        assertFalse(Alerts.battery(Battery(6, false))!!.blink)
        assertTrue(Alerts.battery(Battery(5, false))!!.blink)
        assertTrue(Alerts.battery(Battery(15, false))!!.heat in 0.4f..0.6f)
    }

    @Test
    fun signal() {
        assertNull(Alerts.signal(Signal.Quiet))
        assertNull(Alerts.signal(Signal.Bars(2)))
        assertEquals(Phrase.SIGNAL_WEAK, Alerts.signal(Signal.Bars(1))!!.phrase)
        assertEquals(1f, Alerts.signal(Signal.Bars(0))!!.heat, 0.001f)
        assertTrue(Alerts.signal(Signal.NoService)!!.blink)
    }

    @Test
    fun readings() {
        assertEquals("バッテリー にじゅうごパーセント", JaReading.say(Phrase.BATTERY, 25))
        assertEquals("バッテリー じゅっパーセント", JaReading.say(Phrase.BATTERY, 10))
        assertEquals("けんがい", JaReading.say(Phrase.NO_SERVICE))
    }
}
