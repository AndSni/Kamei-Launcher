package com.asnidev.kamei

import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.dayFadeAt
import com.asnidev.kamei.data.fade
import com.asnidev.kamei.data.longFade
import com.asnidev.kamei.lang.JlptLevel
import com.asnidev.kamei.lang.Phrases
import com.asnidev.kamei.lang.Vocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class VocabularyTest {

    init {
        // Unit tests run in the app module's folder.
        Vocabulary.load(File("src/main/assets/vocab.tsv").readText())
    }

    @Test
    fun everyLevelIsFull() {
        for (l in JlptLevel.entries) assertEquals(l.name, 365, Vocabulary.from(setOf(l)).size)
        // No word in two levels.
        assertEquals(Vocabulary.all.size, Vocabulary.all.map { it.ja }.toSet().size)
    }

    @Test
    fun everyWordHasAMeaning() {
        for (w in Vocabulary.all) {
            assertFalse(w.ja, w.kana.isBlank())
            assertTrue(w.ja, w.kana.all { it in '\u3040'..'\u30ff' || it == 'ー' })
            // English always; any other language falls back to it.
            for ((code, _) in Phrases.targets) assertFalse("${w.ja} $code", w.word(code).isBlank())
        }
    }

    @Test
    fun eachWordOnceBeforeAnyRepeats() {
        val start = LocalDate.of(2026, 10, 14).toEpochDay()
        for (levels in listOf(setOf(JlptLevel.N5), setOf(JlptLevel.N5, JlptLevel.N4), JlptLevel.entries.toSet())) {
            val n = Vocabulary.from(levels).size
            val seen = (0 until n).map { Vocabulary.of(start + it, levels)!!.ja }.toSet()
            assertEquals(n, seen.size)
            assertTrue(seen.all { ja -> Vocabulary.all.first { it.ja == ja }.level in levels })
        }
        // The hand-checked words keep their translations.
        val rain = Vocabulary.all.first { it.ja == "雨" }
        assertEquals("あめ", rain.kana)
        assertEquals("lietus", rain.word("lv"))
    }

    @Test
    fun bundledFontsCoverTheirLevels() {
        for (w in Vocabulary.all.filter { it.level in Vocabulary.FONT_LEVELS }) assertTrue(w.ja, Vocabulary.inBundledFonts(w.ja))
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
