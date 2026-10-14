package com.asnidev.kamei.ui

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A home-screen block: a quick tap runs [onTap]; holding it for [holdMillis]
 * runs [onHold] (the translation). While held, a thin line under the block
 * fills up so the hold is visibly counting. Moving the finger cancels, so
 * swipes started on the block still reach the home screen.
 */
@Composable
fun HoldBlock(holdMillis: Int, onTap: () -> Unit, onHold: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    val haptics = LocalHapticFeedback.current
    val lineColor = LocalLook.current.dim
    Column(
        modifier.pointerInput(holdMillis) {
            awaitEachGesture {
                val down = awaitFirstDown()
                // Taken so the home screen's own long press (settings) doesn't fire here;
                // moves stay free for the swipe gestures.
                down.consume()
                val start = down.uptimeMillis
                val slop = viewConfiguration.touchSlop
                var fired = false
                while (true) {
                    val event = withTimeoutOrNull(16) { awaitPointerEvent() }
                    // Pointer times are uptime too; a still finger sends no events, so the clock is read directly.
                    val elapsed = SystemClock.uptimeMillis() - start
                    if (!fired) progress = ((elapsed - SHOW_AFTER).toFloat() / (holdMillis - SHOW_AFTER)).coerceIn(0f, 1f)
                    if (!fired && elapsed >= holdMillis) {
                        fired = true
                        progress = 0f
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        hold()
                    }
                    if (event == null) continue
                    val change = event.changes.firstOrNull() ?: break
                    if ((change.position - down.position).getDistance() > slop) { progress = 0f; break }
                    if (!change.pressed) {
                        progress = 0f
                        if (!fired && elapsed < TAP_MAX) tap()
                        break
                    }
                }
            }
        },
    ) {
        content()
        Box(
            Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp).drawBehind {
                if (progress > 0f) drawLine(lineColor, Offset(0f, size.height / 2), Offset(size.width * progress, size.height / 2), size.height)
            },
        )
    }
}

private const val SHOW_AFTER = 350L
private const val TAP_MAX = 500L

