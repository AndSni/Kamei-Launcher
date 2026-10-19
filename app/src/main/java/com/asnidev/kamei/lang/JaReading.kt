package com.asnidev.kamei.lang

import com.asnidev.kamei.data.ReadingHint
import java.time.DayOfWeek

/**
 * Readings (furigana) for the Japanese the home screen writes, as kana with
 * spaces between words, and as rōmaji. Numbers follow the counter they're
 * read with: 1分 いっぷん, 3分 さんぷん, 10分 じゅっぷん, 14日 じゅうよっか.
 */
object JaReading {

    /** Each [Phrase]'s reading; {n:fun} {n:ken} {n:jikan} read the number with that counter, {t} is a time. */
    private val phrases: Map<Phrase, String> = mapOf(
        Phrase.NOTIFICATIONS to "つうち",
        Phrase.NO_NOTIFICATIONS to "あたらしい つうちは ありません",
        Phrase.N_NOTIFICATIONS to "つうち {n:ken}",
        Phrase.ALLOW_NOTIFICATIONS to "タップして つうちへの アクセスを きょか",
        Phrase.WEATHER to "てんき",
        Phrase.SET_UP_WEATHER to "タップして てんきを せってい",
        Phrase.UPDATING to "こうしんちゅう",
        Phrase.NO_LOCATION to "いち じょうほう なし",
        Phrase.NO_DATA to "てんき データ なし",
        Phrase.CLEAR to "はれ",
        Phrase.PARTLY_CLOUDY to "はれ ときどき くもり",
        Phrase.CLOUDY to "くもり",
        Phrase.FOG to "きり",
        Phrase.DRIZZLE to "きりさめ",
        Phrase.RAIN to "あめ",
        Phrase.HEAVY_RAIN to "おおあめ",
        Phrase.SHOWERS to "にわかあめ",
        Phrase.SLEET to "みぞれ",
        Phrase.SNOW to "ゆき",
        Phrase.THUNDER to "らいう",
        Phrase.RAIN_IN to "あと {n:fun}で あめ",
        Phrase.RAIN_NOW to "あめが ふって います",
        Phrase.RAIN_STOPS_IN to "あと {n:fun}で あめが やみます",
        Phrase.SNOW_IN to "あと {n:fun}で ゆき",
        Phrase.SNOW_NOW to "ゆきが ふって います",
        Phrase.SNOW_STOPS_IN to "あと {n:fun}で ゆきが やみます",
        Phrase.RAIN_AT to "{t} ごろに あめ",
        Phrase.KIND_MESSAGE to "メッセージ",
        Phrase.KIND_EMAIL to "メール",
        Phrase.KIND_CALL to "でんわ",
        Phrase.KIND_EVENT to "よてい",
        Phrase.KIND_REMINDER to "リマインダー",
        Phrase.KIND_ALARM to "アラーム",
        Phrase.KIND_SOCIAL to "エスエヌエス",
        Phrase.KIND_PROMO to "こうこく",
        Phrase.KIND_SYSTEM to "システム",
        Phrase.KIND_OTHER to "おしらせ",
        Phrase.JUST_NOW to "たったいま",
        Phrase.MIN_AGO to "{n:fun} まえ",
        Phrase.HOURS_AGO to "{n:jikan} まえ",
        Phrase.N_MORE to "ほか {n:ken}",
        Phrase.WORD_OF_DAY to "きょうの ことば",
        Phrase.BATTERY to "バッテリー {n:pct}",
        Phrase.SIGNAL_WEAK to "でんぱが よわい",
        Phrase.SIGNAL_VERY_WEAK to "でんぱが とても よわい",
        Phrase.NO_SERVICE to "けんがい",
    )

    fun has(p: Phrase) = p in phrases

    /** [t] is a time written "13:00". */
    fun say(p: Phrase, n: Int? = null, t: String? = null): String {
        var r = phrases[p] ?: return ""
        if (n != null) {
            r = r.replace("{n:pct}", counter(n, "pct")).replace("{n:fun}", counter(n, "fun")).replace("{n:ken}", counter(n, "ken")).replace("{n:jikan}", counter(n, "jikan"))
        }
        if (t != null) {
            val (h, m) = t.split(':').map { it.toIntOrNull() ?: 0 } + listOf(0, 0)
            r = r.replace("{t}", time(h, m))
        }
        return r
    }

