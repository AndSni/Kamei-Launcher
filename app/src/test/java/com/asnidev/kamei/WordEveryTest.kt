package com.asnidev.kamei

import com.asnidev.kamei.data.WordEvery
import org.junit.Assert.assertEquals
import org.junit.Test

class WordEveryTest {

    @Test
    fun dailyKeepsTheOldTurns() {
        assertEquals(20_000L, WordEvery.DAY.slot(20_000, 0))
        assertEquals(20_000L, WordEvery.DAY.slot(20_000, 23 * 60 + 59))
    }

    @Test
    fun spansSplitTheDay() {
        // 6 hours: 00-06, 06-12, 12-18, 18-24.
        assertEquals(40_002L, WordEvery.H6.slot(10_000, 12 * 60))
        assertEquals(40_001L, WordEvery.H6.slot(10_000, 11 * 60 + 59))
        assertEquals(24 * 10_000L + 13, WordEvery.H1.slot(10_000, 13 * 60 + 30))
        // Each span follows straight on from the day before's last one.
        assertEquals(WordEvery.H3.slot(9_999, 23 * 60) + 1, WordEvery.H3.slot(10_000, 0))
    }

    @Test
    fun elapsedRunsThroughEachSpan() {
        assertEquals(0f, WordEvery.H3.elapsed(9 * 60), 0.001f)
        assertEquals(0.5f, WordEvery.H3.elapsed(10 * 60 + 30), 0.001f)
        assertEquals(0.5f, WordEvery.DAY.elapsed(12 * 60), 0.001f)
        assertEquals(0.75f, WordEvery.H1.elapsed(14 * 60 + 45), 0.001f)
    }
}
