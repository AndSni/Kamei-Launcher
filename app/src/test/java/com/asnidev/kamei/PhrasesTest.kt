package com.asnidev.kamei

import com.asnidev.kamei.lang.DisplayLang
import com.asnidev.kamei.lang.Phrase
import com.asnidev.kamei.lang.Phrases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PhrasesTest {

    @Test
    fun everyLanguageHasEveryPhrase() {
        // say() indexes by ordinal: a missing line would shift every phrase after it.
        for ((code, _) in Phrases.targets) {
            assertTrue(code, Phrases.has(code))
            for (p in Phrase.entries) assertFalse("$code $p", Phrases.say(code, p, 3, "14:00").isBlank())
        }
        for (d in DisplayLang.entries) assertTrue(d.tag, Phrases.has(d.tag))
    }

    @Test
    fun placeholdersSurviveInEveryLanguage() {
        for ((code, _) in Phrases.targets) {
            assertTrue(code, "25" in Phrases.say(code, Phrase.RAIN_IN, 25))
            assertTrue(code, "14:00" in Phrases.say(code, Phrase.RAIN_AT, t = "14:00"))
            assertTrue(code, "7" in Phrases.say(code, Phrase.N_NOTIFICATIONS, 7))
        }
    }

    @Test
    fun matchesTheSketch() {
        val now = LocalDateTime.of(2026, 10, 14, 9, 31)
        assertEquals("水曜日", Phrases.weekday(DisplayLang.JA, now))
        assertEquals("10月14日", Phrases.date(DisplayLang.JA, now))
        assertEquals("新しい通知はありません", Phrases.say("ja", Phrase.NO_NOTIFICATIONS))
        assertEquals("曇り", Phrases.say("ja", Phrase.CLOUDY))
    }

    @Test
    fun unknownTargetFallsBackToEnglish() {
        assertEquals("Cloudy", Phrases.say("xx", Phrase.CLOUDY))
        assertEquals("lv", Phrases.effectiveTarget(DisplayLang.JA, "lv"))
        // Japanese into Japanese is pointless: the phone language (English in tests) instead.
        assertTrue(Phrases.effectiveTarget(DisplayLang.JA, "ja") != "ja")
    }
}
