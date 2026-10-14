package com.asnidev.kamei.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asnidev.kamei.BuildConfig
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.ClockLayout
import com.asnidev.kamei.data.CutoutMode
import com.asnidev.kamei.data.DEFAULT_PREFIX
import com.asnidev.kamei.data.DrawerLayout
import com.asnidev.kamei.data.FontChoice
import com.asnidev.kamei.data.HomeBlock
import com.asnidev.kamei.data.HomeTheme
import com.asnidev.kamei.data.Gesture
import com.asnidev.kamei.data.GestureKind
import com.asnidev.kamei.data.GlyphSet
import com.asnidev.kamei.data.IconStyle
import com.asnidev.kamei.data.InfoAlign
import com.asnidev.kamei.data.LearnLines
import com.asnidev.kamei.data.LocationMode
import com.asnidev.kamei.data.NotifStyle
import com.asnidev.kamei.data.Place
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.ReadingHint
import com.asnidev.kamei.data.SettingsStyle
import com.asnidev.kamei.data.WeatherGlyphs
import com.asnidev.kamei.data.WeatherSource
import com.asnidev.kamei.data.dayFadeAt
import com.asnidev.kamei.data.longFade
import com.asnidev.kamei.lang.DisplayLang
import com.asnidev.kamei.lang.Phrase
import com.asnidev.kamei.lang.Phrases
import com.asnidev.kamei.lang.Transcribe
import com.asnidev.kamei.system.CustomFonts
import com.asnidev.kamei.system.SystemActions
import com.asnidev.kamei.weather.WeatherStatus
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(prefs: Prefs) {
    val vm = LocalVm.current
    val page = vm.settings.lastOrNull() ?: return
    val back: () -> Unit = { vm.settings.removeAt(vm.settings.lastIndex) }
    val title = TITLES[page] ?: "Settings"
    CompositionLocalProvider(LocalSettingsStyle provides prefs.settingsStyle.resolve()) { Page(page, title, prefs, back) }
}

@Composable
private fun Page(page: String, title: String, prefs: Prefs, back: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable { back() }, contentAlignment = Alignment.Center) {
                GlyphIcon("arrow-left", 24.dp, tint = MaterialTheme.colorScheme.onSurface, stroke = 1.75f)
            }
        }
        if (page == "place") {
            PlacePage()
            return@Column
        }
        // The clock page keeps its preview still above its own scrolling list.
        if (page == "clock") {
            ClockPage(prefs)
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp))
            when (page) {
                "main" -> MainPage(prefs)
                "language" -> LanguagePage(prefs)
                "fonts" -> FontsPage(prefs)
                "icons" -> IconsPage(prefs)
                "drawer" -> DrawerPage(prefs)
                "hidden" -> HiddenPage(prefs)
                "weather" -> WeatherPage(prefs)
                "notifications" -> NotificationsPage(prefs)
                "privacy" -> PrivacyPage(prefs)
                "wallpaper" -> WallpaperPage(prefs)
                "gestures" -> GesturesPage(prefs)
                "status" -> StatusPage(prefs)
                "order" -> OrderPage(prefs)
                "about" -> AboutPage()
            }
        }
    }
}

private val TITLES = mapOf(
    "main" to "Settings", "language" to "Language", "fonts" to "Fonts & layout", "clock" to "Clock", "icons" to "Icons",
    "drawer" to "Favourites & drawer", "hidden" to "Hidden apps", "weather" to "Weather",
    "notifications" to "Notifications", "privacy" to "Privacy", "wallpaper" to "Wallpaper", "gestures" to "Gestures", "status" to "Battery & signal", "order" to "Order of lines", "about" to "About", "place" to "Place",
)

// ---------- building blocks ----------

/** Rows in one rounded card, split by thin gaps, like Android's own Settings. */
@Composable
private fun Group(label: String? = null, content: GroupScope.() -> Unit) {
    val rows = GroupScope().apply(content).rows
    val plain = LocalSettingsStyle.current == SettingsStyle.PLAIN
    if (label != null) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = if (plain) 24.dp else 40.dp, bottom = 8.dp, top = 4.dp))
    }
    if (plain) {
        Column(Modifier.padding(horizontal = 4.dp)) { rows.forEach { it() } }
        HorizontalDivider(Modifier.padding(top = 8.dp, bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        return
    }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { i, row ->
            val top = if (i == 0) 24.dp else 6.dp
            val bottom = if (i == rows.lastIndex) 24.dp else 6.dp
            val shape: Shape = RoundedCornerShape(top, top, bottom, bottom)
            Box(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer)) { row() }
        }
    }
    Spacer(Modifier.height(20.dp))
}

private class GroupScope {
    val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { rows += content }
}

/** Pixel look (cards, coloured circles) or plain look (flat list, accent icons); see [SettingsStyle]. */
private val LocalSettingsStyle = staticCompositionLocalOf { SettingsStyle.PIXEL }

private val CIRCLES = listOf(Color(0xFF7FCFFF), Color(0xFFB4E89C), Color(0xFFFFB68A), Color(0xFFD7BAFF), Color(0xFFFFD86B), Color(0xFF8EE3D3), Color(0xFFFF9FB3), Color(0xFFA9C2FF))

