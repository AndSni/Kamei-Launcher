package com.asnidev.kamei.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Prefs as JSON. Reading is forgiving: an unknown or missing field keeps its
 * default, so old files keep working as fields are added.
 */
object PrefsJson {

    fun write(p: Prefs): String = JSONObject().apply {
        put("favorites", JSONArray().apply { p.favorites.forEach { put(it.encode()) } })
        put("hidden", JSONArray().apply { p.hidden.forEach { put(it.encode()) } })
        put("renames", keyed(p.renames))
        put("customIcons", keyed(p.customIcons))
        put("iconStyle", p.iconStyle.name)
        put("glyphSet", p.glyphSet.name)
        put("glyphSeed", p.glyphSeed)
        put("stroke", p.stroke.toDouble())
        put("iconPack", p.iconPack)
        put("railIconDp", p.railIconDp)
        put("drawerLayout", p.drawerLayout.name)
        put("drawerColumns", p.drawerColumns)
        put("drawerKeyboard", p.drawerKeyboard)
        put("phoneticLabels", p.phoneticLabels)
        put("displayLang", p.displayLang.name)
        put("translateTo", p.translateTo)
        put("holdMillis", p.holdMillis)
        put("textFont", p.textFont.name)
        put("clockFont", p.clockFont.name)
        put("clockOutline", p.clockOutline)
        put("clockShuffle", p.clockShuffle)
        put("customClockFont", p.customClockFont)
        put("clock24", p.clock24)
        put("hourSize", p.hourSize)
        put("minuteSize", p.minuteSize)
        put("clockLayout", p.clockLayout.name)
        put("hourOffsetX", p.hourOffsetX)
        put("hourOffsetY", p.hourOffsetY)
        put("minuteOffsetX", p.minuteOffsetX)
        put("minuteOffsetY", p.minuteOffsetY)
        put("clockStroke", p.clockStroke.toDouble())
        put("infoAlign", p.infoAlign.name)
        put("blockOrder", JSONArray().apply { p.blockOrder.forEach { put(it.name) } })
        put("prefix", p.prefix)
        put("weatherOn", p.weatherOn)
        put("weatherSource", p.weatherSource.name)
        put("locationMode", p.locationMode.name)
        p.place?.let { put("place", JSONObject().put("name", it.name).put("lat", it.lat).put("lon", it.lon)) }
        put("fahrenheit", p.fahrenheit)
        put("weatherGlyphs", p.weatherGlyphs.name)
        put("rainTint", p.rainTint)
        put("rainWindow", p.rainWindow)
        put("weatherApp", p.weatherApp)
        put("notifView", p.notifStyle.name)
        put("theme", p.theme.name)
        put("hideStatusBar", p.hideStatusBar)
        put("cutoutMode", p.cutoutMode.name)
        put("swipeLeft", p.swipeLeft.encode())
        put("swipeRight", p.swipeRight.encode())
        put("showWallpaper", p.showWallpaper)
        put("wallpaperDim", p.wallpaperDim)
        put("readingHint", p.readingHint.name)
        put("learnLines", p.learnLines.name)
        put("learnReading", p.learnReading)
        put("wordOfDay", p.wordOfDay)
        put("batteryLine", p.batteryLine)
        put("signalLine", p.signalLine)
        put("fadeDays", p.fadeDays)
        put("fadeStart", p.fadeStart)
        put("dayFade", p.dayFade)
        put("dayFadeFrom", p.dayFadeFrom)
        put("dayFadeTo", p.dayFadeTo)
        put("seeded", p.seeded)
    }.toString()

