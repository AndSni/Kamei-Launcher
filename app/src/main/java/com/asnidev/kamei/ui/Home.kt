package com.asnidev.kamei.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.BasicText
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.FontChoice
import com.asnidev.kamei.data.HomeBlock
import com.asnidev.kamei.data.InfoAlign
import com.asnidev.kamei.data.LearnLines
import com.asnidev.kamei.data.ReadingHint
import com.asnidev.kamei.data.NotifStyle
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.fade
import com.asnidev.kamei.data.WeatherGlyphs
import com.asnidev.kamei.lang.DisplayLang
import com.asnidev.kamei.lang.JaReading
import com.asnidev.kamei.lang.Vocabulary
import com.asnidev.kamei.lang.Phrase
import com.asnidev.kamei.lang.Phrases
import com.asnidev.kamei.lang.Words
import com.asnidev.kamei.system.Alerts
import com.asnidev.kamei.system.CustomFonts
import com.asnidev.kamei.system.NotifKind
import com.asnidev.kamei.system.NotifListener
import com.asnidev.kamei.system.Pending
import com.asnidev.kamei.system.SystemActions
import com.asnidev.kamei.weather.Condition
import com.asnidev.kamei.weather.Outlook
import com.asnidev.kamei.weather.Weather
import com.asnidev.kamei.weather.WeatherState
import com.asnidev.kamei.weather.WeatherStatus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun Home(prefs: Prefs) {
    val vm = LocalVm.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val now by produceNow()
    val swipe = with(density) { 56.dp.toPx() }
    // The gesture handler lives across recompositions; it reads the latest settings through this.
    val current = rememberUpdatedState(prefs)
    val look = LocalLook.current

    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (prefs.showWallpaper) {
                    // Src replaces what's under it rather than blending onto it, so a redrawn
                    // area always starts from the dimmed wallpaper, never from the last frame.
                    // Light themes veil the wallpaper with their paper, dark ones with black.
                    val dim = (if (look.light) look.background else Color.Black).copy(alpha = prefs.wallpaperDim / 100f)
                    Modifier.drawBehind { drawRect(dim, blendMode = BlendMode.Src) }
                } else {
                    Modifier.background(look.background)
                },
            )
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { _, d -> dx += d.x; dy += d.y },
                    onDragEnd = {
                        // The longer axis decides: up and down are fixed, the sides are the user's.
                        val p = current.value
                        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                            when {
                                dx < -swipe -> vm.runGesture(p.swipeLeft, context)
                                dx > swipe -> vm.runGesture(p.swipeRight, context)
                            }
                        } else {
                            when {
                                dy < -swipe -> vm.drawerOpen = true
                                dy > swipe -> SystemActions.expandNotifications(context)
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { vm.openSettings() }) }
            .windowInsetsPadding(screenInsets(prefs)),
    ) {
        // Tokyo: a band of the accent colour along the top, like the site's green bar.
        if (look.tokyo) Box(Modifier.fillMaxWidth().height(5.dp).background(look.accent))
        Row(Modifier.fillMaxSize()) {
            // Material's cards reach nearer the edge; the others keep the sketch's margin.
            val start = if (look.cards) 14.dp else 28.dp
            InfoColumn(prefs, now, Modifier.weight(1f).fillMaxHeight().padding(start = start, top = 24.dp, end = 8.dp))
            FavoritesRail(
                prefs,
                Modifier.width((prefs.railIconDp + 40).dp).fillMaxHeight().then(
                    when {
                        // Material: the favourites in a rounded tray.
                        look.cards -> Modifier.padding(top = 24.dp, bottom = 16.dp, end = 10.dp).clip(RoundedCornerShape(28.dp)).background(look.card)
                        // Tokyo: a fine rule between the text and the favourites.
                        look.tokyo -> Modifier.drawBehind { drawLine(look.line, Offset(0f, size.height * 0.04f), Offset(0f, size.height * 0.96f), 1.dp.toPx()) }
                        else -> Modifier
                    },
                ),
            )
        }
    }
}

/** The current time, ticking over on each new minute. */
@Composable
fun produceNow() = androidx.compose.runtime.produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - System.currentTimeMillis() % 60_000L + 50)
    }
}