@Composable
private fun Item(
    title: String,
    summary: String? = null,
    glyph: String? = null,
    circle: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            if (circle != null && LocalSettingsStyle.current == SettingsStyle.PLAIN) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    GlyphIcon(glyph, 24.dp, tint = MaterialTheme.colorScheme.primary, stroke = 1.75f)
                }
            } else if (circle != null) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
                    GlyphIcon(glyph, 22.dp, tint = Color(0xFF1B1B1F), stroke = 1.9f)
                }
            } else {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    GlyphIcon(glyph, 24.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, stroke = 1.75f)
                }
            }
            Spacer(Modifier.width(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SwitchItem(title: String, summary: String?, checked: Boolean, glyph: String? = null, onChange: (Boolean) -> Unit) =
    Item(title, summary, glyph, trailing = { Switch(checked, onChange) }, onClick = { onChange(!checked) })

/** A row that opens a single-choice dialog, like a ListPreference. */
@Composable
private fun <T> ChoiceItem(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    glyph: String? = null,
    circle: Color? = null,
    describe: ((T) -> String?)? = null,
    preview: (@Composable (T) -> Unit)? = null,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Item(title, label(selected), glyph, circle, onClick = { open = true })
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(options) { o ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .selectable(o == selected) { onSelect(o); open = false }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(o == selected, onClick = null)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(label(o), style = MaterialTheme.typography.bodyLarge)
                                describe?.invoke(o)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                preview?.invoke(o)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ open = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SliderItem(title: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, format: (Float) -> String, onDone: (Float) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
            Text(format(v), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(v, { v = it }, valueRange = range, steps = steps, onValueChangeFinished = { onDone(v) })
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 36.dp, vertical = 4.dp))
    Spacer(Modifier.height(12.dp))
}

// ---------- pages ----------

@Composable
private fun MainPage(prefs: Prefs) {
    val vm = LocalVm.current
    val context = LocalContext.current
    val target = Phrases.targets.firstOrNull { it.first == Phrases.resolveTarget(prefs.translateTo) }?.second ?: "English"
    Group {
        row {
            ChoiceItem("Theme", HomeTheme.entries, prefs.theme, { it.label }, "palette", CIRCLES[7], describe = { it.summary }) { t -> vm.update { it.copy(theme = t) } }
        }
        row {
            Item(
                "Colours",
                if (android.os.Build.VERSION.SDK_INT >= 31) "From your wallpaper (Material You). Change them in Wallpaper & style" else "Android 12 and later take them from the wallpaper",
                "color-swatch",
                CIRCLES[2],
            ) { SystemActions.openWallpaperAndStyle(context) }
        }
        row {
            ChoiceItem("Settings look", SettingsStyle.entries, prefs.settingsStyle,
                { if (it == SettingsStyle.AUTO) "Automatic · ${it.resolve().label} on this phone" else it.label },
                "layout-list", CIRCLES[1], describe = { it.summary }) { st -> vm.update { it.copy(settingsStyle = st) } }
        }
        row { Item("Language", "${prefs.displayLang.label} · translated into $target", "language", CIRCLES[0]) { vm.settings += "language" } }
        row { Item("Wallpaper", if (prefs.showWallpaper) "Shown, ${prefs.wallpaperDim}% darker" else "Black", "photo", CIRCLES[6]) { vm.settings += "wallpaper" } }
        row { Item("Clock", "${if (prefs.clockShuffle) "A new font daily" else prefs.clockFont.label} · ${prefs.clockLayout.label.lowercase()}", "clock", CIRCLES[1]) { vm.settings += "clock" } }
        row { Item("Fonts & layout", "${prefs.textFont.label} · ${prefs.infoAlign.label.lowercase()}", "typography", CIRCLES[5]) { vm.settings += "fonts" } }
        row { Item("Order of lines", prefs.blockOrder.joinToString(" · ") { it.label.substringBefore(' ') }, "arrows-sort", CIRCLES[4]) { vm.settings += "order" } }
        row { Item("Icons", prefs.iconStyle.label, "icons", CIRCLES[2]) { vm.settings += "icons" } }
        row { Item("Favourites & drawer", "${prefs.favorites.size} favourites · ${prefs.drawerLayout.label.lowercase()}", "layout-grid", CIRCLES[3]) { vm.settings += "drawer" } }
    }
    Group {
        row { Item("Gestures", "Left: ${gestureLabel(vm, prefs.swipeLeft)} · Right: ${gestureLabel(vm, prefs.swipeRight)}", "hand-move", CIRCLES[2]) { vm.settings += "gestures" } }
        row { Item("Weather", if (prefs.weatherOn) prefs.weatherSource.label else "Off", "cloud", CIRCLES[4]) { vm.settings += "weather" } }
        row {
            val on = listOfNotNull("battery".takeIf { prefs.batteryLine }, "signal".takeIf { prefs.signalLine })
            Item("Battery & signal", if (on.isEmpty()) "Off" else "Warns about " + on.joinToString(" and "), "battery-2", CIRCLES[3]) { vm.settings += "status" }
        }
        row { Item("Notifications", if (vm.notifAccess) prefs.notifStyle.label else "No access", "bell", CIRCLES[5]) { vm.settings += "notifications" } }
        row { Item("Privacy", "No accounts, no tracking", "shield-lock", CIRCLES[6]) { vm.settings += "privacy" } }
    }
    Group {
        val isHome = SystemActions.isDefaultLauncher(context)
        row {
            Item("Home app", if (isHome) "Kamei is your home app" else "Make Kamei your home app", "home", CIRCLES[7]) {
                (context as? android.app.Activity)?.let { SystemActions.requestDefaultLauncher(it) }
            }
        }
        row { Item("About", "Version ${BuildConfig.VERSION_NAME}", "info-circle", CIRCLES[0]) { vm.settings += "about" } }
    }
}

@Composable
private fun LanguagePage(prefs: Prefs) {
    val vm = LocalVm.current
    val system = Phrases.targets.firstOrNull { it.first == Phrases.resolveTarget("system") }?.second ?: "English"
    Group {
        row {
            ChoiceItem("Home screen language", DisplayLang.entries, prefs.displayLang, { it.label }, "language",
                describe = { Phrases.say(it.tag, Phrase.NO_NOTIFICATIONS) }) { l -> vm.update { it.copy(displayLang = l) } }
        }
        row {
            val options = listOf("system") + Phrases.targets.map { it.first }
            ChoiceItem("Translate into", options, prefs.translateTo,
                { code -> if (code == "system") "Phone language ($system)" else Phrases.targets.first { it.first == code }.second }, "arrows-exchange",
                describe = { code -> Phrases.say(Phrases.resolveTarget(code), Phrase.NO_NOTIFICATIONS) }) { c -> vm.update { it.copy(translateTo = c) } }
        }
        row {
            ChoiceItem("Translations on the home screen", LearnLines.entries, prefs.learnLines, { it.label }, "school", describe = { it.summary }) { l ->
                vm.update { it.copy(learnLines = l) }
            }
        }
        if (prefs.learnLines != LearnLines.OFF) row {
            val today = java.time.LocalDate.now().toEpochDay()
            ChoiceItem("Fade translations", Prefs.FADE_DAYS, prefs.fadeDays, { d ->
                when (d) {
                    0 -> "Never"
                    prefs.fadeDays -> "Over ${fadeLabel(d)} · ${(prefs.longFade(today) * 100).roundToInt()}% left"
                    else -> "Over ${fadeLabel(d)}"
                }
            }, "contrast", describe = { d -> if (d == 0) "Translations stay as they are" else "Fainter each day, gone after ${fadeLabel(d)}" }) { d ->
                // Choosing a pace starts it from today.
                vm.update { it.copy(fadeDays = d, fadeStart = today) }
            }
        }
        if (prefs.learnLines != LearnLines.OFF && prefs.fadeDays > 0) row {
            Item("Start fading again", "Bring the translations back to full strength from today", "restore") {
                vm.update { it.copy(fadeStart = java.time.LocalDate.now().toEpochDay()) }
            }
        }
        if (prefs.learnLines != LearnLines.OFF) row {
            val now = java.time.LocalTime.now()
            val left = (prefs.dayFadeAt(now.hour * 60 + now.minute) * 100).roundToInt()
            SwitchItem(
                "Fade during the day",
                "Full at ${hhmm(prefs.dayFadeFrom, prefs.clock24)}, gone by ${hhmm(prefs.dayFadeTo, prefs.clock24)}, back the next morning" +
                    if (prefs.dayFade) " · now $left%" else "",
                prefs.dayFade,
                "sunrise",
            ) { v -> vm.update { it.copy(dayFade = v) } }
        }
        if (prefs.learnLines != LearnLines.OFF && prefs.dayFade) {
            row { TimeItem("Full strength at", prefs.dayFadeFrom, prefs.clock24, "sun-high") { m -> vm.update { it.copy(dayFadeFrom = m) } } }
            row { TimeItem("Gone by", prefs.dayFadeTo, prefs.clock24, "moon") { m -> vm.update { it.copy(dayFadeTo = m) } } }
        }
        row {
            SwitchItem("Word of the day", "A new word each day under the weather, with its reading and meaning. Tap it for another", prefs.wordOfDay, "vocabulary") { v ->
                vm.update { it.copy(wordOfDay = v) }
            }
        }
        if (prefs.displayLang == DisplayLang.JA && prefs.learnLines != LearnLines.OFF) row {
            SwitchItem("Reading on the home screen", "The kana or rōmaji above each translation", prefs.learnReading, "abc") { v -> vm.update { it.copy(learnReading = v) } }
        }
        if (prefs.displayLang == DisplayLang.JA) row {
            ChoiceItem("Reading hint", ReadingHint.entries, prefs.readingHint, { it.label }, "abc",
                describe = { h -> when (h) { ReadingHint.KANA -> "すいようび"; ReadingHint.ROMAJI -> "suiyōbi"; ReadingHint.OFF -> "Translation only" } }) { h ->
                vm.update { it.copy(readingHint = h) }
            }
        }
        row {
            ChoiceItem("Hold to translate", Prefs.HOLD_MILLIS, prefs.holdMillis, { "${it / 1000f} seconds".replace(".0 ", " ") }, "hand-finger") { ms ->
                vm.update { it.copy(holdMillis = ms) }
            }
        }
    }
    Note("Everything on the home screen is written in the language above, with small translations under the lines you choose: a little practice every time you unlock. Hold any line for the full translation and reading. It all comes from a dictionary inside the app; nothing is sent anywhere.")
}

/** The mark before each home-screen heading: free text, a few presets, or none; "//" one tap away. */
@Composable
private fun PrefixItem(prefs: Prefs) {
    val vm = LocalVm.current
    var open by remember { mutableStateOf(false) }
    Item("Line prefix", if (prefs.prefix.isEmpty()) "None" else prefs.prefix, "slash") { open = true }
    if (!open) return
    var text by remember { mutableStateOf(prefs.prefix) }
    val font = remember(prefs.textFont) { prefs.textFont.family() }
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("Line prefix") },
        text = {
            Column {
                OutlinedTextField(
                    text, { text = it.take(Prefs.PREFIX_MAX) },
                    singleLine = true,
                    label = { Text("Before each heading") },
                    placeholder = { Text("None") },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PREFIXES.forEach { p ->
                        androidx.compose.material3.FilterChip(
                            selected = text == p,
                            onClick = { text = p },
                            label = { Text(p.ifEmpty { "none" }) },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                BasicText(
                    text + Phrases.say(prefs.displayLang.tag, Phrase.NOTIFICATIONS),
                    style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = font, fontSize = 24.sp),
                )
            }
        },
        confirmButton = { TextButton({ vm.update { it.copy(prefix = text) }; open = false }) { Text("Save") } },
        dismissButton = { TextButton({ vm.update { it.copy(prefix = DEFAULT_PREFIX) }; open = false }) { Text("Back to $DEFAULT_PREFIX") } },
    )
}

private val PREFIXES = listOf("//", "》", "▸", "#", "")

private fun hhmm(minutes: Int, h24: Boolean): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h24) "%02d:%02d".format(h, m) else "%d:%02d %s".format((h + 11) % 12 + 1, m, if (h < 12) "am" else "pm")
}

/** A row showing a time of day that opens the phone's own time picker. */
@Composable
private fun TimeItem(title: String, minutes: Int, h24: Boolean, glyph: String, onSet: (Int) -> Unit) {
    val context = LocalContext.current
    Item(title, hhmm(minutes, h24), glyph) {
        android.app.TimePickerDialog(context, { _, h, m -> onSet(h * 60 + m) }, minutes / 60, minutes % 60, h24).show()
    }
}

private fun fadeLabel(days: Int) = when (days) {
    14 -> "2 weeks"
    30 -> "a month"
    90 -> "3 months"
    else -> "$days days"
}

@Composable
private fun FontsPage(prefs: Prefs) {
    val vm = LocalVm.current
    val sample = prefs.prefix + Phrases.say(prefs.displayLang.tag, Phrase.CLOUDY) + " 6°C"
    Group("Text") {
        row {
            ChoiceItem("Home screen font", FontChoice.textFonts, prefs.textFont, { it.label }, "typography", preview = { f ->
                BasicText(sample, style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = f.family(), fontSize = 20.sp))
            }) { f -> vm.update { it.copy(textFont = f) } }
        }
        row { PrefixItem(prefs) }
    }
    Group("Layout") {
        row {
            ChoiceItem("Vertical position", InfoAlign.entries, prefs.infoAlign, { it.label }, prefs.infoAlign.glyph,
                describe = { a -> when (a) {
                    InfoAlign.TOP -> "Clock at the top; new lines grow downwards"
                    InfoAlign.MIDDLE -> "Centred; new lines push the clock up a little"
                    InfoAlign.BOTTOM -> "Within thumb reach; new lines push the clock up"
                } }) { a -> vm.update { it.copy(infoAlign = a) } }
        }
        row { Item("Clock", "Font, sizes, layout, position and outline, with a live preview", "clock") { vm.settings += "clock" } }
        row { Item("Order of lines", prefs.blockOrder.joinToString(" · ") { it.label.substringBefore(' ') }, "arrows-sort") { vm.settings += "order" } }
        row {
            ChoiceItem("Camera hole", CutoutMode.entries, prefs.cutoutMode, { it.label }, "camera-selfie", describe = { it.summary }) { m ->
                vm.update { it.copy(cutoutMode = m) }
            }
        }
    }
    if (!prefs.hideStatusBar) Note("The camera hole setting matters only while the status bar is hidden (Settings › Privacy); with it showing, the hole sits inside the status bar.")
    Note("Bundled fonts carry only the characters the home screen uses. Letters they lack (Korean, for one) come from the phone's own font.")
}