    private val ONES = listOf("", "いち", "に", "さん", "よん", "ご", "ろく", "なな", "はち", "きゅう")

    /** 0..9999 in kana. */
    fun number(n: Int): String {
        if (n == 0) return "ゼロ"
        if (n < 0) return "マイナス " + number(-n)
        val sb = StringBuilder()
        val th = n / 1000 % 10
        val hu = n / 100 % 10
        val te = n / 10 % 10
        val on = n % 10
        when (th) {
            0 -> Unit
            1 -> sb.append("せん")
            3 -> sb.append("さんぜん")
            8 -> sb.append("はっせん")
            else -> sb.append(ONES[th]).append("せん")
        }
        when (hu) {
            0 -> Unit
            1 -> sb.append("ひゃく")
            3 -> sb.append("さんびゃく")
            6 -> sb.append("ろっぴゃく")
            8 -> sb.append("はっぴゃく")
            else -> sb.append(ONES[hu]).append("ひゃく")
        }
        when (te) {
            0 -> Unit
            1 -> sb.append("じゅう")
            else -> sb.append(ONES[te]).append("じゅう")
        }
        sb.append(ONES[on])
        return sb.toString()
    }

    /** [n] with a counter: "fun" 分, "ken" 件, "jikan" 時間, "ji" 時 (o'clock), "do" 度, "pct" %. */
    fun counter(n: Int, c: String): String {
        val s = number(n)
        fun swap(end: String, with: String) = if (s.endsWith(end)) s.dropLast(end.length) + with else null
        return when (c) {
            "fun" -> swap("いち", "いっぷん") ?: swap("ろく", "ろっぷん") ?: swap("はち", "はっぷん") ?: swap("じゅう", "じゅっぷん")
                ?: swap("ひゃく", "ひゃっぷん") ?: if (s.endsWith("さん") || s.endsWith("よん") || s.endsWith("せん")) s + "ぷん" else s + "ふん"
            "ken" -> swap("いち", "いっけん") ?: swap("ろく", "ろっけん") ?: swap("はち", "はっけん") ?: swap("じゅう", "じゅっけん")
                ?: swap("ひゃく", "ひゃっけん") ?: (s + "けん")
            "jikan" -> swap("よん", "よじかん") ?: swap("きゅう", "くじかん") ?: (s + "じかん")
            "ji" -> if (n == 0) "れいじ" else swap("よん", "よじ") ?: swap("なな", "しちじ") ?: swap("きゅう", "くじ") ?: (s + "じ")
            "do" -> s + "ど"
            "pct" -> swap("じゅう", "じゅっパーセント") ?: (s + "パーセント")
            else -> s
        }
    }

    /** 13:05 → じゅうさんじ ごふん; on the hour just the hour. */
    fun time(h: Int, m: Int): String = if (m == 0) counter(h, "ji") else "${counter(h, "ji")} ${counter(m, "fun")}"

    fun weekday(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "げつようび"
        DayOfWeek.TUESDAY -> "かようび"
        DayOfWeek.WEDNESDAY -> "すいようび"
        DayOfWeek.THURSDAY -> "もくようび"
        DayOfWeek.FRIDAY -> "きんようび"
        DayOfWeek.SATURDAY -> "どようび"
        DayOfWeek.SUNDAY -> "にちようび"
    }

    fun date(month: Int, day: Int): String {
        val m = when (month) {
            4 -> "しがつ"
            7 -> "しちがつ"
            9 -> "くがつ"
            else -> number(month) + "がつ"
        }
        val d = DAYS[day] ?: when (day % 10) {
            7 -> number(day - 7) + "しちにち"
            9 -> number(day - 9) + "くにち"
            else -> number(day) + "にち"
        }
        return "$m $d"
    }

    private val DAYS = mapOf(
        1 to "ついたち", 2 to "ふつか", 3 to "みっか", 4 to "よっか", 5 to "いつか", 6 to "むいか", 7 to "なのか",
        8 to "ようか", 9 to "ここのか", 10 to "とおか", 14 to "じゅうよっか", 20 to "はつか", 24 to "にじゅうよっか",
    )

    /** "6°C" → ろくど (the scale isn't read aloud); zero degrees is れいど, as a forecaster says it. */
    fun temp(degrees: Int): String = if (degrees == 0) "れいど" else counter(degrees, "do")

