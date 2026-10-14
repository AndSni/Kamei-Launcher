package com.asnidev.kamei.lang

import java.util.concurrent.ConcurrentHashMap

/**
 * Writes Latin app names the way they sound, in the home screen's own script:
 * "YouTube" → ユーチューブ (Japanese), 유튜브 (Korean), Γιουτιουμπ (Greek).
 * Known words come from a dictionary of their usual spelling; the rest go
 * through English sound rules into one sound sequence, which each script then
 * writes by its own conventions. Anything not in Latin letters stays as it is.
 */
object Transcribe {

    /** Whether [lang] has a phonetic script to write names in (Chinese doesn't, in everyday use). */
    fun supports(lang: DisplayLang) = lang != DisplayLang.ZH

    private val cache = ConcurrentHashMap<String, String>()

    fun label(text: String, lang: DisplayLang): String {
        if (!supports(lang)) return text
        return cache.getOrPut("${lang.tag}|$text") {
            val out = WORD.replace(text) { m -> word(m.value, lang) }
            when (lang) {
                // Words one after another read as one name in Japanese: the middle dot keeps them apart.
                DisplayLang.JA -> out.replace(Regex("(?<=[ァ-ヿ]) (?=[ァ-ヿ])"), "・")
                // Greek: a capital at the start of each name, ς only where a word really ends.
                DisplayLang.EL -> out.split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                    .replace(Regex("σ(?!\\p{L})"), "ς")
                else -> out
            }
        }
    }

    /** Latin words, split where CamelCase starts a new one ("YouTube" → You, Tube). */
    private val WORD = Regex("[A-Z]{2,}(?![a-z])|[A-Z]?[a-z]+|[A-Z]")

    private fun word(w: String, lang: DisplayLang): String {
        // Short all-capital words are read letter by letter: "TV", "VLC".
        if (w.length in 1..4 && w.all { it.isUpperCase() }) return letters(w, lang)
        val lower = w.lowercase()
        when (lang) {
            DisplayLang.JA -> JA_WORDS[lower]
            DisplayLang.KO -> KO_WORDS[lower]
            DisplayLang.EL -> EL_WORDS[lower]?.replace('ς', 'σ')
            DisplayLang.ZH -> null
        }?.let { return it }
        val sounds = Sounds.of(english(lower))
        return when (lang) {
            DisplayLang.JA -> Kana.write(sounds)
            DisplayLang.KO -> Hangul.write(sounds)
            DisplayLang.EL -> Greek.write(sounds)
            DisplayLang.ZH -> w
        }
    }

    private fun letters(w: String, lang: DisplayLang): String {
        val names = when (lang) {
            DisplayLang.JA -> JA_LETTERS
            DisplayLang.KO -> KO_LETTERS
            DisplayLang.EL -> EL_LETTERS
            DisplayLang.ZH -> return w
        }
        val out = w.map { names[it - 'A'] }.joinToString("")
        return if (lang == DisplayLang.EL) out.lowercase() else out
    }

    /** Common words English spells nothing like it says. */
    private val IRREGULAR = mapOf("one" to "wan", "you" to "yuu", "your" to "yoo", "two" to "tuu", "who" to "huu", "to" to "tuu", "do" to "duu")

