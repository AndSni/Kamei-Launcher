package com.asnidev.kamei.lang

import java.util.concurrent.ConcurrentHashMap

/**
 * Writes Latin app names in katakana, the way they sound: "YouTube" → ユーチューブ.
 * Known words come from a dictionary of their usual spelling; the rest go
 * through English sound rules into one sound sequence, which kana then writes.
 * Anything not in Latin letters stays as it is.
 */
object Transcribe {

    private val cache = ConcurrentHashMap<String, String>()

    fun label(text: String): String = cache.getOrPut(text) {
        // Words one after another read as one name in Japanese: the middle dot keeps them apart.
        WORD.replace(text) { m -> word(m.value) }.replace(Regex("(?<=[ァ-ヿ]) (?=[ァ-ヿ])"), "・")
    }

    /** Latin words, split where CamelCase starts a new one ("YouTube" → You, Tube). */
    private val WORD = Regex("[A-Z]{2,}(?![a-z])|[A-Z]?[a-z]+|[A-Z]")

    private fun word(w: String): String {
        // Short all-capital words are read letter by letter: "TV", "VLC".
        if (w.length in 1..4 && w.all { it.isUpperCase() }) return w.map { JA_LETTERS[it - 'A'] }.joinToString("")
        val lower = w.lowercase()
        return JA_WORDS[lower] ?: Kana.write(Sounds.of(english(lower)))
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
        // "-le" is a lone l: ル (キンドル).
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

    /** One spoken syllable: [c] is "" for none, [v] a/i/u/e/o/ə; [held] doubles the consonant (ッ). */
    data class Syl(val c: String, val v: Char, val long: Boolean = false, val held: Boolean = false)

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
                    out += Sound.S(Syl(unit, v, held = held))
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

    // ---- dictionaries ----

    private val JA_LETTERS = listOf(
        "エー", "ビー", "シー", "ディー", "イー", "エフ", "ジー", "エイチ", "アイ", "ジェー", "ケー", "エル", "エム",
        "エヌ", "オー", "ピー", "キュー", "アール", "エス", "ティー", "ユー", "ブイ", "ダブリュー", "エックス", "ワイ", "ゼット",
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