/**
 * Everything about the clock, under a live preview that stays put while the
 * settings scroll. Sliders move a draft the preview follows; letting go saves it.
 */
@Composable
private fun ClockPage(prefs: Prefs) {
    val vm = LocalVm.current
    var draft by remember(prefs) { mutableStateOf(prefs) }
    val now by produceNow()
    val face = if (draft.clockShuffle) FontChoice.daily(now.toLocalDate()) else draft.clockFont
    val family = remember(face, CustomFonts.clock) { face.family() }
    val context = LocalContext.current
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        CustomFonts.import(context, uri)
            .onSuccess { name -> vm.update { it.copy(clockFont = FontChoice.CUSTOM, customClockFont = name, clockShuffle = false) } }
            .onFailure { android.widget.Toast.makeText(context, "That file isn't a font Kamei can use", android.widget.Toast.LENGTH_LONG).show() }
    }
    // Saves the draft's clock fields (the rest of the settings may have moved on meanwhile).
    val save = {
        val d = draft
        vm.update {
            it.copy(
                hourSize = d.hourSize, minuteSize = d.minuteSize, clockStroke = d.clockStroke,
                hourOffsetX = d.hourOffsetX, hourOffsetY = d.hourOffsetY, minuteOffsetX = d.minuteOffsetX, minuteOffsetY = d.minuteOffsetY,
            )
        }
    }
    Column {
        Text("Clock", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp))
        Box(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(min = 180.dp, max = 340.dp)
                .clip(RoundedCornerShape(28.dp)).background(LocalLook.current.background).padding(horizontal = 28.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            ClockFace(now, draft, family)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 32.dp)) {
            Group("Face") {
                if (!prefs.clockShuffle) row {
                    // Clock faces first (the reason to open this list), then the text fonts' digits.
                    val faces = FontChoice.entries.filter { !it.text && (it != FontChoice.CUSTOM || CustomFonts.clock != null) } + FontChoice.textFonts
                    ChoiceItem("Font", faces, prefs.clockFont, { if (it == FontChoice.CUSTOM) prefs.customClockFont.ifBlank { it.label } else it.label }, "typography", preview = { f ->
                        BasicText("09:31", style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = f.family(), fontSize = 30.sp,
                            drawStyle = if (prefs.clockOutline) Stroke(2f) else null))
                    }) { f -> vm.update { it.copy(clockFont = f) } }
                }
                row {
                    Item(
                        "Use your own font",
                        if (CustomFonts.clock != null && prefs.customClockFont.isNotBlank()) "${prefs.customClockFont} · tap to pick another" else "A .ttf or .otf file from your phone",
                        "file-upload",
                    ) {
                        // Font files come with all sorts of types; the font check sorts out what isn't one.
                        pickFont.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/font-sfnt", "application/octet-stream", "*/*"))
                    }
                }
                row {
                    val today = FontChoice.daily(java.time.LocalDate.now())
                    SwitchItem("New font every day", if (prefs.clockShuffle) "Today: ${today.label}" else "A different face each day", prefs.clockShuffle, "arrows-shuffle") { v ->
                        vm.update { it.copy(clockShuffle = v) }
                    }
                }
                row {
                    ChoiceItem("Layout", ClockLayout.entries, prefs.clockLayout, { it.label }, if (prefs.clockLayout == ClockLayout.STACKED) "layout-rows" else "layout-columns",
                        describe = { if (it == ClockLayout.STACKED) "Hours above minutes" else "Hours and minutes side by side" }) { l -> vm.update { it.copy(clockLayout = l) } }
                }
                row { SwitchItem("24-hour clock", null, prefs.clock24, "clock-24") { v -> vm.update { it.copy(clock24 = v) } } }
            }
            val sizes = Prefs.CLOCK_SIZES.first.toFloat()..Prefs.CLOCK_SIZES.last.toFloat()
            Group("Size") {
                row { LiveSlider("Hours", draft.hourSize.toFloat(), sizes, format = { "${it.roundToInt()} sp" }, onChange = { draft = draft.copy(hourSize = it.roundToInt()) }, onDone = save) }
                row { LiveSlider("Minutes", draft.minuteSize.toFloat(), sizes, format = { "${it.roundToInt()} sp" }, onChange = { draft = draft.copy(minuteSize = it.roundToInt()) }, onDone = save) }
            }
            Group("Outline") {
                row { SwitchItem("Outlined digits", "Draw the clock as outlines, like the sketch", prefs.clockOutline, "letter-o") { v -> vm.update { it.copy(clockOutline = v) } } }
                if (prefs.clockOutline) row {
                    LiveSlider("Line thickness", draft.clockStroke, Prefs.CLOCK_STROKE, format = { "%.1f dp".format(it) },
                        onChange = { draft = draft.copy(clockStroke = (it * 10).roundToInt() / 10f) }, onDone = save)
                }
            }
            val offsets = Prefs.CLOCK_OFFSETS.first.toFloat()..Prefs.CLOCK_OFFSETS.last.toFloat()
            fun dp(v: Float) = "${v.roundToInt()} dp"
            Group("Position") {
                row { LiveSlider("Hours, sideways", draft.hourOffsetX.toFloat(), offsets, format = ::dp, onChange = { draft = draft.copy(hourOffsetX = it.roundToInt()) }, onDone = save) }
                row { LiveSlider("Hours, up and down", draft.hourOffsetY.toFloat(), offsets, format = ::dp, onChange = { draft = draft.copy(hourOffsetY = it.roundToInt()) }, onDone = save) }
                row { LiveSlider("Minutes, sideways", draft.minuteOffsetX.toFloat(), offsets, format = ::dp, onChange = { draft = draft.copy(minuteOffsetX = it.roundToInt()) }, onDone = save) }
                row { LiveSlider("Minutes, up and down", draft.minuteOffsetY.toFloat(), offsets, format = ::dp, onChange = { draft = draft.copy(minuteOffsetY = it.roundToInt()) }, onDone = save) }
            }
            Group {
                row {
                    Item("Reset clock", "Default sizes, layout, position and outline (keeps the font)", "restore") {
                        val d = Prefs()
                        vm.update {
                            it.copy(
                                hourSize = d.hourSize, minuteSize = d.minuteSize, clockLayout = d.clockLayout, clockOutline = d.clockOutline, clockStroke = d.clockStroke,
                                hourOffsetX = 0, hourOffsetY = 0, minuteOffsetX = 0, minuteOffsetY = 0,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A slider whose every move is shown at once ([onChange]); [onDone] when the finger lifts. */
@Composable
private fun LiveSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, format: (Float) -> String, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value, onChange, valueRange = range, onValueChangeFinished = onDone)
    }
}