    /**
     * English spelling to a plain sound spelling: doubled vowels are long,
     * doubled consonants are held, "ə" is the "er" sound. Sounds a rule has
     * already worked out are written in capitals, so later rules leave them
     * alone; everything is lower-cased at the end. Rough, as English is, but
     * close to how names get said.
     */
    fun english(word: String): String {
        IRREGULAR[word]?.let { return it }
        val v = "aeiouAEIOUƏ"
        var s = word
        s = s.replace(Regex("^you"), "YUU")
        // Silent letters and whole endings first.
        s = s.replace(Regex("^kn"), "n").replace(Regex("^wr"), "r").replace(Regex("^ps"), "s")
            .replace(Regex("mb$"), "m").replace(Regex("gn$"), "n").replace("chr", "kr").replace("sch", "sk")
        s = s.replace("ation", "EIshon").replace("otion", "OOshon").replace("ution", "UUshon").replace("tion", "shon").replace("sion", "shon").replace("ture", "chƏƏ").replace("sure", "shƏƏ")
            .replace("ough", "OO").replace("augh", "OO").replace("eigh", "EI").replace("igh", "AI")
        // Final y is a vowel of its own: "-ify" → ai, "happy" → ii, "fly" → ai.
        s = s.replace(Regex("fy$"), "fAI")
        s = s.replace(Regex("(?<=[^$v])y$")) { if (Regex("[$v]").containsMatchIn(s.dropLast(1))) "II" else "AI" }
        // A final e after one consonant is silent and lengthens the vowel before it: tube, game, drive, phone.
        s = s.replace(Regex("([aeiou])([^${v}y])e$")) { m ->
            val long = when (m.groupValues[1]) { "a" -> "EI"; "e" -> "II"; "i" -> "AI"; "o" -> "OO"; else -> "YUU" }
            long + m.groupValues[2]
        }
        // "-le" is a lone l: ル in Japanese, ㄹ under the syllable in Korean (킨들), λ in Greek.
        s = s.replace(Regex("([^$v])le$"), "$1l")
        // y between consonants, before a syllable: hyper, type.
        s = s.replace(Regex("(?<=[^$v])y(?=[^$v][$v])"), "AI")
        // Consonant spellings. "ck" is held only where no vowel follows: duck, check, but pocket.
        s = s.replace("ph", "f").replace("th", "s").replace(Regex("ck(?=[$v])"), "k").replace("ck", "kk")
            .replace("qu", "kw").replace("wh", "w").replace("x", "kks").replace("dge", "jj")
        s = s.replace(Regex("c(?=[eiyEIY])"), "s").replace("c", "k")
        s = s.replace(Regex("g(?=[eiy])(?!et|ive|ift|irl)"), "j")
        // Vowel spellings.
        s = s.replace("ee", "II").replace("ea", "II").replace("oo", "UU").replace("oa", "OO")
            .replace("ai", "EI").replace("ay", "EI").replace("ew", "YUU").replace("ou", "AU")
            .replace(Regex("ow$"), "OO").replace("ow", "AU").replace("oy", "OI")
        // The r sounds: "ar" is a long a (sharp, park); "er", "ir", "ur" a long ə; "or" too at the end of a longer word (monitor), else oo.
        s = s.replace(Regex("ar(?![$v])"), "AA")
        s = s.replace(Regex("(er|ir|ur)(?![$v])"), "ƏƏ")
        if (s.endsWith("or") && Regex("[$v]").containsMatchIn(s.dropLast(2))) s = s.dropLast(2) + "ƏƏ"
        s = s.replace(Regex("or(?![$v])"), "OO")
        s = s.replace(Regex("y(?![$v])"), "i")
        // A short vowel and a final stop: the stop is held (organic, chat, pocket).
        s = s.replace(Regex("(?<![$v])([aeiou])([ktpgd])$"), "$1$2$2")
        // Doubled consonants that aren't held.
        s = s.replace(Regex("([lmnrw])\\1"), "$1")
        return s.lowercase()
    }

    // ---- the sound sequence ----

    /** One spoken syllable: [c] is "" for none, [v] a/i/u/e/o/ə, [added] for a vowel only there to carry a lone consonant. */
    data class Syl(val c: String, val v: Char, val long: Boolean = false, val added: Boolean = false, val held: Boolean = false)

    /** A syllable, or a lone n (ン). */
    sealed interface Sound {
        data class S(val syl: Syl) : Sound
        data object N : Sound
    }

    object Sounds {
        private const val VOWELS = "aeiouə"
        private val UNITS = listOf("ch", "sh", "ts", "ky", "gy", "ny", "hy", "by", "py", "my", "ry", "ly", "fy", "vy", "ty", "dy", "sy", "zy", "jy")

