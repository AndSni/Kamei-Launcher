package com.asnidev.kamei.lang

/** The JLPT levels the word of the day draws from, easiest first. */
enum class JlptLevel(val label: String, val summary: String) {
    N5("N5", "Beginner: everyday basics"),
    N4("N4", "Elementary: daily life"),
    N3("N3", "Intermediate: news, work, feelings"),
}

/** One word of the day: the Japanese, its kana, and its meaning in each translation language. */
class VocabWord(val level: JlptLevel, val ja: String, val kana: String, private val meanings: Map<String, String>) {
    /** The meaning in [lang]; English where the dictionaries had none. */
    fun word(lang: String): String = meanings[lang]?.takeIf { it.isNotEmpty() } ?: meanings.getValue("en")
}

/**
 * The bundled word list for "word of the day", JLPT N5 to N3, read once from
 * assets/vocab.tsv (built by tools/make_vocab.py from the JLPT lists, JMdict and
 * Wiktionary). Nothing is ever looked up online.
 */
object Vocabulary {

    /** The translation columns after level, ja, kana and en. */
    private val LANGS = listOf("lv", "lt", "et", "ru", "uk", "de", "fr", "es", "pl", "fi", "sv", "zh", "ko", "el")

    @Volatile
    var all: List<VocabWord> = emptyList()
        private set

    /** Levels whose characters tools/make_fonts.py puts in the bundled fonts. */
    val FONT_LEVELS = setOf(JlptLevel.N5, JlptLevel.N4)

    @Volatile
    private var fontChars: Set<Char> = emptySet()

    /** Whether the bundled fonts can write all of [text] (kana always; kanji from the font levels). */
    fun inBundledFonts(text: String): Boolean = text.all { it in '\u3040'..'\u30ff' || it in fontChars }

    /** Reads the list from the file's text (tab-separated, "#" starts a comment line). */
    fun load(tsv: String) {
        all = tsv.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
            val f = line.split('\t')
            val level = JlptLevel.entries.firstOrNull { it.name == f[0] } ?: return@mapNotNull null
            val meanings = mapOf("en" to f[3]) + LANGS.mapIndexed { i, l -> l to f.getOrElse(4 + i) { "" } }
            VocabWord(level, f[1], f[2], meanings)
        }.toList()
        fontChars = all.filter { it.level in FONT_LEVELS }.flatMap { (it.ja + it.kana).toList() }.toSet()
    }

    fun from(levels: Set<JlptLevel>): List<VocabWord> = all.filter { it.level in levels }

    /**
     * The word for turn [slot] (a day, or a span of hours) among [levels]: a fixed
     * scrambled walk through those words (a step coprime to their number), so every
     * word comes up once before any repeats. [extra] moves on to the following
     * words, for a tap asking for another. Null until the list has loaded.
     */
    fun of(slot: Long, levels: Set<JlptLevel>, extra: Int = 0): VocabWord? {
        val words = from(levels).ifEmpty { all }
        val n = words.size
        if (n == 0) return null
        val step = generateSequence(37) { it + 1 }.first { gcd(it, n) == 1 }
        return words[Math.floorMod((slot + extra) * step, n.toLong()).toInt()]
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