@Composable
private fun IconsPage(prefs: Prefs) {
    val vm = LocalVm.current
    val packs = remember { vm.packs.installed() }
    Group {
        row {
            ChoiceItem("Icon style", IconStyle.entries, prefs.iconStyle, { it.label }, "icons", describe = { it.summary }) { s -> vm.update { it.copy(iconStyle = s) } }
        }
        row {
            ChoiceItem("Glyph set", GlyphSet.entries, prefs.glyphSet, { it.label }, "category",
                describe = { if (it == GlyphSet.HINTS) "Sideways hints: phone → ear, music → feather, bank → anchor" else "${vm.glyphs.pool(it).size} glyphs" }) { g -> vm.update { it.copy(glyphSet = g) } }
        }
        row { Item("Reshuffle glyphs", "Deal every app a new unexpected glyph", "arrows-shuffle") { vm.update { it.copy(glyphSeed = it.glyphSeed + 1) } } }
        row {
            SliderItem("Line width", prefs.stroke, Prefs.STROKE, steps = 4, format = { "%.2f".format(it) }) { v -> vm.update { it.copy(stroke = (v * 4).roundToInt() / 4f) } }
        }
    }
    Group {
        row {
            val options = listOf("") + packs.map { it.pkg }
            ChoiceItem("Icon pack", options, prefs.iconPack, { p -> if (p.isEmpty()) "None" else packs.firstOrNull { it.pkg == p }?.label ?: p }, "packages",
                describe = { p -> if (p.isEmpty()) "Use the style above" else "Used for every app it covers (not with unexpected glyphs)" }) { p -> vm.update { it.copy(iconPack = p) } }
        }
        row {
            Item("Reset per-app icons", "${prefs.customIcons.size} apps have their own icon", "restore") { vm.update { it.copy(customIcons = emptyMap()) } }
        }
    }
    Note("Change one app's icon from its options: hold it in the drawer or in the favourites stack. The glyph library is Tabler Icons (MIT), about 4,900 line icons.")
    if (packs.isEmpty()) Note("No icon packs installed. Line-style packs such as Arcticons fit the look best.")
}