        fun of(s: String): List<Sound> {
            val out = ArrayList<Sound>()
            var i = 0
            var held = false
            while (i < s.length) {
                val ch = s[i]
                if (ch in VOWELS) {
                    val long = i + 1 < s.length && s[i + 1] == ch
                    out += Sound.S(Syl("", ch, long, held = held))
                    held = false
                    i += if (long) 2 else 1
                    continue
                }
                if (!ch.isLetter()) { i++; continue }
                // A doubled stop is held (ッ): "app", "settings".
                if (i + 1 < s.length && s[i + 1] == ch && ch in "kgtdpbsfzj") { held = true; i++; continue }
                val unit = UNITS.firstOrNull { s.startsWith(it, i) && i + it.length < s.length && s[i + it.length] in VOWELS }
                    ?: "ts".takeIf { s.startsWith(it, i) && i + 2 == s.length }
                    ?: ch.toString()
                val next = i + unit.length
                if (unit == "n" && (next >= s.length || s[next] !in VOWELS && s[next] != 'y')) {
                    out += Sound.N
                    i = next
                    continue
                }
                if (next < s.length && s[next] in VOWELS) {
                    val v = s[next]
                    val long = next + 1 < s.length && s[next + 1] == v
                    out += Sound.S(Syl(unit, v, long, held = held))
                    held = false
                    i = next + if (long) 2 else 1
                } else {
                    // A consonant with no vowel gets one: o after t and d, i after ch and j at the end, else u.
                    val atEnd = next >= s.length
                    val v = when {
                        unit == "t" || unit == "d" -> 'o'
                        (unit == "ch" || unit == "j") && atEnd -> 'i'
                        else -> 'u'
                    }
                    out += Sound.S(Syl(unit, v, added = true, held = held))
                    held = false
                    i = next
                }
            }
            return out
        }
    }

    // ---- Japanese katakana ----

    private object Kana {
        private val V = "aiueo"
        private val ROWS: Map<String, String> = mapOf(
            "" to "アイウエオ", "k" to "カキクケコ", "g" to "ガギグゲゴ", "s" to "サシスセソ", "z" to "ザジズゼゾ",
            "t" to "タティトゥテト", "d" to "ダディドゥデド", "n" to "ナニヌネノ", "h" to "ハヒフヘホ", "b" to "バビブベボ",
            "p" to "パピプペポ", "m" to "マミムメモ", "y" to "ヤイユイェヨ", "r" to "ラリルレロ", "w" to "ワウィウウェウォ",
            "f" to "ファフィフフェフォ", "v" to "バビブベボ", "j" to "ジャジジュジェジョ", "ch" to "チャチチュチェチョ",
            "sh" to "シャシシュシェショ", "ts" to "ツァツィツツェツォ",
        )

        /** Splits a row string into its five kana (some are two characters: ティ, ファ…). */
        private fun cells(row: String): List<String> {
            val out = ArrayList<String>()
            var i = 0
            while (i < row.length) {
                val small = i + 1 < row.length && row[i + 1] in "ァィゥェォャュョ"
                out += if (small) row.substring(i, i + 2) else row[i].toString()
                i += if (small) 2 else 1
            }
            return out
        }

        private val TABLE = ROWS.mapValues { cells(it.value) }

        fun kana(c: String, v0: Char): String {
            val v = if (v0 == 'ə') 'a' else v0
            val idx = V.indexOf(v)
            if (c.length == 2 && c.endsWith("y") && c !in TABLE) {
                // Glides: キュ, ニュ, チュ (ty), デュ (dy)…
                val base = when (c[0]) { 't' -> "チ"; 'd' -> "デ"; 'l' -> "リ"; else -> TABLE[c[0].toString()]?.get(1) ?: "イ" }
                val small = when (v) { 'a' -> "ャ"; 'u' -> "ュ"; 'o' -> "ョ"; 'e' -> "ェ"; else -> "ィ" }
                return base + small
            }
            val row = TABLE[if (c == "l") "r" else c] ?: TABLE.getValue("")
            return row[idx]
        }

        fun write(sounds: List<Sound>): String = buildString {
            for (s in sounds) when (s) {
                Sound.N -> append('ン')
                is Sound.S -> {
                    if (s.syl.held) append('ッ')
                    append(kana(s.syl.c, s.syl.v))
                    if (s.syl.long) append('ー')
                }
            }
        }
            // Loanwords write these as long vowels: ステーション, ゲーム, ノーション.
            .replace(Regex("([エケゲセゼテデネヘベペメレェ])イ"), "$1ー")
            .replace(Regex("([オコゴソゾトドノホボポモロォ])ウ"), "$1ー")
    }

    // ---- Korean hangul ----

    private object Hangul {
        // Initial consonant indexes in the Unicode hangul block.
        private val INITIAL = mapOf(
            "" to 11, "k" to 15, "g" to 0, "s" to 9, "z" to 12, "t" to 16, "d" to 3, "n" to 2, "h" to 18, "b" to 7,
            "p" to 17, "m" to 6, "r" to 5, "l" to 5, "f" to 17, "v" to 7, "j" to 12, "ch" to 14, "sh" to 9, "ts" to 14, "y" to 11, "w" to 11,
        )
        // Final consonants Korean writes at the bottom of a syllable: 앱, 크롬, 북, 챗.
        private val FINAL = mapOf("k" to 1, "g" to 1, "n" to 4, "l" to 8, "r" to 8, "m" to 16, "p" to 17, "b" to 17, "t" to 19, "d" to 19)
        private const val NG = 21

