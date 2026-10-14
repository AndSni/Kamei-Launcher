package com.asnidev.kamei

import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.FontChoice
import com.asnidev.kamei.data.Gesture
import com.asnidev.kamei.data.GestureKind
import com.asnidev.kamei.data.HomeBlock
import com.asnidev.kamei.data.IconRef
import com.asnidev.kamei.data.IconStyle
import com.asnidev.kamei.data.Place
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.PrefsJson
import com.asnidev.kamei.data.SettingsStyle
import com.asnidev.kamei.lang.DisplayLang
import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsJsonTest {

    private val a = AppKey("com.example.a", "com.example.a.Main", 0)
    private val b = AppKey("com.example.b", "com.example.b.Main", 10)

    @Test
    fun roundTrips() {
        val p = Prefs(
            favorites = listOf(b, a),
            hidden = setOf(a),
            renames = mapOf(b to "Bee"),
            customIcons = mapOf(a to IconRef.Glyph("cat").encode()),
            iconStyle = IconStyle.LINE,
            displayLang = DisplayLang.KO,
            translateTo = "lv",
            textFont = FontChoice.KLEE,
            weatherOn = true,
            place = Place("Rīga, Latvia", 56.95, 24.11),
            seeded = true,
            settingsStyle = SettingsStyle.PLAIN,
        )
        assertEquals(p, PrefsJson.read(PrefsJson.write(p)))
    }

    @Test
    fun settingsStyleFollowsTheMaker() {
        assertEquals(SettingsStyle.PIXEL, SettingsStyle.AUTO.resolve("Google"))
        assertEquals(SettingsStyle.PIXEL, SettingsStyle.AUTO.resolve("samsung"))
        assertEquals(SettingsStyle.PLAIN, SettingsStyle.AUTO.resolve("Sony"))
        assertEquals(SettingsStyle.PLAIN, SettingsStyle.AUTO.resolve("motorola"))
        assertEquals(SettingsStyle.PIXEL, SettingsStyle.PIXEL.resolve("Sony"))
        assertEquals(SettingsStyle.PLAIN, SettingsStyle.PLAIN.resolve("Google"))
    }

    @Test
    fun garbageGivesDefaults() {
        assertEquals(Prefs(), PrefsJson.read("{nope"))
        assertEquals(Prefs(), PrefsJson.read(null))
        assertEquals(IconStyle.OBSCURE, PrefsJson.read("""{"iconStyle":"WHATEVER"}""").iconStyle)
    }

    @Test
    fun gestures() {
        val p = Prefs(swipeLeft = Gesture(GestureKind.APP, a), swipeRight = Gesture(GestureKind.TORCH))
        val back = PrefsJson.read(PrefsJson.write(p))
        assertEquals(p.swipeLeft, back.swipeLeft)
        assertEquals(p.swipeRight, back.swipeRight)
        assertEquals(Gesture.NONE, Gesture.decode("APP"))
        assertEquals(Gesture.NONE, Gesture.decode("app:broken"))
        assertEquals(Gesture.NONE, Gesture.decode("WHATEVER"))
    }

    @Test
    fun oldClockSizeBecomesBothSizes() {
        val p = PrefsJson.read("""{"clockSize":90}""")
        assertEquals(90, p.hourSize)
        assertEquals(90, p.minuteSize)
        val q = Prefs(hourSize = 150, minuteSize = 70, hourOffsetY = -12, clockStroke = 3.5f)
        assertEquals(q, PrefsJson.read(PrefsJson.write(q)))
    }

    @Test
    fun blockOrder() {
        val custom = listOf(HomeBlock.WEATHER, HomeBlock.CLOCK, HomeBlock.NOTIFICATIONS, HomeBlock.DATE, HomeBlock.WORD, HomeBlock.BATTERY, HomeBlock.SIGNAL)
        assertEquals(custom, PrefsJson.read(PrefsJson.write(Prefs(blockOrder = custom))).blockOrder)
        // An old file with fewer blocks keeps its order and gains the new ones at the end.
        assertEquals(listOf(HomeBlock.DATE, HomeBlock.CLOCK) + HomeBlock.entries.filter { it != HomeBlock.DATE && it != HomeBlock.CLOCK },
            PrefsJson.read("""{"blockOrder":["DATE","CLOCK","NOPE"]}""").blockOrder)
    }

    @Test
    fun prefix() {
        // "None" must stay none, not fall back to the default.
        assertEquals("", PrefsJson.read(PrefsJson.write(Prefs(prefix = ""))).prefix)
        assertEquals("》", PrefsJson.read(PrefsJson.write(Prefs(prefix = "》"))).prefix)
        assertEquals("//", PrefsJson.read("{}").prefix)
        assertEquals("abcd", PrefsJson.read("""{"prefix":"abcdefg"}""").prefix)
    }

    @Test
    fun iconRefs() {
        for (r in listOf(IconRef.Glyph("moon"), IconRef.Pack("com.donnnno.arcticons", "phone"), IconRef.Style(IconStyle.MONO))) {
            assertEquals(r, IconRef.decode(r.encode()))
        }
    }
}