    // ---- rōmaji (Hepburn) ----

    private val KANA: Map<String, String> = buildMap {
        val rows = listOf(
            "あいうえお" to listOf("a", "i", "u", "e", "o"),
            "かきくけこ" to listOf("ka", "ki", "ku", "ke", "ko"),
            "がぎぐげご" to listOf("ga", "gi", "gu", "ge", "go"),
            "さしすせそ" to listOf("sa", "shi", "su", "se", "so"),
            "ざじずぜぞ" to listOf("za", "ji", "zu", "ze", "zo"),
            "たちつてと" to listOf("ta", "chi", "tsu", "te", "to"),
            "だぢづでど" to listOf("da", "ji", "zu", "de", "do"),
            "なにぬねの" to listOf("na", "ni", "nu", "ne", "no"),
            "はひふへほ" to listOf("ha", "hi", "fu", "he", "ho"),
            "ばびぶべぼ" to listOf("ba", "bi", "bu", "be", "bo"),
            "ぱぴぷぺぽ" to listOf("pa", "pi", "pu", "pe", "po"),
            "まみむめも" to listOf("ma", "mi", "mu", "me", "mo"),
            "らりるれろ" to listOf("ra", "ri", "ru", "re", "ro"),
        )
        for ((kana, roma) in rows) kana.forEachIndexed { i, c -> put(c.toString(), roma[i]) }
        putAll(mapOf("や" to "ya", "ゆ" to "yu", "よ" to "yo", "わ" to "wa", "を" to "o", "ん" to "n", "ぁ" to "a", "ぃ" to "i", "ぅ" to "u", "ぇ" to "e", "ぉ" to "o", "ゔ" to "vu"))
        // Digraphs: き+ゃ → kya, し+ゃ → sha…
        val small = mapOf('ゃ' to "a", 'ゅ' to "u", 'ょ' to "o")
        for (c in "きぎしじちぢにひびぴみり") for ((s, v) in small) {
            val base = getValue(c.toString())
            val stem = when (base) { "shi" -> "sh"; "chi" -> "ch"; "ji" -> "j"; else -> base.dropLast(1) + "y" }
            put("$c$s", stem + v)
        }
        // Katakana loanword sounds.
        putAll(mapOf("てぃ" to "ti", "でぃ" to "di", "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo", "うぃ" to "wi", "うぇ" to "we", "しぇ" to "she", "じぇ" to "je", "ちぇ" to "che"))
    }

    /** A reading as [hint] asks: the kana, its rōmaji, both (kana [sep] rōmaji), or nothing. */
    fun hint(kana: String, hint: ReadingHint, sep: String = " · "): String? = when (hint) {
        ReadingHint.KANA -> kana
        ReadingHint.ROMAJI -> romaji(kana)
        ReadingHint.KANA_ROMAJI -> kana + sep + romaji(kana)
        ReadingHint.OFF -> null
    }

    fun romaji(kana: String): String {
        // Katakana to hiragana first; ー lengthens the vowel before it.
        val hira = kana.map { c -> if (c in 'ァ'..'ヶ') c - 0x60 else c }.joinToString("")
        val out = StringBuilder()
        var i = 0
        var double = false
        while (i < hira.length) {
            val c = hira[i]
            when {
                c == 'っ' -> { double = true; i++; continue }
                c == 'ー' -> { out.lastOrNull { it in "aeiou" }?.let { out.append(it) }; i++; continue }
            }
            val two = if (i + 1 < hira.length) KANA[hira.substring(i, i + 2)] else null
            val roma = two ?: KANA[c.toString()]
            if (roma == null) { out.append(c); double = false; i++; continue }
            if (double) { out.append(if (roma.startsWith("ch")) 't' else roma[0]); double = false }
            // ん before a vowel or y is written n' so "kin'en" isn't "kinen".
            if (out.endsWith("n") && hira.getOrNull(i - 1) == 'ん' && roma[0] in "aeiouy") out.append('\'')
            out.append(roma)
            i += if (two != null) 2 else 1
        }
        return out.toString()
            .replace("ou", "ō").replace("oo", "ō").replace("uu", "ū").replace("aa", "ā").replace("ee", "ē")
    }
}