@Composable
private fun DrawerPage(prefs: Prefs) {
    val vm = LocalVm.current
    Group("Drawer") {
        row { ChoiceItem("Layout", DrawerLayout.entries, prefs.drawerLayout, { it.label }, "layout-grid") { l -> vm.update { it.copy(drawerLayout = l) } } }
        row {
            SliderItem("Columns", prefs.drawerColumns.toFloat(), 3f..6f, steps = 2, format = { "${it.roundToInt()}" }) { v -> vm.update { it.copy(drawerColumns = v.roundToInt()) } }
        }
        row {
            val ok = Transcribe.supports(prefs.displayLang)
            val sample = if (ok) "YouTube → ${Transcribe.label("YouTube", prefs.displayLang)}" else "Not available for Chinese: it has no everyday phonetic script"
            SwitchItem("Write names as they sound", sample, prefs.phoneticLabels && ok, "language-katakana") { v ->
                if (ok) vm.update { it.copy(phoneticLabels = v) }
            }
        }
        row {
            SwitchItem("Search right away", "Opening the drawer brings up the keyboard, so you can just type", prefs.drawerKeyboard, "keyboard") { v ->
                vm.update { it.copy(drawerKeyboard = v) }
            }
        }
        row { Item("Hidden apps", "${prefs.hidden.size} hidden", "eye-off") { vm.settings += "hidden" } }
    }
    Group("Favourites") {
        row {
            SliderItem("Icon size", prefs.railIconDp.toFloat(), 28f..56f, steps = 6, format = { "${it.roundToInt()} dp" }) { v -> vm.update { it.copy(railIconDp = v.roundToInt()) } }
        }
    }
    Note("Add favourites from the drawer: hold an app, then \"Add to favourites\". In the stack, hold an icon and drag it up or down to reorder.")
    if (prefs.phoneticLabels) Note("With names written as they sound, search still matches the real names too, so you can type \"you\" on any keyboard. Anyone who knows that can do the same.")
}

