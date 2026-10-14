package com.asnidev.kamei

import android.graphics.Rect
import com.asnidev.kamei.ui.cutoutPadFor
import org.junit.Assert.assertEquals
import org.junit.Test

class CutoutTest {
    // android.graphics.Rect is a stub in unit tests: fill its public fields by hand.
    private fun rect(l: Int, t: Int, r: Int, b: Int) = Rect().apply { left = l; top = t; right = r; bottom = b }
    private val centreHole = listOf(rect(480, 0, 625, 136))

    @Test
    fun aHoleBesideTheContentMovesNothing() {
        // The clock on the left, the hole in the middle.
        assertEquals(0, cutoutPadFor(centreHole, 74f, 60f, 440f, 600f, 16f))
    }

    @Test
    fun aHoleOnTheContentPushesItJustBelow() {
        // The search bar spans the width and starts at 21 px: down to 136 + 16.
        assertEquals(131, cutoutPadFor(centreHole, 42f, 21f, 1038f, 168f, 16f))
        // A corner hole over the clock.
        assertEquals(96, cutoutPadFor(listOf(rect(0, 0, 200, 120)), 74f, 40f, 440f, 600f, 16f))
    }

    @Test
    fun contentWellBelowTheHoleStays() {
        assertEquals(0, cutoutPadFor(centreHole, 0f, 400f, 1080f, 800f, 16f))
    }
}