        private fun vowel(c: String, v: Char, added: Boolean): Int {
            val glide = c.length == 2 && c.endsWith("y") || c == "y" || c == "sh"
            if (added) return 18 // ㅡ, the vowel Korean adds to a lone consonant
            if (c == "w") return when (v) { 'a', 'ə' -> 9; 'i' -> 16; 'e' -> 15; 'o' -> 14; else -> 13 }
            return when (v) {
                'a' -> if (glide) 2 else 0
                'ə' -> if (glide) 6 else 4 // the "er" of keeper, monitor: 퍼, 터
                'e' -> if (glide) 7 else 5
                'o' -> if (glide) 12 else 8
                'u' -> if (glide) 17 else 13
                else -> 20
            }
        }

        private fun block(initial: Int, vowel: Int) = (0xAC00 + (initial * 21 + vowel) * 28).toChar()

        fun write(sounds: List<Sound>): String {
            val out = StringBuilder()
            var open = false // whether the last block can still take a final consonant
            var lastLong = false
            var i = 0
            while (i < sounds.size) {
                val s = sounds[i]
                val next = sounds.getOrNull(i + 1)
                when (s) {
                    Sound.N -> {
                        // "ng" (settings, king) is ㅇ under the block; plain n is ㄴ.
                        val ng = next is Sound.S && next.syl.added && next.syl.c == "g"
                        if (open) withFinal(out, if (ng) NG else 4) else out.append(if (ng) "응" else "ㄴ")
                        open = false
                        if (ng) i++
                    }
                    is Sound.S -> {
                        val syl = s.syl
                        val base = if (syl.c.length == 2 && syl.c.endsWith("y")) syl.c.dropLast(1) else syl.c
                        if (syl.added && open && !lastLong && base in FINAL) {
                            withFinal(out, FINAL.getValue(base))
                            open = false
                        } else {
                            out.append(block(INITIAL[base] ?: 11, vowel(syl.c, syl.v, syl.added)))
                            open = true
                            lastLong = syl.long
                        }
                    }
                }
                i++
            }
            return out.toString()
        }

        private fun withFinal(out: StringBuilder, final: Int) {
            val ch = out.last()
            if (ch.code !in 0xAC00..0xD7A3 || (ch.code - 0xAC00) % 28 != 0) return
            out.setCharAt(out.lastIndex, (ch.code + final).toChar())
        }
    }

    // ---- Greek letters ----

    private object Greek {
        private val CONS = mapOf(
            "" to "", "k" to "κ", "g" to "γκ", "s" to "σ", "z" to "ζ", "t" to "τ", "d" to "ντ", "n" to "ν", "h" to "χ",
            "b" to "μπ", "p" to "π", "m" to "μ", "y" to "γι", "r" to "ρ", "l" to "λ", "w" to "ου", "f" to "φ", "v" to "β",
            "j" to "τζ", "ch" to "τσ", "sh" to "σ", "ts" to "τσ",
        )
        private val VOW = mapOf('a' to "α", 'i' to "ι", 'u' to "ου", 'e' to "ε", 'o' to "ο", 'ə' to "α")

        fun write(sounds: List<Sound>): String {
            val raw = buildString {
                for (s in sounds) when (s) {
                    Sound.N -> append('ν')
                    is Sound.S -> {
                        val c = s.syl.c
                        if (c.length == 2 && c.endsWith("y")) append(CONS[c.dropLast(1)] ?: "").append("ι") else append(CONS[c] ?: c)
                        // Greek writes consonant clusters as they are: no helper vowels. An i right after
                        // a, e or o is ϊ, or "αι" would read as e (Ραϊτ, not Ραιτ).
                        if (!s.syl.added) append(if (s.syl.v == 'i' && c.isEmpty() && length > 0 && last() in "αεο") "ϊ" else VOW[s.syl.v])
                    }
                }
            }
            return raw.replace("νντ", "ντ").replace("μμπ", "μπ")
        }
    }

    // ---- dictionaries ----