    fun read(json: String?): Prefs {
        val d = Prefs()
        if (json.isNullOrBlank()) return d
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return d
        return Prefs(
            favorites = o.optJSONArray("favorites")?.let { a ->
                (0 until a.length()).mapNotNull { AppKey.decode(a.optString(it)) }.distinct()
            } ?: d.favorites,
            hidden = o.optJSONArray("hidden")?.let { a ->
                (0 until a.length()).mapNotNull { AppKey.decode(a.optString(it)) }.toSet()
            } ?: d.hidden,
            renames = o.optJSONObject("renames")?.let(::unkeyed) ?: d.renames,
            customIcons = o.optJSONObject("customIcons")?.let(::unkeyed) ?: d.customIcons,
            iconStyle = enumOr(o.optString("iconStyle"), d.iconStyle),
            glyphSet = enumOr(o.optString("glyphSet"), d.glyphSet),
            glyphSeed = o.optInt("glyphSeed", d.glyphSeed),
            stroke = o.optDouble("stroke", d.stroke.toDouble()).toFloat().coerceIn(Prefs.STROKE),
            iconPack = o.optString("iconPack", d.iconPack),
            railIconDp = o.optInt("railIconDp", d.railIconDp).coerceIn(Prefs.RAIL_ICON_DP),
            drawerLayout = enumOr(o.optString("drawerLayout"), d.drawerLayout),
            drawerColumns = o.optInt("drawerColumns", d.drawerColumns).coerceIn(Prefs.DRAWER_COLUMNS),
            drawerKeyboard = o.optBoolean("drawerKeyboard", d.drawerKeyboard),
            phoneticLabels = o.optBoolean("phoneticLabels", d.phoneticLabels),
            displayLang = enumOr(o.optString("displayLang"), d.displayLang),
            translateTo = o.optString("translateTo", d.translateTo).ifBlank { d.translateTo },
            holdMillis = o.optInt("holdMillis", d.holdMillis).takeIf { it in Prefs.HOLD_MILLIS } ?: d.holdMillis,
            textFont = enumOr(o.optString("textFont"), d.textFont),
            clockFont = enumOr(o.optString("clockFont"), d.clockFont),
            clockOutline = o.optBoolean("clockOutline", d.clockOutline),
            clockShuffle = o.optBoolean("clockShuffle", d.clockShuffle),
            customClockFont = o.optString("customClockFont", d.customClockFont),
            clock24 = o.optBoolean("clock24", d.clock24),
            // Before separate sizes there was one "clockSize" for both.
            hourSize = o.optInt("hourSize", o.optInt("clockSize", d.hourSize)).coerceIn(Prefs.CLOCK_SIZES),
            minuteSize = o.optInt("minuteSize", o.optInt("clockSize", d.minuteSize)).coerceIn(Prefs.CLOCK_SIZES),
            clockLayout = enumOr(o.optString("clockLayout"), d.clockLayout),
            hourOffsetX = o.optInt("hourOffsetX", d.hourOffsetX).coerceIn(Prefs.CLOCK_OFFSETS),
            hourOffsetY = o.optInt("hourOffsetY", d.hourOffsetY).coerceIn(Prefs.CLOCK_OFFSETS),
            minuteOffsetX = o.optInt("minuteOffsetX", d.minuteOffsetX).coerceIn(Prefs.CLOCK_OFFSETS),
            minuteOffsetY = o.optInt("minuteOffsetY", d.minuteOffsetY).coerceIn(Prefs.CLOCK_OFFSETS),
            clockStroke = o.optDouble("clockStroke", d.clockStroke.toDouble()).toFloat().coerceIn(Prefs.CLOCK_STROKE),
            infoAlign = enumOr(o.optString("infoAlign"), d.infoAlign),
            blockOrder = o.optJSONArray("blockOrder")?.let { a ->
                // Known names in the saved order, then any block added since, in its default place at the end.
                val saved = (0 until a.length()).mapNotNull { i -> runCatching { HomeBlock.valueOf(a.optString(i)) }.getOrNull() }.distinct()
                saved + HomeBlock.entries.filter { it !in saved }
            } ?: d.blockOrder,
            prefix = if (o.has("prefix")) o.optString("prefix").take(Prefs.PREFIX_MAX) else d.prefix,
            weatherOn = o.optBoolean("weatherOn", d.weatherOn),
            weatherSource = enumOr(o.optString("weatherSource"), d.weatherSource),
            locationMode = enumOr(o.optString("locationMode"), d.locationMode),
            place = o.optJSONObject("place")?.let { p ->
                val lat = p.optDouble("lat", Double.NaN)
                val lon = p.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) null else Place(p.optString("name"), lat, lon)
            } ?: d.place,
            fahrenheit = o.optBoolean("fahrenheit", d.fahrenheit),
            weatherGlyphs = enumOr(o.optString("weatherGlyphs"), d.weatherGlyphs),
            rainTint = o.optBoolean("rainTint", d.rainTint),
            rainWindow = o.optInt("rainWindow", d.rainWindow).takeIf { it in Prefs.RAIN_WINDOWS } ?: d.rainWindow,
            weatherApp = o.optString("weatherApp", d.weatherApp),
            notifStyle = enumOr(o.optString("notifView"), d.notifStyle),
            theme = enumOr(o.optString("theme"), d.theme),
            hideStatusBar = o.optBoolean("hideStatusBar", d.hideStatusBar),
            cutoutMode = enumOr(o.optString("cutoutMode"), d.cutoutMode),
            swipeLeft = if (o.has("swipeLeft")) Gesture.decode(o.optString("swipeLeft")) else d.swipeLeft,
            swipeRight = if (o.has("swipeRight")) Gesture.decode(o.optString("swipeRight")) else d.swipeRight,
            showWallpaper = o.optBoolean("showWallpaper", d.showWallpaper),
            wallpaperDim = o.optInt("wallpaperDim", d.wallpaperDim).coerceIn(0, 95),
            readingHint = enumOr(o.optString("readingHint"), d.readingHint),
            learnLines = enumOr(o.optString("learnLines"), d.learnLines),
            learnReading = o.optBoolean("learnReading", d.learnReading),
            wordOfDay = o.optBoolean("wordOfDay", d.wordOfDay),
            batteryLine = o.optBoolean("batteryLine", d.batteryLine),
            signalLine = o.optBoolean("signalLine", d.signalLine),
            fadeDays = o.optInt("fadeDays", d.fadeDays).takeIf { it in Prefs.FADE_DAYS } ?: d.fadeDays,
            fadeStart = o.optLong("fadeStart", d.fadeStart),
            dayFade = o.optBoolean("dayFade", d.dayFade),
            dayFadeFrom = Math.floorMod(o.optInt("dayFadeFrom", d.dayFadeFrom), MINUTES_A_DAY),
            dayFadeTo = Math.floorMod(o.optInt("dayFadeTo", d.dayFadeTo), MINUTES_A_DAY),
            seeded = o.optBoolean("seeded", d.seeded),
        )
    }

    private fun keyed(m: Map<AppKey, String>) = JSONObject().apply { m.forEach { (k, v) -> put(k.encode(), v) } }

    private fun unkeyed(m: JSONObject): Map<AppKey, String> =
        m.keys().asSequence().mapNotNull { k -> AppKey.decode(k)?.let { it to m.optString(k) } }
            .filter { it.second.isNotBlank() }.toMap()

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
}