@Composable
private fun HiddenPage(prefs: Prefs) {
    val vm = LocalVm.current
    val all by vm.repo.apps.collectAsState()
    val hidden = all.filter { it.key in prefs.hidden }
    if (hidden.isEmpty()) {
        Note("No hidden apps. Hide one from its options in the drawer.")
        return
    }
    Group {
        hidden.forEach { app ->
            row {
                Item(prefs.renames[app.key] ?: app.label, "Tap to show it again", trailing = { AppIcon(app.key, 28.dp, tint = MaterialTheme.colorScheme.onSurface) }) {
                    vm.hide(app.key, false)
                }
            }
        }
    }
}

@Composable
private fun WeatherPage(prefs: Prefs) {
    val vm = LocalVm.current
    val state by vm.weather.state.collectAsState()
    val allApps by vm.repo.apps.collectAsState()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshWeather(force = true) }
    Group {
        row {
            SwitchItem("Show weather", "Connects to the weather service you pick below", prefs.weatherOn, "cloud") { v ->
                vm.update { it.copy(weatherOn = v) }
                if (v && prefs.locationMode == LocationMode.DEVICE && !vm.weather.hasLocationPermission()) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }
    if (!prefs.weatherOn) {
        Note("Off: Kamei makes no network connections at all.")
        return
    }
    Group("Forecast") {
        row { ChoiceItem("Source", WeatherSource.entries, prefs.weatherSource, { it.label }, "satellite", describe = { it.summary }) { s -> vm.update { it.copy(weatherSource = s) } } }
        row {
            ChoiceItem("Location", LocationMode.entries, prefs.locationMode, { it.label }, "map-pin",
                describe = { if (it == LocationMode.DEVICE) "Rough location from the phone, rounded to about 1 km" else "A place you choose; the phone's location is never read" }) { m ->
                vm.update { it.copy(locationMode = m) }
                if (m == LocationMode.DEVICE && !vm.weather.hasLocationPermission()) permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (m == LocationMode.FIXED && prefs.place == null) vm.settings += "place"
            }
        }
        if (prefs.locationMode == LocationMode.FIXED) {
            row { Item("Place", prefs.place?.name ?: "Not set", "building-community") { vm.settings += "place" } }
        } else if (!vm.weather.hasLocationPermission()) {
            row { Item("Allow approximate location", "Needed for weather where you are", "location") { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) } }
        }
        row {
            val status = when (state.status) {
                WeatherStatus.UPDATING -> "Updating…"
                WeatherStatus.NO_LOCATION -> "No location yet"
                WeatherStatus.ERROR -> "Couldn't update" + (state.forecast?.let { ", showing ${ago(it.fetchedAt)}" } ?: "")
                else -> state.forecast?.let { "Updated ${ago(it.fetchedAt)} · ${it.source}" } ?: "No data yet"
            }
            Item("Update now", status, "refresh") { vm.refreshWeather(force = true) }
        }
    }
    Group("Look") {
        row { ChoiceItem("Weather icons", WeatherGlyphs.entries, prefs.weatherGlyphs, { it.label }, "sun") { g -> vm.update { it.copy(weatherGlyphs = g) } } }
        row { SwitchItem("Blue when rain is coming", "The closer the rain, the bluer the line", prefs.rainTint, "droplet") { v -> vm.update { it.copy(rainTint = v) } } }
        row {
            ChoiceItem("Look ahead", Prefs.RAIN_WINDOWS, prefs.rainWindow, { "${it / 60} h" }, "hourglass") { w -> vm.update { it.copy(rainWindow = w) } }
        }
        row { SwitchItem("Fahrenheit", null, prefs.fahrenheit, "temperature") { v -> vm.update { it.copy(fahrenheit = v) } } }
    }
    Group("Tap") {
        row {
            val auto = vm.weatherApp()
            val options = listOf("") + allApps.map { it.key.encode() }
            ChoiceItem("Weather app", options, prefs.weatherApp, { code ->
                if (code.isEmpty()) "Automatic (${auto?.let { vm.label(it) } ?: "built-in forecast"})"
                else AppKey.decode(code)?.let { vm.label(it) } ?: code
            }, "hand-click", describe = { if (it.isEmpty()) "Yr or Drops when installed" else null }) { c -> vm.update { it.copy(weatherApp = c) } }
        }
    }
    state.forecast?.let { Note(credit(it.source)) }
}

