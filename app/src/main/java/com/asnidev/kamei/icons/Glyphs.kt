package com.asnidev.kamei.icons

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.GlyphSet
import java.util.concurrent.ConcurrentHashMap

/** One line icon from the bundled library, on a 24×24 grid. */
class Glyph(val name: String, val category: String, val tags: String, val paths: List<String>) {
    /** Lower-case text a search matches against. */
    val haystack: String = "$name ${name.replace('-', ' ')} $tags ${category.lowercase()}".lowercase()
}

/**
 * The Tabler Icons library (MIT, ~4,900 outline icons) from assets/glyphs/tabler.tsv,
 * made by tools/make_glyphs.py. Loaded once, off the main thread.
 */
class Glyphs(private val context: Context) {

    @Volatile
    private var loaded: List<Glyph>? = null
    private var byName: Map<String, Glyph> = emptyMap()
    private val vectors = ConcurrentHashMap<String, ImageVector>()
    private val pools = ConcurrentHashMap<GlyphSet, List<Glyph>>()

    /** Every glyph, by name. Blocks on first use: call [load] from a background thread early. */
    fun all(): List<Glyph> = loaded ?: load()

    @Synchronized
    fun load(): List<Glyph> {
        loaded?.let { return it }
        val list = runCatching {
            context.assets.open("glyphs/tabler.tsv").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val f = line.split('\t')
                    if (f.size < 4) null else Glyph(f[0], f[1], f[2], f.subList(3, f.size))
                }.toList()
            }
        }.onFailure { Log.e("Glyphs", "couldn't read the glyph library", it) }.getOrDefault(emptyList())
        byName = list.associateBy { it.name }
        loaded = list
        return list
    }

    fun get(name: String): Glyph? {
        all()
        return byName[name]
    }

    fun search(query: String): List<Glyph> {
        val words = query.lowercase().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return all()
        return all().filter { g -> words.all { it in g.haystack } }
            // Name matches first, then tag matches.
            .sortedBy { g -> if (words.all { it in g.name }) 0 else 1 }
    }

    /**
     * The glyph as a vector in white (tint it when drawing). [stroke] is the line
     * width in grid units; Tabler draws at 2, thinner reads as more refined.
     */
    fun vector(name: String, stroke: Float): ImageVector? {
        val key = "$name@$stroke"
        vectors[key]?.let { return it }
        val glyph = get(name) ?: return null
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (p in glyph.paths) {
            val filled = p.startsWith("F")
            val nodes = runCatching { PathParser().parsePathString(if (filled) p.substring(1) else p).toNodes() }.getOrNull() ?: continue
            if (filled) {
                builder.addPath(nodes, fill = SolidColor(Color.White))
            } else {
                builder.addPath(
                    nodes,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = stroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }
        return builder.build().also { vectors[key] = it }
    }

    /** Glyphs [set] deals out: no crossed-out "-off" variants, nothing that names an app. */
    fun pool(set: GlyphSet): List<Glyph> = pools.getOrPut(set) {
        all().filter { it.category in set.categories && !it.name.endsWith("-off") && !it.name.startsWith("brand-") }
    }

    /**
     * Deals every app in [keys] an unexpected glyph from [set]: the same app keeps
     * the same glyph for the same [seed], and no two apps share one while the pool
     * lasts. Apps are placed in order of their own hash, so installing an app only
     * moves the apps it collides with, not everyone. With [GlyphSet.HINTS], an app
     * first tries the hint glyphs [hints] gives for it (in a seeded order).
     */
    fun deal(keys: Collection<AppKey>, set: GlyphSet, seed: Int, hints: (AppKey) -> List<String>? = { null }): Map<AppKey, String> {
        val pool = pool(set)
        if (pool.isEmpty()) return emptyMap()
        val index = pool.withIndex().associate { it.value.name to it.index }
        val taken = BooleanArray(pool.size)
        val takenNames = HashSet<String>()
        val out = HashMap<AppKey, String>()
        val order = keys.map { it to stableHash("${it.pkg}/${it.cls}#$seed") }.sortedBy { it.second }
        if (set == GlyphSet.HINTS) {
            for ((key, h) in order) {
                val own = hints(key)?.filter { get(it) != null } ?: continue
                if (own.isEmpty()) continue
                val start = Math.floorMod(h, own.size)
                val pick = (own.indices).map { own[(start + it) % own.size] }.firstOrNull { it !in takenNames } ?: continue
                out[key] = pick
                takenNames += pick
                index[pick]?.let { taken[it] = true }
            }
        }
        for ((key, h) in order) {
            if (key in out) continue
            var i = Math.floorMod(h, pool.size)
            var tries = 0
            while ((taken[i] || pool[i].name in takenNames) && tries < pool.size) { i = (i + 1) % pool.size; tries++ }
            taken[i] = true
            takenNames += pool[i].name
            out[key] = pool[i].name
        }
        return out
    }

    companion object {
        /** FNV-1a: String.hashCode is stable too, but clusters badly on similar package names. */
        fun stableHash(s: String): Int {
            var h = 0x811C9DC5.toInt()
            for (c in s) { h = h xor c.code; h *= 0x01000193 }
            return h
        }
    }
}
