package com.asnidev.kamei.data

import com.asnidev.kamei.lang.DisplayLang

/** One launchable activity in one user profile ([user] is the profile's serial number). */
data class AppKey(val pkg: String, val cls: String, val user: Long) {
    fun encode(): String = "$pkg/$cls/$user"

    companion object {
        fun decode(s: String?): AppKey? {
            val parts = s?.split('/') ?: return null
            if (parts.size != 3 || parts[0].isEmpty() || parts[1].isEmpty()) return null
            return AppKey(parts[0], parts[1], parts[2].toLongOrNull() ?: return null)
        }
    }
}

/** How app icons are drawn unless the user picked one for that app. */
enum class IconStyle(val label: String, val summary: String) {
    OBSCURE("Unexpected glyphs", "Every app gets an unrelated symbol only you know"),
    LINE("Line drawing", "The app's own icon traced in thin lines"),
    THEMED("Themed", "The app's one-colour icon, where it has one"),
    MONO("Greyscale", "The app's own icon without colour"),
    ORIGINAL("Original", "The app's own icon"),
}

/** Where unexpected glyphs are drawn from: Tabler categories. */
enum class GlyphSet(val label: String, val categories: Set<String>) {
    /** Picks from each app's own sideways hints (icons/AppTypes.kt), the mixed pool for the rest. */
    HINTS("Hints only you get", setOf("Shapes", "Nature", "Animals", "Food", "Zodiac", "Symbols", "Sport", "Games", "Laundry", "Math", "Mood", "Buildings", "Vehicles")),
    MIXED("Mixed", setOf("Shapes", "Nature", "Animals", "Food", "Zodiac", "Symbols", "Sport", "Games", "Laundry", "Math", "Mood", "Buildings", "Vehicles")),
    SHAPES("Shapes & math", setOf("Shapes", "Math", "Logic")),
    NATURE("Nature & animals", setOf("Nature", "Animals", "Weather")),
    FOOD("Food", setOf("Food")),
    SIGNS("Zodiac & symbols", setOf("Zodiac", "Symbols", "Gender")),
    PLAY("Sport & games", setOf("Sport", "Games")),
    HOME("Laundry & buildings", setOf("Laundry", "Buildings")),
}

enum class DrawerLayout(val label: String) {
    GRID_LABELS("Icons with names"),
    GRID("Icons only"),
    LIST("List"),
}

/**
 * Bundled fonts (cut down by tools/make_fonts.py) and the phone's own. [text]
 * fonts carry the home screen's Japanese; the rest are clock faces (digits only).
 */
enum class FontChoice(val label: String, val text: Boolean) {
    SYSTEM("System", true),
    // Japanese text fonts.
    MPLUS_CODE("M PLUS 1 Code", true),
    ZEN_MARU("Zen Maru Gothic", true),
    DOT_GOTHIC("DotGothic16", true),
    SHIPPORI("Shippori Mincho", true),
    KLEE("Klee One", true),
    MURECHO("Murecho", true),
    BIZ_UD("BIZ UDGothic", true),
    KIWI_MARU("Kiwi Maru", true),
    KAISEI_DECOL("Kaisei Decol", true),
    DELA_GOTHIC("Dela Gothic One", true),
    RAMPART("Rampart One", true),
    TRAIN("Train One", true),
    STICK("Stick", true),
    ROCKNROLL("RocknRoll One", true),
    REGGAE("Reggae One", true),
    YUSEI_MAGIC("Yusei Magic", true),
    HACHI_MARU("Hachi Maru Pop", true),
    POTTA("Potta One", true),
    YUJI_SYUKU("Yuji Syuku", true),
    // Clock faces.
    INTER("Inter", false),
    SPACE_GROTESK("Space Grotesk", false),
    UNBOUNDED("Unbounded", false),
    BEBAS("Bebas Neue", false),
    BIG_SHOULDERS("Big Shoulders", false),
    SYNE("Syne", false),
    INSTRUMENT_SERIF("Instrument Serif", false),
    FRAUNCES("Fraunces", false),
    ORBITRON("Orbitron", false),
    MAJOR_MONO("Major Mono", false),
    RUBIK_MONO("Rubik Mono One", false),
    DOTO("Doto", false),
    SIXTYFOUR("Sixtyfour", false),
    MONOTON("Monoton", false),
    BUNGEE_HAIRLINE("Bungee Hairline", false),
    TILT_NEON("Tilt Neon", false),
    CODYSTAR("Codystar", false),
    /** A font file the user picked (system/CustomFonts.kt). */
    CUSTOM("Your own font", false),
    ;

