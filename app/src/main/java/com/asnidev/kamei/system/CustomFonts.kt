package com.asnidev.kamei.system

import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * A font of the user's own for the clock: picked from anywhere on the phone (a
 * download, a cloud drive), copied into the app so it keeps working if the
 * original moves, and checked to be a real font that can draw digits.
 */
object CustomFonts {

    /** The loaded clock font, or null. Compose reads it, so a new pick redraws the clock. */
    var clock: Typeface? by mutableStateOf(null)
        private set

    private fun file(context: Context) = File(File(context.filesDir, "fonts"), "clock.font")

    fun load(context: Context) {
        val f = file(context)
        clock = if (f.exists()) runCatching { Typeface.createFromFile(f) }.getOrNull()?.takeIf(::drawsDigits) else null
    }

    /**
     * Copies the font at [uri] in and loads it; the result is its file name, for
     * the settings row. Fails (keeping the old font) when it isn't a usable font.
     */
    fun import(context: Context, uri: Uri): Result<String> = runCatching {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "Your font"
        val tmp = File(context.cacheDir, "font-import.tmp")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            tmp.outputStream().use { out ->
                val copied = input.copyTo(out)
                require(copied in 1..MAX_BYTES) { "font file too big or empty" }
            }
        }
        val typeface = Typeface.createFromFile(tmp)
        require(drawsDigits(typeface)) { "not a font with digits" }
        val dest = file(context)
        dest.parentFile?.mkdirs()
        tmp.copyTo(dest, overwrite = true)
        tmp.delete()
        clock = Typeface.createFromFile(dest)
        name.substringBeforeLast('.')
    }.onFailure { Log.w("CustomFonts", "couldn't use that font", it) }

    /** Android falls back to its default typeface for broken files; real fonts draw "0123456789" with some ink. */
    private fun drawsDigits(t: Typeface): Boolean {
        if (t == Typeface.DEFAULT) return false
        val bounds = Rect()
        Paint().apply { typeface = t; textSize = 100f }.getTextBounds("0123456789", 0, 10, bounds)
        return bounds.width() > 0 && bounds.height() > 0
    }

    private const val MAX_BYTES = 30L * 1024 * 1024
}
