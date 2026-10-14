package com.asnidev.kamei.ui

import android.os.Build
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import com.asnidev.kamei.system.CustomFonts
import com.asnidev.kamei.LauncherViewModel
import com.asnidev.kamei.R
import com.asnidev.kamei.data.FontChoice
import com.asnidev.kamei.data.HomeTheme

/**
 * Everything the home screen and drawer are painted with, for the chosen theme in the
 * phone's colours (Android 12+ wallpaper colours; a neutral set before). [light] says
 * which way the system bars' icons should go.
 */
@Immutable
data class Look(
    val theme: HomeTheme,
    val light: Boolean,
    val background: Color,
    val text: Color,
    val dim: Color,
    val accent: Color,
    /** Card and chip fills (Material, Tokyo). */
    val card: Color,
    /** Fine rules and outlines. */
    val line: Color,
    val clock: Color,
    val iconTint: Color,
    /** Material: the tonal circle behind favourite icons, and the icon on it. */
    val iconCircle: Color,
    val onIconCircle: Color,
    val rain: Color,
    val warn: Color,
    val alarm: Color,
) {
    fun rainTint(blueness: Float): Color = lerp(text, rain, blueness.coerceIn(0f, 1f))
    fun alertTint(heat: Float): Color = lerp(warn, alarm, heat.coerceIn(0f, 1f))
    val cards get() = theme == HomeTheme.MATERIAL
    val tokyo get() = theme == HomeTheme.TOKYO
    /** Whether headings start with the user's prefix (Original); the others mark them their own way. */
    val usesPrefix get() = theme == HomeTheme.ORIGINAL
}

val LocalLook = staticCompositionLocalOf<Look> { error("no look") }

/** The phone's colour scheme, light or dark: wallpaper colours on Android 12+. */
@Composable
fun phoneScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> FALLBACK_DARK
        else -> lightColorScheme(primary = Color(0xFF3F5F91), primaryContainer = Color(0xFFD6E3FF), onPrimaryContainer = Color(0xFF001B3E))
    }
}

private val FALLBACK_DARK = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    surface = Color(0xFF131314),
    surfaceContainer = Color(0xFF1E1F20),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF333537),
)

/** Whether a theme follows the phone's dark mode (Original is always dark). */
@Composable
fun themeIsDark(theme: HomeTheme): Boolean = theme == HomeTheme.ORIGINAL || isSystemInDarkTheme()

@Composable
fun rememberLook(theme: HomeTheme): Look {
    val dark = themeIsDark(theme)
    val scheme = phoneScheme(dark)
    return remember(theme, scheme, dark) {
        when (theme) {
            HomeTheme.ORIGINAL -> Look(
                theme, light = false, background = Color.Black, text = Ink.text, dim = Ink.dim, accent = scheme.primary,
                card = Color(0xFF161616), line = Ink.faint, clock = Ink.text, iconTint = Ink.text,
                iconCircle = Color.Transparent, onIconCircle = Ink.text, rain = Ink.rain, warn = Ink.warn, alarm = Ink.alarm,
            )
            HomeTheme.MATERIAL -> Look(
                theme, light = !dark, background = scheme.surface, text = scheme.onSurface, dim = scheme.onSurfaceVariant,
                accent = scheme.primary, card = scheme.surfaceContainerHigh, line = scheme.outlineVariant, clock = scheme.primary,
                iconTint = scheme.onSurfaceVariant, iconCircle = scheme.primaryContainer, onIconCircle = scheme.onPrimaryContainer,
                rain = if (dark) Ink.rain else Color(0xFF1565C0), warn = if (dark) Ink.warn else Color(0xFFE65100),
                alarm = if (dark) Ink.alarm else Color(0xFFC62828),
            )
            HomeTheme.TOKYO -> if (dark) Look(
                theme, light = false, background = Color(0xFF121212), text = Color(0xFFEDEDE8), dim = Color(0xFF9C9C96),
                accent = scheme.primary, card = Color(0xFF1C1C1C), line = Color(0xFF2E2E2C), clock = Color(0xFFEDEDE8),
                iconTint = Color(0xFFEDEDE8), iconCircle = Color.Transparent, onIconCircle = Color(0xFFEDEDE8),
                rain = Ink.rain, warn = Ink.warn, alarm = Ink.alarm,
            ) else Look(
                theme, light = true, background = Color(0xFFFAFAF7), text = Color(0xFF1A1A1A), dim = Color(0xFF6B6B66),
                accent = scheme.primary, card = Color(0xFFF0F0EB), line = Color(0xFFDEDED8), clock = Color(0xFF1A1A1A),
                iconTint = Color(0xFF1A1A1A), iconCircle = Color.Transparent, onIconCircle = Color(0xFF1A1A1A),
                rain = Color(0xFF1565C0), warn = Color(0xFFE65100), alarm = Color(0xFFC62828),
            )
        }
    }
}