    companion object {
        val textFonts get() = entries.filter { it.text }

        /** The clock's face for [day] when shuffling: every bundled font in turn, in a fixed scrambled order. */
        fun daily(day: java.time.LocalDate): FontChoice {
            val pool = entries.filter { it != SYSTEM && it != CUSTOM }
            // Multiplying by a prime coprime to the pool size walks every font before repeating.
            return pool[Math.floorMod(day.toEpochDay() * 7919L, pool.size.toLong()).toInt()]
        }
    }
}

enum class WeatherSource(val label: String, val summary: String) {
    AUTO("Automatic", "Yr forecast; rain minutes from Yr's radar nowcast, or Open-Meteo outside the Nordics"),
    MET("Yr (MET Norway)", "Forecast and radar nowcast from the Norwegian Meteorological Institute"),
    OPEN_METEO("Open-Meteo", "Forecast with 15-minute precipitation"),
}

enum class WeatherGlyphs(val label: String) {
    LINE("Line icons"),
    KANJI("Kanji"),
    NONE("None"),
}

enum class LocationMode(val label: String) {
    DEVICE("Approximate device location"),
    FIXED("Fixed place"),
}

/** Hours above minutes, as in the sketch, or side by side. */
enum class ClockLayout(val label: String) {
    STACKED("Stacked"),
    INLINE("Inline"),
}

/** The look of the home screen and drawer; all of them take the phone's own colours. */
enum class HomeTheme(val label: String, val summary: String) {
    ORIGINAL("Original", "White outlines on black, as sketched"),
    MATERIAL("Material", "Rounded cards in your wallpaper colours, like the phone's own apps"),
    TOKYO("Tokyo", "Paper and ink, tag chips and fine rules: modern Japanese"),
}

/** What to do about a camera hole or notch while the status bar is hidden. */
enum class CutoutMode(val label: String, val summary: String) {
    AUTO("Automatic", "Moves only what the hole would cover"),
    KEEP_CLEAR("Always keep clear", "Leaves the whole band around the hole empty"),
    FULL("Use the whole screen", "Ignores the hole"),
}

/** The lines of the home screen's column, in the user's order. */
enum class HomeBlock(val label: String, val glyph: String) {
    CLOCK("Clock", "clock"),
    DATE("Day and date", "calendar"),
    WEATHER("Weather", "cloud"),
    BATTERY("Battery warning", "battery-2"),
    SIGNAL("Signal warning", "antenna-bars-2"),
    WORD("Word of the day", "vocabulary"),
    NOTIFICATIONS("Notifications", "bell"),
}

/** Where the clock-and-info column sits; away from the top, new lines push it up into free space. */
enum class InfoAlign(val label: String, val glyph: String) {
    TOP("Top", "layout-align-top"),
    MIDDLE("Middle", "layout-align-middle"),
    BOTTOM("Bottom", "layout-align-bottom"),
}

/** Which home-screen lines carry a small translation underneath, for learning the language. */
enum class LearnLines(val label: String, val summary: String) {
    DATE_WEATHER("Date and weather", "A small translation under the date and the weather"),
    ALL("Every line", "Under the date, weather and notifications"),
    OFF("Off", "Translations only when you hold a line"),
}

/** How strongly the translation lines still show on [today], 0..1: they fade evenly over [Prefs.fadeDays]. */
fun Prefs.longFade(today: Long): Float =
    if (fadeDays <= 0) 1f else (1f - (today - fadeStart).toFloat() / fadeDays).coerceIn(0f, 1f)

/**
 * The daily fade at [minute] after midnight, 0..1: full at [Prefs.dayFadeFrom],
 * evenly fainter until [Prefs.dayFadeTo], then gone until the next day's start.
 * A window may run past midnight (22:00 to 02:00).
 */
fun Prefs.dayFadeAt(minute: Int): Float {
    if (!dayFade) return 1f
    val span = Math.floorMod(dayFadeTo - dayFadeFrom, MINUTES_A_DAY).takeIf { it > 0 } ?: MINUTES_A_DAY
    val elapsed = Math.floorMod(minute - dayFadeFrom, MINUTES_A_DAY)
    return if (elapsed > span) 0f else 1f - elapsed.toFloat() / span
}

