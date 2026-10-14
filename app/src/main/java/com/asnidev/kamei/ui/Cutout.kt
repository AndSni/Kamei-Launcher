package com.asnidev.kamei.ui

import android.graphics.Rect
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.asnidev.kamei.data.CutoutMode
import com.asnidev.kamei.data.Prefs

/**
 * Room to leave around the screen. With the status bar showing, the hole sits inside
 * it and the usual safe area applies. With it hidden: "keep clear" leaves the hole's
 * whole band empty; "automatic" and "whole screen" don't, and automatic instead moves
 * just the pieces the hole would cover ([CutoutClear], [cutoutPadFor]).
 */
@Composable
fun screenInsets(prefs: Prefs): WindowInsets =
    if (!prefs.hideStatusBar || prefs.cutoutMode == CutoutMode.KEEP_CLEAR) WindowInsets.safeDrawing
    else WindowInsets.systemBars.union(WindowInsets.ime)

/** Debug builds: a pretend cutout (`--es fakecutout "l,t,r,b"`), for testing layouts the emulator can't show. */
var fakeCutout by mutableStateOf<Rect?>(null)

/** Where the phone's camera holes and notches are, in window pixels (none before Android 9). */
@Composable
fun cutoutRects(): List<Rect> {
    val view = LocalView.current
    // Read so a change in the cutout (rotation, a test cutout) recomposes this.
    val top = WindowInsets.displayCutout.getTop(LocalDensity.current)
    val fake = fakeCutout
    return remember(top, view.rootWindowInsets, fake) {
        if (fake != null) listOf(fake)
        else if (Build.VERSION.SDK_INT < 28) emptyList()
        else view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty().filter { !it.isEmpty }
    }
}

/** Whether automatic moving applies: status bar hidden and the setting on Automatic. */
fun cutoutAuto(prefs: Prefs) = prefs.hideStatusBar && prefs.cutoutMode == CutoutMode.AUTO

/**
 * How far down something whose content would sit at [left]..[right], [top]..[bottom]
 * (window pixels, before any moving) must go to clear every hole it overlaps, plus [gap].
 */
fun cutoutPadFor(rects: List<Rect>, left: Float, top: Float, right: Float, bottom: Float, gap: Float): Int =
    rects.filter { it.left < right && it.right > left && it.top < bottom && it.bottom + gap > top }
        .maxOfOrNull { it.bottom + gap - top }?.coerceAtLeast(0f)?.toInt() ?: 0

/**
 * Shows [content] where it is, or, with automatic cutout handling, just far enough
 * below a camera hole that would cover it. Measures where the content actually is
 * (its own width, not the screen's), so a hole beside it moves nothing.
 */
@Composable
fun CutoutClear(prefs: Prefs, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val rects = cutoutRects()
    val density = LocalDensity.current
    val gap = with(density) { 6.dp.toPx() }
    var pad by remember { mutableIntStateOf(0) }
    val active = cutoutAuto(prefs) && rects.isNotEmpty()
    Column(
        modifier.onGloballyPositioned { c ->
            val b = c.boundsInWindow()
            // The column holds the gap and then the content: without the gap the content
            // would start at the column's top and end [pad] higher.
            val needed = if (active) cutoutPadFor(rects, b.left, b.top, b.right, b.bottom - pad, gap) else 0
            // Never more than one hole's depth (a bottom-anchored column grows upwards instead of moving).
            val capped = needed.coerceAtMost((rects.maxOfOrNull { it.bottom } ?: 0) + gap.toInt())
            if (kotlin.math.abs(capped - pad) > 1) pad = capped
        },
    ) {
        if (pad > 0) Spacer(Modifier.height(with(density) { pad.toDp() }))
        content()
    }
}
