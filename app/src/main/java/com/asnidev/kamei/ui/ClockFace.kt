package com.asnidev.kamei.ui

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asnidev.kamei.data.ClockLayout
import com.asnidev.kamei.data.Prefs
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** The hour as the clock shows it (12- or 24-hour), always two digits. */
fun clockHour(now: LocalDateTime, h24: Boolean): String = "%02d".format(if (h24) now.hour else (now.hour + 11) % 12 + 1)

/**
 * The home-screen clock: hours and minutes each at their own size, stacked or
 * inline, outlined or solid, each nudged by its own offset. Used by the home
 * screen and by the clock settings' live preview.
 */
@Composable
fun ClockFace(now: LocalDateTime, prefs: Prefs, family: FontFamily, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val clockColor = LocalLook.current.clock
    val ink = rememberDigitInk(family)
    // Half an outline sticks out past the glyph's edge.
    val strokePad = if (prefs.clockOutline) prefs.clockStroke / 2 else 0f
    fun style(size: Int) = TextStyle(
        color = clockColor,
        fontFamily = family,
        fontSize = size.sp,
        drawStyle = if (prefs.clockOutline) Stroke(width = with(density) { prefs.clockStroke.dp.toPx() }) else null,
    )
    val hour = clockHour(now, prefs.clock24)
    val minute = "%02d".format(now.minute)
    val hourMod = Modifier.offset(prefs.hourOffsetX.dp, prefs.hourOffsetY.dp)
    val minuteMod = Modifier.offset(prefs.minuteOffsetX.dp, prefs.minuteOffsetY.dp)
    when (prefs.clockLayout) {
        ClockLayout.STACKED -> Column(modifier, verticalArrangement = Arrangement.spacedBy((minOf(prefs.hourSize, prefs.minuteSize) * 0.12f).dp)) {
            BasicText(hour, style = style(prefs.hourSize), modifier = hourMod.digitBox(prefs.hourSize, ink, strokePad))
            BasicText(minute, style = style(prefs.minuteSize), modifier = minuteMod.digitBox(prefs.minuteSize, ink, strokePad))
        }
        ClockLayout.INLINE -> Row(modifier) {
            BasicText(hour, style = style(prefs.hourSize), modifier = hourMod.alignByBaseline())
            BasicText(":", style = style(minOf(prefs.hourSize, prefs.minuteSize)), modifier = Modifier.alignByBaseline())
            BasicText(minute, style = style(prefs.minuteSize), modifier = minuteMod.alignByBaseline())
        }
    }
}

/** How far the digits' ink reaches above and below the baseline, as shares of the font size. */
class DigitInk(val top: Float, val bottom: Float)

/** Measures [family]'s digits once: faces differ a lot (Bebas is tall, Syne short). */
@Composable
private fun rememberDigitInk(family: FontFamily): DigitInk {
    val resolver = LocalFontFamilyResolver.current
    return remember(family) {
        val typeface = runCatching { resolver.resolve(family).value as? Typeface }.getOrNull()
        val paint = Paint().apply { textSize = 1000f; this.typeface = typeface }
        val bounds = Rect()
        paint.getTextBounds("0123456789", 0, 10, bounds)
        if (bounds.height() <= 0) DigitInk(0.72f, 0f) else DigitInk(-bounds.top / 1000f, bounds.bottom.coerceAtLeast(0) / 1000f)
    }
}

/**
 * Shrinks a line of digits to the digits' own ink (dropping the font's spacing
 * above and below), so stacked lines sit tight whatever their sizes, and nothing
 * pokes out to be clipped. [strokePad] (dp) leaves room for the outline.
 */
private fun Modifier.digitBox(sizeSp: Int, ink: DigitInk, strokePad: Float): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val baseline = p[FirstBaseline].takeIf { it > 0 } ?: p.height
    val px = sizeSp.sp.toPx()
    val pad = strokePad.dp.toPx()
    val above = (px * ink.top + pad).roundToInt().coerceAtMost(baseline)
    val below = (px * ink.bottom + pad).roundToInt()
    layout(p.width, above + below) { p.place(0, above - baseline) }
}