    private val JA_LETTERS = listOf(
        "エー", "ビー", "シー", "ディー", "イー", "エフ", "ジー", "エイチ", "アイ", "ジェー", "ケー", "エル", "エム",
        "エヌ", "オー", "ピー", "キュー", "アール", "エス", "ティー", "ユー", "ブイ", "ダブリュー", "エックス", "ワイ", "ゼット",
    )
    private val KO_LETTERS = listOf(
        "에이", "비", "씨", "디", "이", "에프", "지", "에이치", "아이", "제이", "케이", "엘", "엠",
        "엔", "오", "피", "큐", "아르", "에스", "티", "유", "브이", "더블유", "엑스", "와이", "제트",
    )
    private val EL_LETTERS = listOf(
        "Έι", "Μπι", "Σι", "Ντι", "Ι", "Εφ", "Τζι", "Έιτς", "Άι", "Τζέι", "Κέι", "Ελ", "Εμ",
        "Εν", "Όου", "Πι", "Κιου", "Αρ", "Ες", "Τι", "Γιου", "Βι", "Ντάμπλιου", "Εξ", "Γουάι", "Ζεντ",
    )

    /** The usual hangul for the best-known app words (the rules cover the rest). */
    private val KO_WORDS: Map<String, String> = mapOf(
        "you" to "유", "tube" to "튜브", "google" to "구글", "chrome" to "크롬", "gmail" to "지메일", "spotify" to "스포티파이",
        "whats" to "왓츠", "app" to "앱", "telegram" to "텔레그램", "instagram" to "인스타그램", "facebook" to "페이스북",
        "netflix" to "넷플릭스", "maps" to "맵", "map" to "맵", "camera" to "카메라", "calendar" to "캘린더", "settings" to "세팅",
        "phone" to "폰", "messages" to "메시지", "message" to "메시지", "clock" to "클록", "contacts" to "콘택트", "files" to "파일",
        "file" to "파일", "drive" to "드라이브", "photos" to "포토", "photo" to "포토", "music" to "뮤직", "play" to "플레이",
        "store" to "스토어", "one" to "원", "bit" to "비트", "gemini" to "제미나이", "safety" to "세이프티", "glasses" to "글라스",
        "keep" to "킵", "chat" to "챗", "mail" to "메일", "news" to "뉴스", "weather" to "웨더", "notes" to "노트", "gallery" to "갤러리",
        "browser" to "브라우저", "bank" to "뱅크", "pay" to "페이", "home" to "홈", "launcher" to "런처", "zoom" to "줌",
        "discord" to "디스코드", "firefox" to "파이어폭스", "termux" to "터먹스", "wikipedia" to "위키피디아", "himitsu" to "히미츠", "kamei" to "카메이",
    )

    /** The usual Greek spelling of the best-known app words. */
    private val EL_WORDS: Map<String, String> = mapOf(
        "you" to "γιου", "tube" to "τιουμπ", "google" to "γκουγκλ", "chrome" to "κρομ", "gmail" to "τζιμέιλ", "spotify" to "σποτιφάι",
        "whats" to "γουότς", "app" to "απ", "facebook" to "φέισμπουκ", "instagram" to "ινσταγκραμ", "phone" to "φόουν",
        "maps" to "μαπς", "camera" to "κάμερα", "settings" to "σέτινγκς", "music" to "μιούζικ", "play" to "πλέι", "store" to "στορ",
        "one" to "γουάν", "bit" to "μπιτ", "himitsu" to "χιμίτσου", "kamei" to "καμέι",
    )