@Composable
private fun InfoColumn(prefs: Prefs, now: LocalDateTime, modifier: Modifier) {
    val vm = LocalVm.current
    val look = LocalLook.current
    val context = LocalContext.current
    val font = remember(prefs.textFont) { prefs.textFont.family() }
    val face = if (prefs.clockShuffle) FontChoice.daily(now.toLocalDate()) else prefs.clockFont
    val clockFont = remember(face, CustomFonts.clock) { face.family() }
    val lang = prefs.displayLang
    val target = Phrases.effectiveTarget(prefs.displayLang, prefs.translateTo)
    val weather by vm.weather.state.collectAsState()
    val pending by NotifListener.pending.collectAsState()
    val apps by vm.repo.apps.collectAsState()
    val shade = { SystemActions.expandNotifications(context) }
    // Over a wallpaper a soft shadow keeps white text readable on light patches.
    val shadow = if (prefs.showWallpaper && !look.light) Shadow(Color.Black.copy(alpha = 0.7f), blurRadius = 8f) else null
    val big = TextStyle(color = look.text, fontFamily = font, fontSize = 26.sp, lineHeight = 34.sp, shadow = shadow)
    val small = TextStyle(color = look.text, fontFamily = font, fontSize = 15.sp, lineHeight = 22.sp, shadow = shadow)
    // Learning lines: the translation (and maybe the reading) under the text, in the phone's own font.
    // Only Original starts headings with the prefix; Material and Tokyo mark them their own way.
    val pre = if (look.usesPrefix) prefs.prefix else ""
    val learnStyle = TextStyle(color = look.text, fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp, shadow = shadow)
    // Fading: the lines grow fainter day by day until they're gone (the long hold still translates).
    val fade = prefs.fade(now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute)
    val learnSome = prefs.learnLines != LearnLines.OFF && fade > 0.03f
    val learnAll = prefs.learnLines == LearnLines.ALL && fade > 0.03f

    // No scrolling here: a scrollable column would take the swipe up that opens the drawer.
    // Placed top, middle or bottom; its size animates, so lines coming and going slide
    // the rest (the clock included) smoothly instead of making it jump.
    val align = when (prefs.infoAlign) {
        InfoAlign.TOP -> Alignment.TopStart
        InfoAlign.MIDDLE -> Alignment.CenterStart
        InfoAlign.BOTTOM -> Alignment.BottomStart
    }
    Box(modifier, contentAlignment = align) {
        Column(Modifier.smoothHeight(prefs.infoAlign)) {
            // State the blocks below read (collected here: the block builder isn't composable).
            val battery by vm.status.battery.collectAsState()
            val signal by vm.status.signal.collectAsState()
            val alerts = listOfNotNull(
                Alerts.battery(battery).takeIf { prefs.batteryLine }?.let { Triple(HomeBlock.BATTERY, it, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) },
                Alerts.signal(signal).takeIf { prefs.signalLine }?.let { Triple(HomeBlock.SIGNAL, it, Intent(Settings.ACTION_WIRELESS_SETTINGS)) },
            )
            val total = pending.size
            val status = when {
                !vm.notifAccess -> Phrase.ALLOW_NOTIFICATIONS
                total == 0 -> Phrase.NO_NOTIFICATIONS
                else -> Phrase.N_NOTIFICATIONS
            }
            fun keyFor(p: Pending): AppKey? = apps.firstOrNull { it.key.pkg == p.pkg && it.key.user == p.userSerial }?.key
                ?: apps.firstOrNull { it.key.pkg == p.pkg }?.key

            // Each block as a part, drawn below in the user's order.
            val parts = HashMap<HomeBlock, @Composable () -> Unit>()

            // Clock
            parts[HomeBlock.CLOCK] = {
            HoldBlock(prefs.holdMillis, onTap = shade, onHold = {
                val h = if (prefs.clock24) now.hour else (now.hour + 11) % 12 + 1
                vm.translation = Words(
                    clockText(now, prefs.clock24).replace('\n', ':'),
                    "${Phrases.time(target, now)}\n${Phrases.fullDate(target, now)}",
                    if (lang == DisplayLang.JA) JaReading.time(h, now.minute) else null,
                )
            }) {
                CutoutClear(prefs) { ClockFace(now, prefs, clockFont) }
            }
            }

            // Weekday and date
            parts[HomeBlock.DATE] = {
            val dateReading = if (lang == DisplayLang.JA) "${JaReading.weekday(now.dayOfWeek)}\n${JaReading.date(now.monthValue, now.dayOfMonth)}" else null
            HoldBlock(prefs.holdMillis, onTap = shade, onHold = {
                vm.translation = Words("${Phrases.weekday(lang, now)}\n${Phrases.date(lang, now)}", Phrases.fullDate(target, now), dateReading)
            }) {
                Heading(Phrases.weekday(lang, now), pre, big)
                BasicText(spaced(Phrases.date(lang, now), lang), style = big)
                if (learnSome) Learning(dateReading, shortDate(target, now), prefs, learnStyle, look.text, fade)
            }
            }

            // Weather
            parts[HomeBlock.WEATHER] = {
            val line = weatherLine(weather, prefs, now)
            val tint by animateFloatAsState(if (prefs.rainTint) line.blueness else 0f, tween(800), label = "rain")
            val color = look.rainTint(tint)
            HoldBlock(prefs.holdMillis, onTap = {
                when {
                    !prefs.weatherOn || line.setup -> vm.openSettings("weather")
                    else -> vm.weatherApp()?.let { vm.launch(it) } ?: run { vm.forecastOpen = true }
                }
            }, onHold = { vm.translation = line.all }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The mark takes the accent colour until rain tints the whole line.
                    if (pre.isNotEmpty()) BasicText(pre, style = big.copy(color = if (tint > 0.02f) color else look.accent))
                    if (line.condition != null && prefs.weatherGlyphs != WeatherGlyphs.NONE) {
                        WeatherIcon(line.condition, line.night, prefs.weatherGlyphs, 28.dp, color, prefs.stroke, font)
                        Spacer(Modifier.width(8.dp))
                    }
                    BasicText(line.words.display, style = big.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val c = line.coming
                // Flush left like every other line.
                if (c != null) BasicText(c.display, style = big.copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
                // One learning line for the whole block, after both lines, as under the date.
                if (learnSome) {
                    val reading = listOfNotNull(line.words.reading, c?.reading).joinToString("  ").ifBlank { null }
                    Learning(reading, listOfNotNull(line.learn, c?.translated).joinToString(" · "), prefs, learnStyle, color, fade)
                }
            }
            }

            // Battery and signal warnings
            alerts.forEach { (which, alert, settings) -> parts[which] = {
                val w = Phrases.words(lang, target, alert.phrase, alert.n)
                val tint = look.alertTint(alert.heat)
                val shown by produceState(true, alert.blink) {
                    // Critical: the whole line blinks, on and off each half second.
                    value = true
                    while (alert.blink) { delay(500); value = !value }
                }
                HoldBlock(
                    prefs.holdMillis,
                    modifier = Modifier.alpha(if (shown) 1f else 0f),
                    onTap = { SystemActions.open(context, settings) },
                    onHold = { vm.translation = w },
                ) {
                    BasicText(pre + w.display, style = big.copy(color = tint))
                    if (learnSome) Learning(w.reading, w.translated, prefs, learnStyle, tint, fade)
                }
            } }

            // Word of the day
            if (prefs.wordOfDay) parts[HomeBlock.WORD] = {
                var extra by remember(now.toLocalDate()) { mutableIntStateOf(0) }
                val v = Vocabulary.ofDay(now.toLocalDate(), extra)
                val word = v.word(lang.tag)
                val rawReading = v.reading(lang.tag)?.takeIf { it != word }
                val reading = rawReading?.let { r ->
                    if (lang != DisplayLang.JA) r else when (prefs.readingHint) {
                        ReadingHint.KANA -> r
                        ReadingHint.ROMAJI -> JaReading.romaji(r)
                        ReadingHint.OFF -> null
                    }
                }
                val meaning = v.word(target)
                HoldBlock(prefs.holdMillis, onTap = { extra++ }, onHold = {
                    val head = Phrases.words(lang, target, Phrase.WORD_OF_DAY)
                    vm.translation = Words(
                        "${head.display}\n$word",
                        "${head.translated}\n$meaning",
                        if (lang == DisplayLang.JA) "${head.reading}\n${rawReading ?: word}" else rawReading,
                    )
                }) {
                    Row {
                        BasicText(accented(pre, word, look.accent), style = big, modifier = Modifier.alignByBaseline())
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            listOfNotNull(reading, meaning).joinToString(" · "),
                            style = learnStyle.copy(fontSize = 15.sp, color = look.text.copy(alpha = 0.7f)),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }

            // Notifications
            parts[HomeBlock.NOTIFICATIONS] = {
            HoldBlock(prefs.holdMillis, onTap = {
                if (!vm.notifAccess) SystemActions.open(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) else shade()
            }, onHold = {
                val head = Phrases.words(lang, target, Phrase.NOTIFICATIONS)
                val body = Phrases.words(lang, target, status, total)
                vm.translation = Words("${head.display}\n${body.display}", "${head.translated}\n${body.translated}", head.reading?.let { "$it\n${body.reading}" })
            }) {
                Heading(Phrases.say(lang.tag, Phrase.NOTIFICATIONS), pre, big)
                Spacer(Modifier.height(4.dp))
                BasicText(Phrases.say(lang.tag, status, total), style = small.copy(color = if (total > 0) look.text else look.text.copy(alpha = 0.85f)))
                if (learnAll) {
                    val w = Phrases.words(lang, target, status, total)
                    Learning(w.reading, w.translated, prefs, learnStyle, look.text, fade)
                }
                if (total > 0 && prefs.notifStyle == NotifStyle.GLYPHS) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        pending.groupBy { it.pkg to it.userSerial }.values.take(7).forEach { group ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NotifGlyph(keyFor(group.first()), 22.dp, prefs)
                                if (group.size > 1) BasicText("${group.size}", style = small.copy(fontSize = 11.sp, color = look.dim), modifier = Modifier.offset(x = 2.dp, y = 6.dp))
                            }
                        }
                    }
                }
            }
            if (total > 0 && prefs.notifStyle != NotifStyle.GLYPHS) {
                val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                pending.take(MAX_ROWS).forEach { p ->
                    val key = keyFor(p)
                    val kind = kindPhrase(p.kind)
                    val summary = Phrases.say(lang.tag, kind) + " · " + Phrases.ago(lang.tag, p.postTime, nowMs)
                    HoldBlock(prefs.holdMillis, modifier = Modifier.padding(top = 6.dp), onTap = {
                        if (!NotifListener.open(p.key)) shade()
                    }, onHold = {
                        val t = Phrases.resolveTarget(target)
                        val app = key?.let { vm.label(it) } ?: appName(context, p.pkg)
                        val meta = "$app · ${Phrases.say(t, kind)} · ${Phrases.ago(t, p.postTime, nowMs)}"
                        val reading = if (lang == DisplayLang.JA) "${JaReading.say(kind)} · ${agoReading(p.postTime, nowMs)}" else null
                        vm.translation = Words(summary, listOf(meta, p.title, p.text).filter { it.isNotBlank() }.joinToString("\n\n"), reading)
                    }) {
                        Row(verticalAlignment = Alignment.Top) {
                            NotifGlyph(key, 20.dp, prefs, Modifier.padding(top = 1.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                if (prefs.notifStyle == NotifStyle.FULL) {
                                    BasicText(p.title.ifBlank { summary }, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (p.text.isNotBlank()) {
                                        BasicText(p.text, style = small.copy(color = look.dim, fontFamily = FontFamily.Default, fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                } else {
                                    BasicText(summary, style = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (learnAll) {
                                        val t = Phrases.resolveTarget(target)
                                        val reading = if (lang == DisplayLang.JA) "${JaReading.say(kind)} · ${agoReading(p.postTime, nowMs)}" else null
                                        Learning(reading, "${Phrases.say(t, kind)} · ${Phrases.ago(t, p.postTime, nowMs)}", prefs, learnStyle, look.text, fade)
                                    }
                                }
                            }
                        }
                    }
                }
                if (total > MAX_ROWS) BasicText(Phrases.say(lang.tag, Phrase.N_MORE, total - MAX_ROWS), style = small.copy(color = look.dim), modifier = Modifier.padding(start = 32.dp, top = 2.dp))
            }
            }

            // The clock stands alone (its own card in Material); the lines between it form groups.
            val segments = ArrayList<List<HomeBlock>>()
            var run = ArrayList<HomeBlock>()
            for (b in prefs.blockOrder) {
                if (b !in parts) continue
                if (b == HomeBlock.CLOCK) {
                    if (run.isNotEmpty()) segments += run
                    segments += listOf(HomeBlock.CLOCK)
                    run = ArrayList()
                } else run += b
            }
            if (run.isNotEmpty()) segments += run
            segments.forEachIndexed { i, seg ->
                if (seg == listOf(HomeBlock.CLOCK)) {
                    ClockSection { parts.getValue(HomeBlock.CLOCK)() }
                } else {
                    Sections { seg.forEach { b -> block(parts.getValue(b)) } }
                    if (i < segments.lastIndex) Spacer(Modifier.height(if (look.cards) 12.dp else 24.dp))
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

private const val MAX_ROWS = 5

/** An app icon: plain, or (Material) as in the Phone app: one-colour icons in a tonal circle, colour icons filling it. */
@Composable
fun RailIcon(key: AppKey, size: Dp) {
    val look = LocalLook.current
    if (look.cards) AppIcon(key, size + 10.dp, tonal = Tonal(look.iconCircle, look.onIconCircle)) else AppIcon(key, size)
}

/** "//" in the phone's accent colour, then [text] as it is: Original follows the colour scheme through its marks. */
fun accented(prefix: String, text: String, accent: Color) = buildAnnotatedString {
    withStyle(SpanStyle(color = accent)) { append(prefix) }
    append(text)
}

/** Collects the home blocks below the clock, so each theme can lay them out its own way. */
class SectionScope {
    val blocks = mutableListOf<@Composable () -> Unit>()
    fun block(content: @Composable () -> Unit) { blocks += content }
}

/** The clock's place: bare (Original, Tokyo) or on a card of its own (Material). */
@Composable
private fun ClockSection(content: @Composable () -> Unit) {
    val look = LocalLook.current
    if (look.cards) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(look.card).padding(horizontal = 22.dp, vertical = 20.dp)) { content() }
        Spacer(Modifier.height(12.dp))
    } else {
        content()
        Spacer(Modifier.height(if (look.tokyo) 20.dp else 28.dp))
    }
}

/**
 * The blocks under the clock. Original: open space between them. Material: one
 * segmented group of rounded cards (large corners at the group's ends, small gaps),
 * like the phone's own apps. Tokyo: fine rules between them.
 */
@Composable
private fun Sections(build: SectionScope.() -> Unit) {
    val look = LocalLook.current
    val blocks = SectionScope().apply(build).blocks
    when {
        look.cards -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            blocks.forEachIndexed { i, b ->
                val top = if (i == 0) 24.dp else 6.dp
                val bottom = if (i == blocks.lastIndex) 24.dp else 6.dp
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(top, top, bottom, bottom)).background(look.card)
                        .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                ) { Column { b() } }
            }
        }
        look.tokyo -> Column {
            blocks.forEachIndexed { i, b ->
                if (i > 0) Box(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(look.line))
                Column { b() }
            }
        }
        else -> Column {
            blocks.forEachIndexed { i, b ->
                if (i > 0) Spacer(Modifier.height(24.dp))
                Column { b() }
            }
        }
    }
}

/**
 * A block's heading: "//水曜日" in Original, an accent-coloured title in Material, a
 * rounded outline tag in Tokyo (like the filter chips on Japanese sites).
 */
@Composable
private fun Heading(text: String, prefix: String, style: TextStyle) {
    val look = LocalLook.current
    when {
        look.tokyo -> {
            Box(
                Modifier.padding(bottom = 6.dp).border(1.dp, look.accent, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                BasicText(text, style = style.copy(fontSize = 15.sp, lineHeight = 22.sp, color = look.accent))
            }
        }
        look.cards -> BasicText(text, style = style.copy(color = look.accent))
        else -> BasicText(accented(prefix, text, look.accent), style = style)
    }
}

/**
 * Like animateContentSize, but without its clipping: the column's height glides to
 * its new size while the content stays anchored to [align]'s edge, so a nudged
 * clock or a tall font is never cut off.
 */
@Composable
private fun Modifier.smoothHeight(align: InfoAlign): Modifier {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val height = remember { Animatable(-1f) }
    return layout { measurable, constraints ->
        val p = measurable.measure(constraints)
        val target = p.height.toFloat()
        when {
            height.value < 0f -> scope.launch { height.snapTo(target) }
            height.targetValue != target -> scope.launch { height.animateTo(target, tween(320)) }
        }
        val current = (if (height.value < 0f) target else height.value).roundToInt()
        layout(p.width, current) {
            val y = when (align) {
                InfoAlign.TOP -> 0
                InfoAlign.MIDDLE -> (current - p.height) / 2
                InfoAlign.BOTTOM -> current - p.height
            }
            p.place(0, y)
        }
    }
}

/**
 * The learning lines under a home-screen block: the reading (Japanese, if wanted)
 * and the translation, small and dimmed so the original still leads.
 */
@Composable
private fun Learning(reading: String?, translated: String, prefs: Prefs, style: TextStyle, color: Color, fade: Float) {
    val r = reading?.takeIf { prefs.learnReading && it.isNotBlank() }?.let { text ->
        val oneLine = text.lines().joinToString("  ")
        when (prefs.readingHint) {
            ReadingHint.KANA -> oneLine
            ReadingHint.ROMAJI -> JaReading.romaji(oneLine)
            ReadingHint.OFF -> null
        }
    }
    Spacer(Modifier.height(2.dp))
    if (r != null) BasicText(r, style = style.copy(color = color.copy(alpha = 0.75f * fade)))
    BasicText(translated.lines().joinToString("  "), style = style.copy(color = color.copy(alpha = 0.6f * fade)))
}

/** "Wednesday, 14 October" in the order and words of [target]'s language, without the year. */
private fun shortDate(target: String, now: LocalDateTime): String {
    val locale = java.util.Locale.forLanguageTag(Phrases.resolveTarget(target))
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return DateTimeFormatter.ofPattern(pattern, locale).format(now)
}

private fun agoReading(then: Long, now: Long): String {
    val min = ((now - then) / 60_000).toInt()
    return when {
        min < 1 -> JaReading.say(Phrase.JUST_NOW)
        min < 60 -> JaReading.say(Phrase.MIN_AGO, min)
        else -> JaReading.say(Phrase.HOURS_AGO, min / 60)
    }
}

/** An app's glyph for a notification; Android's own (System UI and the like) get a cog. */
@Composable
private fun NotifGlyph(key: AppKey?, size: Dp, prefs: Prefs, modifier: Modifier = Modifier) {
    if (key != null) AppIcon(key, size, modifier) else GlyphIcon("settings", size, modifier, stroke = prefs.stroke)
}

private fun appName(context: android.content.Context, pkg: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
}.getOrDefault(pkg)

private fun kindPhrase(k: NotifKind): Phrase = when (k) {
    NotifKind.MESSAGE -> Phrase.KIND_MESSAGE
    NotifKind.EMAIL -> Phrase.KIND_EMAIL
    NotifKind.CALL -> Phrase.KIND_CALL
    NotifKind.EVENT -> Phrase.KIND_EVENT
    NotifKind.REMINDER -> Phrase.KIND_REMINDER
    NotifKind.ALARM -> Phrase.KIND_ALARM
    NotifKind.SOCIAL -> Phrase.KIND_SOCIAL
    NotifKind.PROMO -> Phrase.KIND_PROMO
    NotifKind.SYSTEM -> Phrase.KIND_SYSTEM
    NotifKind.OTHER -> Phrase.KIND_OTHER
}

/** CJK dates in the sketch sit on a wide grid ("10 月 14 日"); other scripts stay as written. */
private fun spaced(date: String, lang: DisplayLang): String =
    if (lang == DisplayLang.JA || lang == DisplayLang.ZH) date.replace(Regex("(\\d)(\\D)"), "$1\u2009$2").replace(Regex("(\\D)(\\d)"), "$1\u2009$2") else date

private fun clockText(now: LocalDateTime, h24: Boolean): String = "${clockHour(now, h24)}\n%02d".format(now.minute)


/**
 * The weather line: what it says (in both languages), its icon and how blue it is.
 * [learn] is the translation for the learning line, without the temperature it repeats.
 */
class WeatherLine(
    val words: Words,
    val condition: Condition?,
    val night: Boolean,
    val blueness: Float,
    val setup: Boolean,
    val learn: String = words.translated,
    /** What's coming (rain in 25 min, rain stops in 10 min…), on a line of its own so neither line wraps. */
    val coming: Words? = null,
) {
    /** Both lines together, for the long-hold popup. */
    val all: Words
        get() = coming?.let { c ->
            Words(
                "${words.display}\n${c.display}",
                "${words.translated}\n${c.translated}",
                listOfNotNull(words.reading, c.reading).joinToString("\n").ifBlank { null },
            )
        } ?: words
}

fun weatherLine(state: WeatherState, prefs: Prefs, now: LocalDateTime): WeatherLine {
    val lang = prefs.displayLang
    val target = Phrases.effectiveTarget(prefs.displayLang, prefs.translateTo)
    fun plain(p: Phrase, setup: Boolean = false) = WeatherLine(Phrases.words(lang, target, p), null, false, 0f, setup)
    if (!prefs.weatherOn || state.status == WeatherStatus.OFF) return plain(Phrase.SET_UP_WEATHER, setup = true)
    val f = state.forecast ?: return when (state.status) {
        WeatherStatus.NO_LOCATION -> plain(Phrase.NO_LOCATION, setup = true)
        WeatherStatus.ERROR -> plain(Phrase.NO_DATA)
        else -> plain(Phrase.UPDATING)
    }
    val ms = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val outlook = Weather.outlook(f, ms)
    val degrees = f.tempC?.let { c -> if (prefs.fahrenheit) (c * 9 / 5 + 32).roundToInt() else c.roundToInt() }
    val temp = degrees?.let { if (prefs.fahrenheit) "$it°F" else "$it°C" } ?: "–"
    val hhmm = DateTimeFormatter.ofPattern("HH:mm")
    fun at(t: Long) = hhmm.format(Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()))
    // Line one is now: the temperature and what's falling or not. Line two, if any, is what's coming.
    val raining = outlook is Outlook.Now
    val nowPhrase = when {
        raining && outlook.snow -> Phrase.SNOW
        raining -> if (f.condition in WET_CONDITIONS && f.condition != Condition.SNOW && f.condition != Condition.SLEET) conditionPhrase(f.condition) else Phrase.RAIN
        else -> conditionPhrase(f.condition)
    }
    val coming = when (outlook) {
        is Outlook.Now -> outlook.stopsInMin?.takeIf { it <= 120 }?.let {
            Phrases.words(lang, target, if (outlook.snow) Phrase.SNOW_STOPS_IN else Phrase.RAIN_STOPS_IN, it)
        }
        // Past two hours "in 190 min" reads worse than a time of day.
        is Outlook.Soon -> if (outlook.inMin <= 120) Phrases.words(lang, target, if (outlook.snow) Phrase.SNOW_IN else Phrase.RAIN_IN, outlook.inMin)
            else Phrases.words(lang, target, Phrase.RAIN_AT, t = at(ms + outlook.inMin * 60_000L))
        is Outlook.Later -> Phrases.words(lang, target, Phrase.RAIN_AT, t = at(outlook.at))
        Outlook.Dry -> null
    }
    val w = Phrases.words(lang, target, nowPhrase)
    val icon = if (raining) {
        if (outlook.snow) Condition.SNOW else if (f.condition in WET_CONDITIONS) f.condition else Condition.RAIN
    } else f.condition
    val reading = w.reading?.let { r -> degrees?.let { "${JaReading.temp(it)}  $r" } ?: r }
    return WeatherLine(
        Words("$temp  ${w.display}", "$temp  ${w.translated}", reading),
        icon, f.night, Weather.blueness(outlook, prefs.rainWindow), false, w.translated, coming,
    )
}

private val WET_CONDITIONS = setOf(Condition.DRIZZLE, Condition.RAIN, Condition.HEAVY_RAIN, Condition.SHOWERS, Condition.SLEET, Condition.SNOW, Condition.THUNDER)

fun conditionPhrase(c: Condition): Phrase = when (c) {
    Condition.CLEAR -> Phrase.CLEAR
    Condition.PARTLY_CLOUDY -> Phrase.PARTLY_CLOUDY
    Condition.CLOUDY -> Phrase.CLOUDY
    Condition.FOG -> Phrase.FOG
    Condition.DRIZZLE -> Phrase.DRIZZLE
    Condition.RAIN -> Phrase.RAIN
    Condition.HEAVY_RAIN -> Phrase.HEAVY_RAIN
    Condition.SHOWERS -> Phrase.SHOWERS
    Condition.SLEET -> Phrase.SLEET
    Condition.SNOW -> Phrase.SNOW
    Condition.THUNDER -> Phrase.THUNDER
}

/** Weather in the launcher's own line style, or as a single kanji in a thin ring. */
@Composable
fun WeatherIcon(c: Condition, night: Boolean, style: WeatherGlyphs, size: Dp, tint: Color, stroke: Float, kanjiFont: FontFamily = FontFamily.Default) {
    when (style) {
        WeatherGlyphs.NONE -> Unit
        WeatherGlyphs.KANJI -> Box(Modifier.size(size).border(1.dp, tint, CircleShape), contentAlignment = Alignment.Center) {
            BasicText(
                KANJI.getValue(c),
                style = TextStyle(color = tint, fontSize = (size.value * 0.5f).sp, fontFamily = kanjiFont),
            )
        }
        WeatherGlyphs.LINE -> if (c == Condition.PARTLY_CLOUDY) {
            Box(Modifier.size(size)) {
                GlyphIcon(if (night) "moon" else "sun", size * 0.62f, Modifier.align(Alignment.TopStart), tint, stroke * 1.3f)
                GlyphIcon("cloud", size * 0.78f, Modifier.align(Alignment.BottomEnd), tint, stroke)
            }
        } else {
            GlyphIcon(lineGlyph(c, night), size, tint = tint, stroke = stroke)
        }
    }
}

private fun lineGlyph(c: Condition, night: Boolean) = when (c) {
    Condition.CLEAR -> if (night) "moon-stars" else "sun"
    Condition.PARTLY_CLOUDY, Condition.CLOUDY -> "cloud"
    Condition.FOG -> "mist"
    Condition.DRIZZLE, Condition.RAIN, Condition.SHOWERS -> "cloud-rain"
    Condition.HEAVY_RAIN -> "umbrella"
    Condition.SLEET -> "cloud-snow"
    Condition.SNOW -> "snowflake"
    Condition.THUNDER -> "cloud-storm"
}

private val KANJI = mapOf(
    Condition.CLEAR to "晴", Condition.PARTLY_CLOUDY to "晴", Condition.CLOUDY to "曇", Condition.FOG to "霧",
    Condition.DRIZZLE to "雨", Condition.RAIN to "雨", Condition.HEAVY_RAIN to "雨", Condition.SHOWERS to "雨",
    Condition.SLEET to "霙", Condition.SNOW to "雪", Condition.THUNDER to "雷",
)

/**
 * The favourites stack: spread down the full height like the sketch, scrolling
 * once there are too many. Tap opens; hold and drag to reorder; hold and let go
 * for the app's options.
 */
@Composable
private fun FavoritesRail(prefs: Prefs, modifier: Modifier) {
    val favorites = prefs.favorites
    val iconSize = prefs.railIconDp.dp
    val vm = LocalVm.current
    val haptics = LocalHapticFeedback.current
    // Automatic cutout handling: the stack starts below a hole that would sit on its top icon.
    val rects = cutoutRects()
    val density = LocalDensity.current
    var railPad by remember { mutableIntStateOf(0) }
    BoxWithConstraints(
        modifier
            .onGloballyPositioned { c ->
                val b = c.boundsInWindow()
                val gap = with(density) { 6.dp.toPx() }
                val slot = with(density) { (iconSize + 16.dp).toPx() }
                // Measured outside the padding, so b.top is where the top icon's slot would start.
                val needed = if (cutoutAuto(prefs)) cutoutPadFor(rects, b.left, b.top, b.right, b.top + slot, gap) else 0
                if (kotlin.math.abs(needed - railPad) > 1) railPad = needed
            }
            .padding(top = with(density) { railPad.toDp() }),
    ) {
        val count = favorites.size.coerceAtLeast(1)
        val slot = (maxHeight / count).coerceIn(iconSize + 16.dp, iconSize * 2.4f)
        val slotPx by rememberUpdatedState(with(LocalDensity.current) { slot.toPx() })
        // The pointer handlers outlive recompositions, so they read the stored
        // favourites through this, never a copy taken when they were set up.
        val stored by rememberUpdatedState(favorites)
        // The order while a drag is going on (and until the store catches up after it).
        var dragOrder by remember { mutableStateOf<List<AppKey>?>(null) }
        var dragging by remember { mutableStateOf<AppKey?>(null) }
        var offset by remember { mutableFloatStateOf(0f) }
        var moved by remember { mutableStateOf(false) }
        var held by remember { mutableStateOf(false) }
        LaunchedEffect(favorites) { if (dragging == null) dragOrder = null }
        val order = dragOrder ?: favorites
        val fits = slot * favorites.size <= maxHeight
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        // A lazy list keeps its first visible item in place; when that item is the one
        // being swapped, hold the scroll where it was instead of following it.
        fun swap(list: List<AppKey>, i: Int, j: Int): List<AppKey> {
            val first = listState.firstVisibleItemIndex
            if (i == first || j == first) {
                val off = listState.firstVisibleItemScrollOffset
                scope.launch { listState.scrollToItem(first, off) }
            }
            return list.toMutableList().apply { add(j, removeAt(i)) }
        }
        LazyColumn(
            Modifier.fillMaxWidth().fillMaxHeight(),
            state = listState,
            verticalArrangement = if (fits) Arrangement.Center else Arrangement.Top,
            userScrollEnabled = !fits && dragging == null,
        ) {
            items(order, key = { it.encode() }) { k ->
                val isDragged = dragging == k
                Box(
                    Modifier
                        .then(if (isDragged) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                        .fillMaxWidth()
                        .height(slot)
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) offset else 0f
                            val s = if (isDragged) 1.18f else 1f
                            scaleX = s; scaleY = s
                        }
                        .pointerInput(k) {
                            detectTapGestures(
                                onPress = { held = false },
                                onTap = { if (!held) vm.launch(k) },
                            )
                        }
                        .pointerInput(k) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    held = true
                                    dragging = k
                                    dragOrder = stored
                                    offset = 0f
                                    moved = false
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    val list = dragOrder ?: return@detectDragGesturesAfterLongPress
                                    offset += amount.y
                                    if (kotlin.math.abs(offset) > 8f) moved = true
                                    val i = list.indexOf(k)
                                    if (offset > slotPx / 2 && i in 0 until list.lastIndex) {
                                        dragOrder = swap(list, i, i + 1)
                                        offset -= slotPx
                                    } else if (offset < -slotPx / 2 && i > 0) {
                                        dragOrder = swap(list, i, i - 1)
                                        offset += slotPx
                                    }
                                },
                                onDragEnd = {
                                    val list = dragOrder
                                    dragging = null
                                    offset = 0f
                                    if (moved && list != null && list != stored) vm.setFavorites(list)
                                    else {
                                        dragOrder = null
                                        if (!moved) vm.sheetFor = k
                                    }
                                },
                                onDragCancel = {
                                    dragging = null
                                    offset = 0f
                                    dragOrder = null
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    RailIcon(k, iconSize)
                }
            }
        }
    }
}