private fun ago(t: Long): String = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()))

@Composable
private fun PlacePage() {
    val vm = LocalVm.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(400)
        searching = true
        results = vm.weather.searchPlaces(query)
        searching = false
    }
    Column {
        Text("Place", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp))
        OutlinedTextField(
            query, { query = it },
            singleLine = true,
            placeholder = { Text("Search for a town") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
        )
        Note(if (searching) "Searching…" else "The name you type is sent to Open-Meteo's place search.")
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            items(results) { p ->
                Item(p.name, "%.2f, %.2f".format(p.lat, p.lon), "map-pin") {
                    vm.update { it.copy(place = p, locationMode = LocationMode.FIXED) }
                    scope.launch { vm.settings.removeAt(vm.settings.lastIndex) }
                }
            }
        }
    }
}

@Composable
private fun NotificationsPage(prefs: Prefs) {
    val vm = LocalVm.current
    val context = LocalContext.current
    Group {
        row {
            Item("Notification access", if (vm.notifAccess) "Allowed" else "Not allowed: tap to allow", "bell") {
                SystemActions.open(context, Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        row {
            ChoiceItem("Show as", NotifStyle.entries, prefs.notifStyle, { it.label }, "apps", describe = { it.summary }) { s -> vm.update { it.copy(notifStyle = s) } }
        }
    }
    Note("Notification text stays in memory only while the notification exists; nothing is saved or sent. Tap a line to open it, as in the shade.")
}

private fun gestureLabel(vm: com.asnidev.kamei.LauncherViewModel, g: Gesture): String =
    if (g.kind == GestureKind.APP && g.app != null) "Open ${vm.label(g.app)}" else g.kind.label

@Composable
private fun GesturesPage(prefs: Prefs) {
    val vm = LocalVm.current
    val torchless = !vm.torch.available
    // Which side is waiting for an app to be picked.
    var picking by remember { mutableStateOf<String?>(null) }
    val kinds = GestureKind.entries.filter { !(it == GestureKind.TORCH && torchless) }
    @Composable
    fun side(title: String, glyph: String, g: Gesture, key: String, set: (Prefs, Gesture) -> Prefs) {
        ChoiceItem(title, kinds, g.kind, { k -> if (k == g.kind) gestureLabel(vm, g) else k.label }, glyph) { k ->
            if (k == GestureKind.APP) picking = key else vm.update { set(it, Gesture(k)) }
        }
    }
    Group("Swipe on the home screen") {
        row { side("Swipe left", "hand-finger-left", prefs.swipeLeft, "left") { p, g -> p.copy(swipeLeft = g) } }
        row { side("Swipe right", "hand-finger-right", prefs.swipeRight, "right") { p, g -> p.copy(swipeRight = g) } }
    }
    Group("Fixed") {
        row { Item("Swipe up", "App drawer", "arrow-big-up") }
        row { Item("Swipe down", "Notifications", "arrow-big-down") }
    }
    Note("Set a side to \"Nothing\" to switch it off. Swipe up and down stay fixed so the drawer is always one swipe away.")
    picking?.let { side ->
        AppPickerDialog(onDismiss = { picking = null }) { key ->
            vm.update { if (side == "left") it.copy(swipeLeft = Gesture(GestureKind.APP, key)) else it.copy(swipeRight = Gesture(GestureKind.APP, key)) }
            picking = null
        }
    }
}

/** A list of every app to choose one from. */
@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (AppKey) -> Unit) {
    val vm = LocalVm.current
    val apps by vm.apps.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an app") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(apps, key = { it.key.encode() }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onPick(app.key) }.padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.key, 28.dp, tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(16.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

/** The home screen's lines top to bottom, each moved with up and down arrows. */
@Composable
private fun OrderPage(prefs: Prefs) {
    val vm = LocalVm.current
    val order = prefs.blockOrder
    fun move(i: Int, by: Int) {
        val j = i + by
        if (j !in order.indices) return
        vm.update { it.copy(blockOrder = order.toMutableList().apply { add(j, removeAt(i)) }) }
    }
    Group {
        order.forEachIndexed { i, b ->
            row {
                Item(b.label, null, b.glyph, trailing = {
                    Row {
                        ArrowButton("arrow-up", enabled = i > 0) { move(i, -1) }
                        ArrowButton("arrow-down", enabled = i < order.lastIndex) { move(i, 1) }
                    }
                })
            }
        }
    }
    Group {
        row { Item("Default order", "Clock, day, weather, warnings, word, notifications", "restore") { vm.update { it.copy(blockOrder = HomeBlock.entries) } } }
    }
    Note("Warnings and the word of the day only appear when there's something to show; they keep their place in the order for when they do.")
}

@Composable
private fun ArrowButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.3f)
    Box(
        Modifier.size(44.dp).clip(CircleShape).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { GlyphIcon(glyph, 22.dp, tint = tint, stroke = 1.9f) }
}

@Composable
private fun StatusPage(prefs: Prefs) {
    val vm = LocalVm.current
    Group {
        row {
            SwitchItem("Battery warning", "From 25%: orange, turning red by 6%, blinking from 5%. Hidden while charging", prefs.batteryLine, "battery-2") { v ->
                vm.update { it.copy(batteryLine = v) }
            }
        }
        row {
            SwitchItem("Signal warning", "One bar: orange. No bars: red. No service: blinking red. Quiet in airplane mode", prefs.signalLine, "antenna-bars-2") { v ->
                vm.update { it.copy(signalLine = v) }
            }
        }
    }
    Note("The lines appear under the weather only when there's something to warn about, in the home screen's language like everything else. Tap one for the phone's battery or network settings.")
}

@Composable
private fun WallpaperPage(prefs: Prefs) {
    val vm = LocalVm.current
    val context = LocalContext.current
    Group {
        row { SwitchItem("Show wallpaper", "Your phone's wallpaper behind the home screen", prefs.showWallpaper, "photo") { v -> vm.update { it.copy(showWallpaper = v) } } }
        if (prefs.showWallpaper) {
            row {
                SliderItem("Darken", prefs.wallpaperDim.toFloat(), 0f..90f, steps = 8, format = { "${it.roundToInt()}%" }) { v ->
                    vm.update { it.copy(wallpaperDim = v.roundToInt()) }
                }
            }
        }
        row {
            Item("Change wallpaper", "Opens the phone's wallpaper picker", "brush") {
                SystemActions.open(context, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
            }
        }
    }
    Note("Off, the home screen is pure black, which also saves power on OLED screens.")
}

@Composable
private fun PrivacyPage(prefs: Prefs) {
    val vm = LocalVm.current
    Group {
        row { SwitchItem("Hide status bar", "Swipe down on the home screen still opens notifications", prefs.hideStatusBar, "layout-navbar") { v -> vm.update { it.copy(hideStatusBar = v) } } }
    }
    Group("What leaves this phone") {
        row { Item("Nothing, with weather off", "No account, analytics, ads or crash reporting", "shield-check") }
        row { Item("Weather", "api.met.no and/or api.open-meteo.com get your location rounded to about 1 km, and nothing else", "cloud") }
        row { Item("Place search", "The town name you type goes to geocoding-api.open-meteo.com", "search") }
    }
    Group("What stays on it") {
        row { Item("Notifications", "Read to show them at home; held in memory only, never saved or sent", "bell") }
        row { Item("Translations", "Looked up in the app's own dictionary", "language") }
    }
}

@Composable
private fun AboutPage() {
    Group {
        row { Item("Kamei", "Version ${BuildConfig.VERSION_NAME} · a private launcher", "info-circle") }
        row { Item("Tabler Icons", "MIT licence · tabler.io/icons", "icons") }
        row { Item("Fonts", FontChoice.entries.drop(1).joinToString(", ") { it.label } + " · SIL Open Font Licence", "typography") }
        row { Item("Weather data", "MET Norway (Yr) and Open-Meteo.com · CC BY 4.0", "cloud") }
    }
    Note("Licence texts are in the app's assets/licenses folder.")
}