/** The fixed colours of the Original theme: white on black, like the sketch. */
object Ink {
    val text = Color(0xFFF2F2F2)
    val dim = Color(0xFF9A9A9A)
    val faint = Color(0xFF3A3A3A)
    val background = Color.Black
    /** The weather line's colour when rain is imminent. */
    val rain = Color(0xFF4DA3FF)

    fun rainTint(blueness: Float): Color = lerp(text, rain, blueness.coerceIn(0f, 1f))

    /** Warning lines: orange when they appear, red when it's serious. */
    val warn = Color(0xFFFF9F1C)
    val alarm = Color(0xFFFF3B30)

    fun alertTint(heat: Float): Color = lerp(warn, alarm, heat.coerceIn(0f, 1f))
}

fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.SYSTEM -> FontFamily.Default
    // The user's file; the phone's font until one loads.
    FontChoice.CUSTOM -> CustomFonts.clock?.let { FontFamily(Typeface(it)) } ?: FontFamily.Default
    else -> FontFamily(Font(fontRes()))
}

private fun FontChoice.fontRes(): Int = when (this) {
    FontChoice.SYSTEM, FontChoice.CUSTOM -> 0
    FontChoice.MPLUS_CODE -> R.font.mplus1code
    FontChoice.ZEN_MARU -> R.font.zenmarugothic
    FontChoice.DOT_GOTHIC -> R.font.dotgothic16
    FontChoice.SHIPPORI -> R.font.shipporimincho
    FontChoice.KLEE -> R.font.kleeone
    FontChoice.MURECHO -> R.font.murecho
    FontChoice.BIZ_UD -> R.font.bizudgothic
    FontChoice.KIWI_MARU -> R.font.kiwimaru
    FontChoice.KAISEI_DECOL -> R.font.kaiseidecol
    FontChoice.DELA_GOTHIC -> R.font.delagothicone
    FontChoice.RAMPART -> R.font.rampartone
    FontChoice.TRAIN -> R.font.trainone
    FontChoice.STICK -> R.font.stick
    FontChoice.ROCKNROLL -> R.font.rocknrollone
    FontChoice.REGGAE -> R.font.reggaeone
    FontChoice.YUSEI_MAGIC -> R.font.yuseimagic
    FontChoice.HACHI_MARU -> R.font.hachimarupop
    FontChoice.POTTA -> R.font.pottaone
    FontChoice.YUJI_SYUKU -> R.font.yujisyuku
    FontChoice.INTER -> R.font.inter
    FontChoice.SPACE_GROTESK -> R.font.spacegrotesk
    FontChoice.UNBOUNDED -> R.font.unbounded
    FontChoice.BEBAS -> R.font.bebasneue
    FontChoice.BIG_SHOULDERS -> R.font.bigshouldersdisplay
    FontChoice.SYNE -> R.font.syne
    FontChoice.INSTRUMENT_SERIF -> R.font.instrumentserif
    FontChoice.FRAUNCES -> R.font.fraunces
    FontChoice.ORBITRON -> R.font.orbitron
    FontChoice.MAJOR_MONO -> R.font.majormonodisplay
    FontChoice.RUBIK_MONO -> R.font.rubikmonoone
    FontChoice.DOTO -> R.font.doto
    FontChoice.SIXTYFOUR -> R.font.sixtyfour
    FontChoice.MONOTON -> R.font.monoton
    FontChoice.BUNGEE_HAIRLINE -> R.font.bungeehairline
    FontChoice.TILT_NEON -> R.font.tiltneon
    FontChoice.CODYSTAR -> R.font.codystar
}

val LocalVm = staticCompositionLocalOf<LauncherViewModel> { error("no view model") }

/**
 * Sheets, dialogs and settings look like the phone's own Settings app: the
 * wallpaper-based Material You colours where the phone has them (Android 12+),
 * a neutral dark scheme otherwise.
 */
@Composable
fun SystemLook(dark: Boolean = true, content: @Composable () -> Unit) {
    val scheme = phoneScheme(dark)
    MaterialTheme(colorScheme = scheme) {
        // Text outside a Surface takes this colour; the default is black.
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
    }
}
