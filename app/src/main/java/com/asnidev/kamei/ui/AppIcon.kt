package com.asnidev.kamei.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.IconRef
import com.asnidev.kamei.data.IconStyle
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.icons.IconRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What an app's icon is drawn from, after the user's choices are applied. */
sealed interface IconSourceRef {
    data class Glyph(val name: String) : IconSourceRef
    data class Pack(val pkg: String, val drawable: String) : IconSourceRef
    data class App(val style: IconStyle) : IconSourceRef
}

/**
 * The user's pick for the app; else its unexpected glyph; else the icon pack's
 * drawing of it; else its own icon in the chosen style.
 */
fun resolveIcon(key: AppKey, prefs: Prefs, dealt: Map<AppKey, String>, packDrawable: (String) -> String?): IconSourceRef {
    val style = when (val ref = IconRef.decode(prefs.customIcons[key])) {
        is IconRef.Glyph -> return IconSourceRef.Glyph(ref.name)
        is IconRef.Pack -> return IconSourceRef.Pack(ref.pkg, ref.drawable)
        is IconRef.Style -> ref.style
        null -> prefs.iconStyle
    }
    if (style == IconStyle.OBSCURE) return IconSourceRef.Glyph(dealt[key] ?: "circle")
    if (prefs.iconPack.isNotEmpty()) packDrawable(prefs.iconPack)?.let { return IconSourceRef.Pack(prefs.iconPack, it) }
    return IconSourceRef.App(style)
}

/** A tonal circle for one-colour icons (Material): its fill, and the colour the icon takes on it. */
class Tonal(val circle: Color, val on: Color)

/**
 * An app's icon at [size]. With [tonal], one-colour icons (glyphs, line drawings) sit
 * small in a filled circle of [size], and full-colour icons, which bring their own
 * shape, fill that whole circle as their designers meant.
 */
@Composable
fun AppIcon(key: AppKey, size: Dp, modifier: Modifier = Modifier, tint: Color = LocalLook.current.iconTint, tonal: Tonal? = null) {
    val vm = LocalVm.current
    val prefs by vm.prefs.collectAsState()
    val dealt by vm.dealt.collectAsState()
    val generation by vm.repo.generation.collectAsState()
    val source = remember(key, prefs.customIcons, prefs.iconStyle, prefs.iconPack, dealt) {
        resolveIcon(key, prefs, dealt) { pack -> vm.packs.drawableFor(pack, key.pkg, key.cls) }
    }
    when (source) {
        is IconSourceRef.Glyph -> if (tonal != null) InCircle(size, tonal, modifier) { GlyphIcon(source.name, size * INNER, tint = tonal.on, stroke = prefs.stroke) }
            else GlyphIcon(source.name, size, modifier, tint, prefs.stroke)
        is IconSourceRef.Pack, is IconSourceRef.App -> {
            val id = when (source) {
                is IconSourceRef.Pack -> "pack:${source.pkg}:${source.drawable}"
                is IconSourceRef.App -> "${key.encode()}|${source.style}|$generation"
                else -> ""
            }
            val rendered by produceState(vm.renderer.cached(id), id) {
                if (value != null) return@produceState
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        when (source) {
                            is IconSourceRef.Pack -> vm.packs.drawable(source.pkg, source.drawable)?.let { vm.renderer.pack(id, it) }
                            is IconSourceRef.App -> vm.repo.icon(key)?.let { vm.renderer.app(id, it, source.style) }
                            else -> null
                        }
                    }.getOrNull()
                }
            }
            val r = rendered
            if (tonal != null && r != null && r.tint) InCircle(size, tonal, modifier) { RenderedIcon(r, size * INNER, Modifier, tonal.on) }
            else RenderedIcon(r, size, modifier, tint)
        }
    }
}

/** How much of a tonal circle a one-colour icon fills. */
private const val INNER = 0.6f

@Composable
private fun InCircle(size: Dp, tonal: Tonal, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(size).background(tonal.circle, CircleShape), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun GlyphIcon(name: String, size: Dp, modifier: Modifier = Modifier, tint: Color = LocalLook.current.iconTint, stroke: Float = 1.25f) {
    val vm = LocalVm.current
    val vector = remember(name, stroke) { vm.glyphs.vector(name, stroke) }
    if (vector == null) {
        Box(modifier.size(size))
        return
    }
    Image(
        painter = rememberVectorPainter(vector),
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun RenderedIcon(r: IconRenderer.Rendered?, size: Dp, modifier: Modifier, tint: Color) {
    if (r == null) {
        Box(modifier.size(size))
        return
    }
    Image(
        bitmap = r.image,
        contentDescription = null,
        modifier = modifier.size(size),
        colorFilter = if (r.tint) ColorFilter.tint(tint) else null,
        filterQuality = FilterQuality.High,
    )
}