    /** The usual katakana for words app names are made of. */
    private val JA_WORDS: Map<String, String> = mapOf(
        "you" to "ユー", "tube" to "チューブ", "google" to "グーグル", "chrome" to "クローム", "gmail" to "ジーメール",
        "maps" to "マップ", "map" to "マップ", "photos" to "フォト", "photo" to "フォト", "camera" to "カメラ",
        "settings" to "セッティング", "phone" to "フォン", "messages" to "メッセージ", "message" to "メッセージ",
        "messenger" to "メッセンジャー", "calendar" to "カレンダー", "clock" to "クロック", "contacts" to "コンタクト",
        "files" to "ファイル", "file" to "ファイル", "drive" to "ドライブ", "gemini" to "ジェミニ", "music" to "ミュージック",
        "play" to "プレイ", "store" to "ストア", "one" to "ワン", "bit" to "ビット", "safety" to "セーフティ",
        "glasses" to "グラス", "shizuku" to "シズク", "whats" to "ワッツ", "app" to "アップ", "telegram" to "テレグラム",
        "signal" to "シグナル", "spotify" to "スポティファイ", "netflix" to "ネットフリックス", "instagram" to "インスタグラム",
        "facebook" to "フェイスブック", "twitter" to "ツイッター", "keep" to "キープ", "docs" to "ドキュメント",
        "sheets" to "シート", "slides" to "スライド", "meet" to "ミート", "chat" to "チャット", "mail" to "メール",
        "news" to "ニュース", "weather" to "ウェザー", "notes" to "ノート", "note" to "ノート", "calculator" to "カリキュレーター",
        "recorder" to "レコーダー", "gallery" to "ギャラリー", "browser" to "ブラウザ", "firefox" to "ファイアフォックス",
        "brave" to "ブレイブ", "duck" to "ダック", "go" to "ゴー", "bank" to "バンク", "pay" to "ペイ", "wallet" to "ウォレット",
        "home" to "ホーム", "mobile" to "モバイル", "launcher" to "ランチャー", "read" to "リード", "readout" to "リードアウト",
        "sys" to "シス", "hyper" to "ハイパー", "discord" to "ディスコード", "slack" to "スラック", "zoom" to "ズーム",
        "uber" to "ウーバー", "bolt" to "ボルト", "wolt" to "ウォルト", "amazon" to "アマゾン", "ebay" to "イーベイ",
        "paypal" to "ペイパル", "revolut" to "リボルート", "strava" to "ストラバ", "garmin" to "ガーミン", "connect" to "コネクト",
        "health" to "ヘルス", "fit" to "フィット", "lens" to "レンズ", "translate" to "トランスレート", "assistant" to "アシスタント",
        "podcasts" to "ポッドキャスト", "books" to "ブックス", "book" to "ブック", "games" to "ゲーム", "game" to "ゲーム",
        "movies" to "ムービー", "radio" to "ラジオ", "video" to "ビデオ", "player" to "プレイヤー", "editor" to "エディター",
        "scanner" to "スキャナー", "manager" to "マネージャー", "cloud" to "クラウド", "proton" to "プロトン",
        "element" to "エレメント", "droid" to "ドロイド", "aurora" to "オーロラ", "termux" to "ターマックス",
        "arcticons" to "アークティコンズ", "life" to "ライフ", "smart" to "スマート", "watch" to "ウォッチ",
        "photoshop" to "フォトショップ", "office" to "オフィス", "word" to "ワード", "excel" to "エクセル", "outlook" to "アウトルック",
        "teams" to "チームズ", "skype" to "スカイプ", "line" to "ライン", "viber" to "バイバー", "tik" to "ティック", "tok" to "トック",
        "snapchat" to "スナップチャット", "reddit" to "レディット", "pinterest" to "ピンタレスト", "linkedin" to "リンクトイン",
        "github" to "ギットハブ", "authenticator" to "オーセンティケーター", "password" to "パスワード", "vault" to "ボールト",
        "tasks" to "タスク", "todo" to "トゥードゥー", "reminder" to "リマインダー", "alarm" to "アラーム", "timer" to "タイマー",
        "flash" to "フラッシュ", "light" to "ライト", "torch" to "トーチ", "voice" to "ボイス", "dialer" to "ダイヤラー",
        "sms" to "エスエムエス", "wifi" to "ワイファイ", "bluetooth" to "ブルートゥース", "battery" to "バッテリー",
        "sound" to "サウンド", "studio" to "スタジオ", "live" to "ライブ", "stream" to "ストリーム", "tv" to "ティーヴィー",
        "shop" to "ショップ", "market" to "マーケット", "express" to "エクスプレス", "food" to "フード", "delivery" to "デリバリー",
        "taxi" to "タクシー", "train" to "トレイン", "bus" to "バス", "travel" to "トラベル", "booking" to "ブッキング",
        "air" to "エア", "bnb" to "ビーエヌビー", "dictionary" to "ディクショナリー",
        "learn" to "ラーン", "duolingo" to "デュオリンゴ", "kindle" to "キンドル", "audible" to "オーディブル",
        "tmobile" to "ティーモバイル", "himitsu" to "ヒミツ", "kamei" to "カメイ", "bitwarden" to "ビットワーデン", "nextcloud" to "ネクストクラウド",
        "tidal" to "タイダル", "notion" to "ノーション", "obsidian" to "オブシディアン", "joplin" to "ジョプリン",
    )
}