/** Both fades together: the daily cycle within the longer one. */
fun Prefs.fade(today: Long, minute: Int): Float = longFade(today) * dayFadeAt(minute)

const val MINUTES_A_DAY = 24 * 60

const val DEFAULT_PREFIX = "//"

/** The long-hold popup's reading of Japanese text. */
enum class ReadingHint(val label: String) {
    KANA("Kana"),
    ROMAJI("Rōmaji"),
    OFF("Off"),
}

enum class NotifStyle(val label: String, val summary: String) {
    SUMMARY("Summary", "\"Message · 5 min ago\" in the home screen language; hold a line for its text"),
    FULL("Title and text", "Each notification's title and text, as the app wrote them"),
    GLYPHS("App glyphs only", "Just the count and which apps"),
}

data class Place(val name: String, val lat: Double, val lon: Double)

/** What a sideways swipe on the home screen does. */
enum class GestureKind(val label: String, val glyph: String) {
    NONE("Nothing", "circle-off"),
    APP("Open an app", "apps"),
    DRAWER("App drawer", "layout-dashboard"),
    SEARCH("Search apps", "search"),
    NOTIFICATIONS("Notifications", "bell"),
    QUICK_SETTINGS("Quick settings", "layout-sidebar-right"),
    WEATHER("Weather app", "cloud"),
    CAMERA("Camera", "camera"),
    TORCH("Torch on/off", "bulb"),
    SETTINGS("Kamei settings", "settings"),
}

/** A gesture's action; [app] only for [GestureKind.APP]. */
data class Gesture(val kind: GestureKind, val app: AppKey? = null) {
    fun encode(): String = if (kind == GestureKind.APP && app != null) "app:${app.encode()}" else kind.name

    companion object {
        val NONE = Gesture(GestureKind.NONE)

        fun decode(s: String?): Gesture {
            if (s.isNullOrBlank()) return NONE
            if (s.startsWith("app:")) return AppKey.decode(s.removePrefix("app:"))?.let { Gesture(GestureKind.APP, it) } ?: NONE
            return runCatching { GestureKind.valueOf(s) }.getOrNull()?.takeIf { it != GestureKind.APP }?.let { Gesture(it) } ?: NONE
        }
    }
}

