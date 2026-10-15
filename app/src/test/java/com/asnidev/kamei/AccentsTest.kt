package com.asnidev.kamei

import com.asnidev.kamei.data.Accents
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentsTest {

    @Test
    fun batteryEnds() {
        assertEquals(0xFF00E676, Accents.battery(100, light = false))
        assertEquals(0xFFFFEA00, Accents.battery(50, light = false))
        assertEquals(0xFFFF1744, Accents.battery(10, light = false))
        assertEquals(0xFFFF1744, Accents.battery(3, light = false))
        assertEquals(0xFF00A844, Accents.battery(100, light = true))
    }

    @Test
    fun batteryBlends() {
        // Halfway between yellow (FFEA00) and green (00E676) at 75%.
        assertEquals(0xFF7FE83B, Accents.battery(75, light = false))
        // 30% is halfway between red (FF1744) and yellow (FFEA00).
        assertEquals(0xFFFF8022, Accents.battery(30, light = false))
    }
}
