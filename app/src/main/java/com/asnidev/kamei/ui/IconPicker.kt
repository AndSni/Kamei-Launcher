package com.asnidev.kamei.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.IconRef
import com.asnidev.kamei.data.IconStyle
import com.asnidev.kamei.icons.AppType
import com.asnidev.kamei.icons.Glyph
import com.asnidev.kamei.icons.IconPacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPicker(key: AppKey) {
    val vm = LocalVm.current
    val scheme = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }
    val packs = remember { vm.packs.installed() }
    val tabs = listOf("Glyphs", "Styles") + if (packs.isNotEmpty()) listOf("Icon pack") else emptyList()
    val choose = { ref: IconRef? -> vm.setIcon(key, ref); vm.pickerFor = null }

    Column(Modifier.fillMaxSize().background(scheme.surface).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).clickable { vm.pickerFor = null }, contentAlignment = Alignment.Center) {
                GlyphIcon("arrow-left", 24.dp, tint = scheme.onSurface, stroke = 1.75f)
            }
            Spacer(Modifier.width(8.dp))
            Text("Icon for ${vm.label(key)}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AppIcon(key, 32.dp, tint = scheme.onSurface)
            Spacer(Modifier.width(16.dp))
        }
        PrimaryTabRow(selectedTabIndex = tab, containerColor = scheme.surface) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> GlyphGrid(key) { choose(IconRef.Glyph(it)) }
            1 -> StyleChoices(key, choose)
            else -> PackGrid(key, packs, choose)
        }
    }
}

@Composable
private fun GlyphGrid(key: AppKey, onPick: (String) -> Unit) {
    val vm = LocalVm.current
    val scheme = MaterialTheme.colorScheme
    val prefs by vm.prefs.collectAsState()
    val apps by vm.repo.apps.collectAsState()
    var query by remember { mutableStateOf("") }
    val all by produceState<List<Glyph>>(emptyList()) { value = withContext(Dispatchers.IO) { vm.glyphs.all() } }
    val shown by produceState(all, all, query) { value = withContext(Dispatchers.Default) { if (all.isEmpty()) all else vm.glyphs.search(query) } }
    // Sideways hints: for the app's own kind when the search is empty, else for any kind the search names.
    val own = remember(key, apps) { apps.firstOrNull { it.key == key }?.let { vm.appType(it) } }
    val types = remember(query, own) { if (query.isBlank()) listOfNotNull(own) else AppType.matching(query) }
    Column {
        OutlinedTextField(
            query, { query = it },
            singleLine = true,
            placeholder = { Text("Search ${all.size} glyphs, or a kind of app (music, bank…)") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        )
        LazyVerticalGrid(GridCells.Adaptive(56.dp), contentPadding = PaddingValues(12.dp)) {
            types.forEach { t ->
                item(key = "h:${t.name}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (t == own && query.isBlank()) "Hints for ${vm.label(key)} (looks like ${t.label.lowercase()})" else "Hints for ${t.label.lowercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                items(t.hints, key = { "${t.name}:$it" }) { name -> GlyphCell(name, prefs.stroke, onPick) }
            }
            if (types.isNotEmpty()) {
                item(key = "all", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (query.isBlank()) "All glyphs" else "Names matching \"$query\"",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
            }
            items(shown, key = { it.name }) { g -> GlyphCell(g.name, prefs.stroke, onPick) }
        }
    }
}

@Composable
private fun GlyphCell(name: String, stroke: Float, onPick: (String) -> Unit) {
    Box(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).clickable { onPick(name) },
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(name, 28.dp, tint = MaterialTheme.colorScheme.onSurface, stroke = stroke)
    }
}

@Composable
private fun StyleChoices(key: AppKey, choose: (IconRef?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 8.dp)) {
        Choice("Automatic", "Follow the launcher's icon style", { AppIconPreview(key, null) }) { choose(null) }
        IconStyle.entries.forEach { style ->
            Choice(style.label, style.summary, { AppIconPreview(key, style) }) { choose(IconRef.Style(style)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tip: unexpected glyphs change for every app when you reshuffle them in Settings › Icons.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun Choice(title: String, summary: String, preview: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { preview() }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The app's icon as [style] would draw it (null: as it is now, before any per-app pick). */
@Composable
private fun AppIconPreview(key: AppKey, style: IconStyle?) {
    val vm = LocalVm.current
    val prefs by vm.prefs.collectAsState()
    val dealt by vm.dealt.collectAsState()
    val tint = MaterialTheme.colorScheme.onSurface
    val effective = style ?: prefs.iconStyle
    if (effective == IconStyle.OBSCURE) {
        GlyphIcon(dealt[key] ?: "circle", 36.dp, tint = tint, stroke = prefs.stroke)
        return
    }
    val id = "${key.encode()}|$effective|preview"
    val r by produceState(vm.renderer.cached(id), id) {
        if (value == null) value = withContext(Dispatchers.IO) { vm.repo.icon(key)?.let { vm.renderer.app(id, it, effective) } }
    }
    r?.let { Image(it.image, null, Modifier.size(36.dp), colorFilter = if (it.tint) ColorFilter.tint(tint) else null) }
}

@Composable
private fun PackGrid(key: AppKey, packs: List<IconPacks.Pack>, choose: (IconRef?) -> Unit) {
    val vm = LocalVm.current
    val prefs by vm.prefs.collectAsState()
    var pack by remember { mutableStateOf(packs.firstOrNull { it.pkg == prefs.iconPack } ?: packs.first()) }
    var query by remember { mutableStateOf("") }
    val drawables by produceState<List<String>>(emptyList(), pack) {
        value = withContext(Dispatchers.IO) { vm.packs.filter(pack.pkg)?.drawables.orEmpty() }
    }
    val suggested = remember(pack, drawables) { vm.packs.drawableFor(pack.pkg, key.pkg, key.cls) }
    val shown = remember(drawables, query, suggested) {
        val q = query.trim().lowercase()
        val list = if (q.isEmpty()) drawables else drawables.filter { q in it.lowercase() }
        listOfNotNull(suggested) + (list - setOfNotNull(suggested))
    }
    Column {
        if (packs.size > 1) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                packs.forEach { p ->
                    androidx.compose.material3.FilterChip(selected = p == pack, onClick = { pack = p }, label = { Text(p.label) })
                }
            }
        }
        OutlinedTextField(
            query, { query = it },
            singleLine = true,
            placeholder = { Text("Search ${pack.label}") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
        )
        LazyVerticalGrid(GridCells.Adaptive(64.dp), contentPadding = PaddingValues(12.dp)) {
            items(shown, key = { it }) { name ->
                Box(
                    Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).clickable { choose(IconRef.Pack(pack.pkg, name)) },
                    contentAlignment = Alignment.Center,
                ) {
                    PackDrawable(pack.pkg, name)
                }
            }
        }
    }
}

@Composable
private fun PackDrawable(pkg: String, name: String) {
    val vm = LocalVm.current
    val id = "pack:$pkg:$name"
    val r by produceState(vm.renderer.cached(id), id) {
        if (value == null) value = withContext(Dispatchers.IO) { vm.packs.drawable(pkg, name)?.let { vm.renderer.pack(id, it) } }
    }
    r?.let { Image(it.image, null, Modifier.size(40.dp), colorFilter = if (it.tint) ColorFilter.tint(MaterialTheme.colorScheme.onSurface) else null) }
}
