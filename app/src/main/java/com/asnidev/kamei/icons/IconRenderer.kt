package com.asnidev.kamei.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.asnidev.kamei.data.IconStyle
import kotlin.math.sqrt

/**
 * Turns app and icon-pack drawables into the launcher's styles and keeps the
 * results in memory. Line, themed and pack one-colour icons come out white on
 * transparent, so the UI tints them like the glyphs.
 */
class IconRenderer {

    /** A drawn icon; [tint] when it is white line art the UI should colour. */
    class Rendered(val image: ImageBitmap, val tint: Boolean)

    private val cache = LruCache<String, Rendered>(600)

    fun cached(id: String): Rendered? = cache.get(id)

    fun clear() = cache.evictAll()

    /** Call off the main thread. [id] must change whenever the inputs do. */
    fun app(id: String, drawable: Drawable, style: IconStyle): Rendered = cache.get(id) ?: run {
        val px = when (style) {
            IconStyle.ORIGINAL, IconStyle.OBSCURE -> raster(drawable, SIDE, Layer.WHOLE)
            IconStyle.MONO -> IconPixels.greyscale(raster(drawable, SIDE, Layer.WHOLE))
            IconStyle.THEMED -> if (hasMonochrome(drawable)) IconPixels.white(raster(drawable, SIDE, Layer.MONOCHROME))
                else line(drawable)
            IconStyle.LINE -> line(drawable)
        }
        Rendered(toBitmap(px), style == IconStyle.LINE || style == IconStyle.THEMED).also { cache.put(id, it) }
    }

    /** An icon-pack drawable: line-art packs (one colour) become white like the glyphs, the rest stay as drawn. */
    fun pack(id: String, drawable: Drawable): Rendered = cache.get(id) ?: run {
        val px = raster(drawable, SIDE, Layer.WHOLE)
        val one = IconPixels.isOneColour(px)
        Rendered(toBitmap(if (one) IconPixels.white(px) else px), one).also { cache.put(id, it) }
    }

    private fun line(drawable: Drawable): IntArray {
        // The foreground alone, without the coloured plate behind it, unless that leaves nothing.
        val fg = raster(drawable, SIDE, Layer.FOREGROUND)
        val coverage = fg.count { (it ushr 24) > 128 }.toFloat() / fg.size
        val src = if (coverage in 0.02f..0.9f) fg else raster(drawable, SIDE, Layer.WHOLE)
        return IconPixels.edges(src, SIDE)
    }

    private fun toBitmap(px: IntArray): ImageBitmap =
        Bitmap.createBitmap(px, SIDE, SIDE, Bitmap.Config.ARGB_8888).asImageBitmap()

    private fun hasMonochrome(drawable: Drawable): Boolean =
        Build.VERSION.SDK_INT >= 33 && (drawable as? AdaptiveIconDrawable)?.monochrome != null

    private enum class Layer { WHOLE, FOREGROUND, MONOCHROME }

    private fun raster(drawable: Drawable, side: Int, layer: Layer): IntArray {
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = drawable as? AdaptiveIconDrawable
        val part: Drawable? = when {
            adaptive == null || layer == Layer.WHOLE -> null
            layer == Layer.MONOCHROME && Build.VERSION.SDK_INT >= 33 -> adaptive.monochrome ?: adaptive.foreground
            else -> adaptive.foreground
        }
        if (part != null) {
            // A layer is 108 units wide and only the middle 72 show through the mask.
            val pad = side / 4
            val old = part.copyBounds()
            part.setBounds(-pad, -pad, side + pad, side + pad)
            part.draw(canvas)
            part.bounds = old
        } else {
            val old = drawable.copyBounds()
            drawable.setBounds(0, 0, side, side)
            drawable.draw(canvas)
            drawable.bounds = old
        }
        val px = IntArray(side * side)
        bitmap.getPixels(px, 0, side, 0, 0, side, side)
        bitmap.recycle()
        return px
    }

    companion object {
        const val SIDE = 144
    }
}

/** Pixel work on plain ARGB arrays (row-major, not premultiplied), so it runs in unit tests. */
object IconPixels {

    private fun lum(c: Int): Float =
        (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f

    fun greyscale(px: IntArray): IntArray = IntArray(px.size) { i ->
        val c = px[i]
        val g = (lum(c) * 255).toInt().coerceIn(0, 255)
        (c and 0xFF000000.toInt()) or (g shl 16) or (g shl 8) or g
    }

    /** The shape in white, keeping its opacity (a one-colour icon's shading). */
    fun white(px: IntArray): IntArray = IntArray(px.size) { i -> (px[i] and 0xFF000000.toInt()) or 0xFFFFFF }

    /** Whether nearly all of the opaque area is one colour. */
    fun isOneColour(px: IntArray): Boolean {
        val opaque = px.filter { (it ushr 24) >= 128 }
        if (opaque.isEmpty()) return true
        val buckets = opaque.groupingBy { c -> ((c shr 20) and 0xF) * 256 + ((c shr 12) and 0xF) * 16 + ((c shr 4) and 0xF) }.eachCount()
        return buckets.values.max() >= opaque.size * 0.85f
    }

    /**
     * A line drawing of the picture: white where its outline or an inner colour
     * boundary runs (Sobel on opacity and on brightness), transparent elsewhere.
     */
    fun edges(px: IntArray, n: Int): IntArray {
        val a = FloatArray(px.size) { ((px[it] ushr 24) and 0xFF) / 255f }
        val l = FloatArray(px.size) { lum(px[it]) * a[it] }
        val out = IntArray(px.size)
        for (y in 1 until n - 1) for (x in 1 until n - 1) {
            val e = maxOf(sobel(a, n, x, y), sobel(l, n, x, y) * 1.6f)
            // Soft threshold: faint texture vanishes, real edges are solid with smooth sides.
            val v = ((e - 0.35f) / 0.5f).coerceIn(0f, 1f)
            if (v > 0f) out[y * n + x] = ((v * 255).toInt() shl 24) or 0xFFFFFF
        }
        return out
    }

    private fun sobel(v: FloatArray, n: Int, x: Int, y: Int): Float {
        fun at(dx: Int, dy: Int) = v[(y + dy) * n + x + dx]
        val gx = -at(-1, -1) - 2 * at(-1, 0) - at(-1, 1) + at(1, -1) + 2 * at(1, 0) + at(1, 1)
        val gy = -at(-1, -1) - 2 * at(0, -1) - at(1, -1) + at(-1, 1) + 2 * at(0, 1) + at(1, 1)
        return sqrt(gx * gx + gy * gy)
    }
}