data class Prefs(
    /** The stack on the right, top to bottom. */
    val favorites: List<AppKey> = emptyList(),
    val hidden: Set<AppKey> = emptySet(),
    val renames: Map<AppKey, String> = emptyMap(),
    /** Per-app icon choice, an [IconRef] string. */
    val customIcons: Map<AppKey, String> = emptyMap(),
    val iconStyle: IconStyle = IconStyle.OBSCURE,
    val glyphSet: GlyphSet = GlyphSet.HINTS,
    /** Changing it deals every app a new unexpected glyph. */
    val glyphSeed: Int = 0,
    /** Glyph line width in Tabler units (its own default is 2). */
    val stroke: Float = 1.25f,
    /** Installed icon pack's package; empty for none. */
    val iconPack: String = "",
    val railIconDp: Int = 40,
    val drawerLayout: DrawerLayout = DrawerLayout.GRID_LABELS,
    val drawerColumns: Int = 4,
    /** Latin app names written as they sound, in the home screen's script (YouTube → ユーチューブ). */
    val phoneticLabels: Boolean = false,
    /** Opening the drawer puts the cursor in search with the keyboard up. */
    val drawerKeyboard: Boolean = false,
    val displayLang: DisplayLang = DisplayLang.JA,
    /** Language code for the long-hold popup, or "system". */
    val translateTo: String = "system",
    val holdMillis: Int = 3000,
    val textFont: FontChoice = FontChoice.MPLUS_CODE,
    val clockFont: FontChoice = FontChoice.INTER,
    val clockOutline: Boolean = true,
    /** A different clock font every day, ignoring [clockFont]. */
    val clockShuffle: Boolean = false,
    /** The picked font file's name, shown in settings ("" when none was ever picked). */
    val customClockFont: String = "",
    val clock24: Boolean = true,
    /** Digit sizes in sp, hours and minutes apart. */
    val hourSize: Int = 112,
    val minuteSize: Int = 112,
    val clockLayout: ClockLayout = ClockLayout.STACKED,
    /** Nudges in dp, each part on its own, without moving anything else. */
    val hourOffsetX: Int = 0,
    val hourOffsetY: Int = 0,
    val minuteOffsetX: Int = 0,
    val minuteOffsetY: Int = 0,
    /** Outline width in dp, for outlined digits. */
    val clockStroke: Float = 1.6f,
    val infoAlign: InfoAlign = InfoAlign.TOP,
    /** Top to bottom; warnings and the word only show when there's something to show. */
    val blockOrder: List<HomeBlock> = HomeBlock.entries,
    /** Written before each home-screen heading, as in the sketch; empty for none. */
    val prefix: String = DEFAULT_PREFIX,
    val weatherOn: Boolean = false,
    val weatherSource: WeatherSource = WeatherSource.AUTO,
    val locationMode: LocationMode = LocationMode.DEVICE,
    val place: Place? = null,
    val fahrenheit: Boolean = false,
    val weatherGlyphs: WeatherGlyphs = WeatherGlyphs.LINE,
    val rainTint: Boolean = true,
    /** How far ahead rain starts turning the weather line blue, in minutes. */
    val rainWindow: Int = 120,
    /** Opened by tapping the weather line: "" picks Yr or Drops if installed, else an [AppKey]. */
    val weatherApp: String = "",
    val notifStyle: NotifStyle = NotifStyle.SUMMARY,
    val theme: HomeTheme = HomeTheme.ORIGINAL,
    val hideStatusBar: Boolean = true,
    val cutoutMode: CutoutMode = CutoutMode.AUTO,
    /** Swipe left / right on the home screen (up is the drawer, down the notifications). */
    val swipeLeft: Gesture = Gesture.NONE,
    val swipeRight: Gesture = Gesture.NONE,
    /** The phone's wallpaper behind the home screen, darkened by [wallpaperDim] percent. */
    val showWallpaper: Boolean = false,
    val wallpaperDim: Int = 55,
    val readingHint: ReadingHint = ReadingHint.KANA,
    val learnLines: LearnLines = LearnLines.DATE_WEATHER,
    /** The reading (kana or rōmaji, as [readingHint]) shown on the home screen too, above the translation. */
    val learnReading: Boolean = false,
    /** A word a day, with its reading and meaning, under the weather. */
    val wordOfDay: Boolean = true,
    /** Warning lines: battery at 25% or less (not while charging), and weak or no mobile signal. */
    val batteryLine: Boolean = true,
    val signalLine: Boolean = true,
    /** Days over which the translation lines fade away (0: they never do), counted from [fadeStart] (an epoch day). */
    val fadeDays: Int = 0,
    val fadeStart: Long = 0,
    /** Daily fade: full at [dayFadeFrom], gone by [dayFadeTo] (minutes after midnight), hidden until the next morning. */
    val dayFade: Boolean = false,
    val dayFadeFrom: Int = 6 * 60,
    val dayFadeTo: Int = 12 * 60,
    val seeded: Boolean = false,
) {
    companion object {
        val DRAWER_COLUMNS = 3..6
        val RAIL_ICON_DP = 28..56
        val CLOCK_SIZES = 40..200
        val CLOCK_OFFSETS = -80..80
        val CLOCK_STROKE = 0.5f..6f
        val HOLD_MILLIS = listOf(1500, 2000, 3000, 4000)
        val RAIN_WINDOWS = listOf(60, 120, 180)
        val STROKE = 0.75f..2f
        val FADE_DAYS = listOf(0, 14, 30, 90)
        const val PREFIX_MAX = 4
    }
}

/** Per-app icon choices, as stored in [Prefs.customIcons]. */
sealed interface IconRef {
    data class Glyph(val name: String) : IconRef
    data class Pack(val pkg: String, val drawable: String) : IconRef
    data class Style(val style: IconStyle) : IconRef

    fun encode(): String = when (this) {
        is Glyph -> "glyph:$name"
        is Pack -> "pack:$pkg:$drawable"
        is Style -> "style:${style.name}"
    }

    companion object {
        fun decode(s: String?): IconRef? {
            if (s == null) return null
            val parts = s.split(':')
            return when (parts.firstOrNull()) {
                "glyph" -> parts.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { Glyph(it) }
                "pack" -> if (parts.size == 3) Pack(parts[1], parts[2]) else null
                "style" -> runCatching { Style(IconStyle.valueOf(parts[1])) }.getOrNull()
                else -> null
            }
        }
    }
}
