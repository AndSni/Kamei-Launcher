package com.asnidev.kamei.system

import com.asnidev.kamei.lang.Phrase

/**
 * A warning line on the home screen. [heat] runs from 0 (orange) to 1 (red);
 * [blink] means it has gone critical.
 */
data class Alert(val phrase: Phrase, val n: Int?, val heat: Float, val blink: Boolean)

object Alerts {

    /** From this level down the battery line shows: orange here, red by [BATTERY_RED], blinking at [BATTERY_BLINK]. */
    const val BATTERY_SHOW = 25
    const val BATTERY_RED = 6
    const val BATTERY_BLINK = 5

    /** Nothing while charging: a low battery that's filling up isn't a warning. */
    fun battery(b: Battery?): Alert? {
        if (b == null || b.charging || b.percent > BATTERY_SHOW) return null
        val heat = ((BATTERY_SHOW - b.percent).toFloat() / (BATTERY_SHOW - BATTERY_RED)).coerceIn(0f, 1f)
        return Alert(Phrase.BATTERY, b.percent, heat, b.percent <= BATTERY_BLINK)
    }

    /** One bar: orange; none: red; no service at all: blinking red. Two bars or more: nothing. */
    fun signal(s: Signal): Alert? = when (s) {
        Signal.Quiet -> null
        Signal.NoService -> Alert(Phrase.NO_SERVICE, null, 1f, true)
        is Signal.Bars -> when {
            s.bars >= 2 -> null
            s.bars == 1 -> Alert(Phrase.SIGNAL_WEAK, null, 0f, false)
            else -> Alert(Phrase.SIGNAL_VERY_WEAK, null, 1f, false)
        }
    }
}
