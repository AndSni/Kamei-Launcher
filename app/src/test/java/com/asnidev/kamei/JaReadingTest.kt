package com.asnidev.kamei

import com.asnidev.kamei.icons.AppType
import com.asnidev.kamei.data.ReadingHint
import com.asnidev.kamei.lang.JaReading
import com.asnidev.kamei.lang.Phrase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class JaReadingTest {

    @Test
    fun everyPhraseHasAReading() {
        for (p in Phrase.entries) assertTrue(p.name, JaReading.has(p))
    }

    @Test
    fun numbersAndCounters() {
        assertEquals("にじゅうご", JaReading.number(25))
        assertEquals("さんびゃくろくじゅう", JaReading.number(360))
        assertEquals("いっぷん", JaReading.counter(1, "fun"))
        assertEquals("さんぷん", JaReading.counter(3, "fun"))
        assertEquals("じゅっぷん", JaReading.counter(10, "fun"))
        assertEquals("にじゅうごふん", JaReading.counter(25, "fun"))
        assertEquals("いっけん", JaReading.counter(1, "ken"))
        assertEquals("はっけん", JaReading.counter(8, "ken"))
        assertEquals("よじかん", JaReading.counter(4, "jikan"))
        assertEquals("くじ にふん", JaReading.time(9, 2))
        assertEquals("じゅうしちじ", JaReading.time(17, 0))
        assertEquals("れいじ さんじゅっぷん", JaReading.time(0, 30))
    }

    @Test
    fun theSketchReadsRight() {
        assertEquals("すいようび", JaReading.weekday(DayOfWeek.WEDNESDAY))
        assertEquals("じゅうがつ じゅうよっか", JaReading.date(10, 14))
        assertEquals("しがつ ついたち", JaReading.date(4, 1))
        assertEquals("くがつ にじゅうくにち", JaReading.date(9, 29))
        assertEquals("ろくど", JaReading.temp(6))
        assertEquals("れいど", JaReading.temp(0))
        assertEquals("マイナス さんど", JaReading.temp(-3))
        assertEquals("でんぱが よわい", JaReading.say(Phrase.SIGNAL_WEAK))
        assertEquals("じゅうさんじ ごろに あめ", JaReading.say(Phrase.RAIN_AT, t = "13:00"))
        assertEquals("あと にじゅうごふんで あめ", JaReading.say(Phrase.RAIN_IN, 25))
    }

    @Test
    fun readingHints() {
        assertEquals("すいようび", JaReading.hint("すいようび", ReadingHint.KANA))
        assertEquals("suiyōbi", JaReading.hint("すいようび", ReadingHint.ROMAJI))
        assertEquals("すいようび · suiyōbi", JaReading.hint("すいようび", ReadingHint.KANA_ROMAJI))
        assertEquals(null, JaReading.hint("すいようび", ReadingHint.OFF))
    }

    @Test
    fun romaji() {
        assertEquals("suiyōbi", JaReading.romaji("すいようび"))
        assertEquals("jūgatsu jūyokka", JaReading.romaji("じゅうがつ じゅうよっか"))
        assertEquals("ato nijūgofun de ame", JaReading.romaji("あと にじゅうごふん で あめ"))
        assertEquals("messēji", JaReading.romaji("メッセージ"))
        assertEquals("kōshinchū", JaReading.romaji("こうしんちゅう"))
        assertEquals("ippun", JaReading.romaji("いっぷん"))
        assertEquals("tsūchi", JaReading.romaji("つうち"))
        assertEquals("atarashii", JaReading.romaji("あたらしい"))
    }

    @Test
    fun appTypes() {
        assertEquals(AppType.MUSIC, AppType.guess("com.google.android.apps.youtube.music", "YT Music", -1))
        assertEquals(AppType.CAMERA, AppType.guess("com.android.camera2", "Camera", -1))
        assertEquals(AppType.GALLERY, AppType.guess("com.google.android.apps.photos", "Photos", -1))
        assertEquals(AppType.WEATHER, AppType.guess("no.nrk.yr", "Yr", -1))
        assertEquals(AppType.PHONE, AppType.guess("com.google.android.dialer", "Phone", -1))
        assertEquals(AppType.GAMES, AppType.guess("com.example.thing", "Thing", android.content.pm.ApplicationInfo.CATEGORY_GAME))
        assertEquals(AppType.CALENDAR, AppType.guess("com.google.android.calendar", "Calendar", -1))
        assertEquals(AppType.CLOCK, AppType.guess("com.google.android.deskclock", "Clock", -1))
        assertEquals(listOf(AppType.MUSIC), AppType.matching("music"))
        assertTrue(AppType.MONEY in AppType.matching("bank"))
    }
}
