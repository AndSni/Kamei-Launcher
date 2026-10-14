package com.asnidev.kamei.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.ReadingHint
import com.asnidev.kamei.lang.JaReading
import com.asnidev.kamei.lang.Phrases
import com.asnidev.kamei.lang.Words
import com.asnidev.kamei.weather.Weather
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(key: AppKey) {
    val vm = LocalVm.current
    val prefs by vm.prefs.collectAsState()
    val fav = key in prefs.favorites
    ModalBottomSheet(onDismissRequest = { vm.sheetFor = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val work = vm.repo.isWork(key)
                AppIcon(key, 44.dp, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(vm.label(key) + if (work && prefs.workMark.star) "*" else "", style = MaterialTheme.typography.titleLarge)
                    Text(if (work) "Work profile · ${key.pkg}" else key.pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            SheetRow(if (fav) "star-off" else "star", if (fav) "Remove from favourites" else "Add to favourites") {
                vm.toggleFavorite(key); vm.sheetFor = null
            }
            SheetRow("palette", "Change icon") { vm.sheetFor = null; vm.pickerFor = key }
            SheetRow("forms", "Rename") { vm.sheetFor = null; vm.renameFor = key }
            SheetRow("eye-off", "Hide from drawer") { vm.hide(key, true); vm.sheetFor = null }
            SheetRow("info-circle", "App info") { vm.repo.openInfo(key); vm.sheetFor = null }
            SheetRow("trash", "Uninstall") { vm.repo.uninstall(key); vm.sheetFor = null }
        }
    }
}

@Composable
fun SheetRow(glyph: String, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(glyph, 24.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant, stroke = 1.75f)
        Spacer(Modifier.width(24.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun RenameDialog(key: AppKey) {
    val vm = LocalVm.current
    var text by remember { mutableStateOf(vm.label(key)) }
    AlertDialog(
        onDismissRequest = { vm.renameFor = null },
        title = { Text("Rename") },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name in the drawer") }) },
        confirmButton = { TextButton({ vm.rename(key, text); vm.renameFor = null }) { Text("Save") } },
        dismissButton = {
            TextButton({ vm.rename(key, ""); vm.renameFor = null }) { Text("Reset") }
        },
    )
}

/** What a long hold shows: the home-screen text and its translation, from the bundled dictionary. */
@Composable
fun TranslationPopup(words: Words, prefs: Prefs) {
    val vm = LocalVm.current
    val font = remember(prefs.textFont) { prefs.textFont.family() }
    Dialog(onDismissRequest = { vm.translation = null }) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { vm.translation = null }
                .padding(28.dp)
                .fillMaxWidth(),
        ) {
            androidx.compose.foundation.text.BasicText(
                words.display,
                style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = font, fontSize = 22.sp, lineHeight = 30.sp),
            )
            val reading = words.reading?.takeIf { it.isNotBlank() }?.let { r ->
                when (prefs.readingHint) {
                    ReadingHint.KANA -> r
                    ReadingHint.ROMAJI -> r.lines().joinToString("\n") { JaReading.romaji(it) }
                    ReadingHint.OFF -> null
                }
            }
            if (reading != null) {
                Spacer(Modifier.height(6.dp))
                // Large enough that ぱ (circle) and ば (two dashes) can't be mistaken for each other.
                Text(reading, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.outlineVariant)
            // Short phrases big; a notification's own text at reading size.
            Text(
                words.translated,
                style = if (words.translated.length > 60) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Shown on this phone only · tap to close",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The next hours, for phones with neither Yr nor Drops installed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastSheet(prefs: Prefs) {
    val vm = LocalVm.current
    val state by vm.weather.state.collectAsState()
    val f = state.forecast
    val hhmm = remember { DateTimeFormatter.ofPattern("HH:mm") }
    ModalBottomSheet(onDismissRequest = { vm.forecastOpen = false }) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
            Text(Phrases.say(Phrases.effectiveTarget(prefs.displayLang, prefs.translateTo), com.asnidev.kamei.lang.Phrase.WEATHER), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            if (f == null) {
                Text("No forecast yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                f.hours.forEach { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(hhmm.format(Instant.ofEpochMilli(h.time).atZone(ZoneId.systemDefault())), modifier = Modifier.width(64.dp))
                        WeatherIcon(h.condition, h.night, prefs.weatherGlyphs.takeIf { it != com.asnidev.kamei.data.WeatherGlyphs.NONE }
                            ?: com.asnidev.kamei.data.WeatherGlyphs.LINE, 24.dp, MaterialTheme.colorScheme.onSurface, prefs.stroke)
                        Spacer(Modifier.width(16.dp))
                        val temp = if (prefs.fahrenheit) "${(h.tempC * 9 / 5 + 32).roundToInt()}°" else "${h.tempC.roundToInt()}°"
                        Text(temp, modifier = Modifier.width(56.dp))
                        Text(
                            Phrases.say(Phrases.effectiveTarget(prefs.displayLang, prefs.translateTo), conditionPhrase(h.condition)),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (h.mm >= Weather.WET) Text("%.1f mm".format(h.mm), color = LocalLook.current.rain)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(credit(f.source), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Both services publish under CC BY 4.0, which asks for this credit wherever the data shows. */
fun credit(source: String): String = when {
    source.startsWith("Yr") && "Open-Meteo" in source -> "Data: MET Norway (Yr) and Open-Meteo.com, CC BY 4.0"
    source.startsWith("Yr") -> "Data: MET Norway (Yr), CC BY 4.0"
    else -> "Data: Open-Meteo.com, CC BY 4.0"
}

