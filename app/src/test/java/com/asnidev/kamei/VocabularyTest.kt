package com.asnidev.kamei

import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.dayFadeAt
import com.asnidev.kamei.data.fade
import com.asnidev.kamei.data.longFade
import com.asnidev.kamei.lang.Phrases
import com.asnidev.kamei.lang.Vocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class VocabularyTest {

    @Test
    fun everyWordInEveryLanguage() {
        assertTrue(Vocabulary.all.size >= 60)
        for (w in Vocabulary.all) {
            for ((code, _) in Phrases.targets) assertFalse("${w.key} $code", w.word(code).isBlank())
            for (l in listOf("ja", "zh", "ko", "el")) assertFalse("${w.key} $l reading", w.reading(l).isNullOrBlank())
        }
        assertEquals(Vocabulary.all.size, Vocabulary.all.map { it.key }.toSet().size)
    }

    @Test
    fun eachWordOnceBeforeAnyRepeats() {
        val start = LocalDate.of(2026, 10, 14)
        val n = Vocabulary.all.size
        val seen = (0 until n).map { Vocabulary.of(start.plusDays(it.toLong()).toEpochDay()).key }.toSet()
        assertEquals(n, seen.size)
        assertEquals("雨", Vocabulary.all.first { it.key == "rain" }.word("ja"))
        assertEquals("あめ", Vocabulary.all.first { it.key == "rain" }.reading("ja"))
    }

    @Test
    fun fadesEvenly() {
        val p = Prefs(fadeDays = 30, fadeStart = 100)
        assertEquals(1f, p.longFade(100), 0.001f)
        assertEquals(0.5f, p.longFade(115), 0.001f)
        assertEquals(0f, p.longFade(200), 0.001f)
        assertEquals(1f, Prefs(fadeDays = 0).longFade(9999), 0.001f)
    }

    @Test
    fun fadesEachMorning() {
        val p = Prefs(dayFade = true, dayFadeFrom = 6 * 60, dayFadeTo = 12 * 60)
        assertEquals(1f, p.dayFadeAt(6 * 60), 0.001f)
        assertEquals(0.5f, p.dayFadeAt(9 * 60), 0.001f)
        assertEquals(0f, p.dayFadeAt(12 * 60), 0.001f)
        assertEquals(0f, p.dayFadeAt(18 * 60), 0.001f)
        assertEquals(0f, p.dayFadeAt(5 * 60 + 59), 0.001f)
        // Over midnight.
        val late = Prefs(dayFade = true, dayFadeFrom = 22 * 60, dayFadeTo = 2 * 60)
        assertEquals(0.5f, late.dayFadeAt(0), 0.001f)
        assertEquals(0f, late.dayFadeAt(12 * 60), 0.001f)
        // Both together: half way through a month, half way through the morning.
        assertEquals(0.25f, Prefs(fadeDays = 30, fadeStart = 0, dayFade = true).fade(15, 9 * 60), 0.001f)
        assertEquals(1f, Prefs().fade(15, 20 * 60), 0.001f)
    }
}
