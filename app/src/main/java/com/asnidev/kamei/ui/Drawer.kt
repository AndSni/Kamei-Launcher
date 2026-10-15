package com.asnidev.kamei.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import com.asnidev.kamei.apps.AppEntry
import com.asnidev.kamei.data.DrawerLayout
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.lang.Transcribe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Drawer(prefs: Prefs) {
    val vm = LocalVm.current
    val look = LocalLook.current
    val apps by vm.apps.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    // Names as the drawer writes them: as they are, or sounded out in the home screen's script.
    val phonetic = prefs.phoneticLabels
    val names = remember(apps, phonetic, prefs.workMark) {
        apps.associate {
            val name = if (phonetic) Transcribe.label(it.label) else it.label
            it.key to if (it.isWork && prefs.workMark.star) "$name*" else name
        }
    }
    val shown = remember(apps, query, names) {
        val q = query.trim()
        // Search matches the written name and the real one, so typing "you" still finds ユーチューブ.
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) || names[it.key].orEmpty().contains(q, ignoreCase = true) }
    }
    val density = LocalDensity.current
    val closeAt = with(density) { 96.dp.toPx() }
    val flingAt = with(density) { 700.dp.toPx() }
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val atTop = { if (prefs.drawerLayout == DrawerLayout.LIST) !listState.canScrollBackward else !gridState.canScrollBackward }
    // How far the drawer has been pulled down; it follows the finger and springs back if let go early.
    val pullOffset = remember { Animatable(0f) }
    val quickPull = with(density) { 40.dp.toPx() }
    val scope = rememberCoroutineScope()
    // Pulling down while the list is at its top drags the drawer itself; far enough, or flicked,
    // it closes. Taken before the list sees the movement (pre-scroll), so it works however short
    // the list is.
    val pull = remember(prefs.drawerLayout) {
        object : NestedScrollConnection {
            /** When the current pull began, to tell a flick (short and fast) from a slow tug. */
            var pullStart = 0L

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val current = pullOffset.value
                return when {
                    available.y > 0 && atTop() -> {
                        if (current == 0f) pullStart = android.os.SystemClock.uptimeMillis()
                        scope.launch { pullOffset.snapTo(current + available.y) }
                        Offset(0f, available.y)
                    }
                    // Pushing back up first undoes the pull, then scrolls the list as usual.
                    available.y < 0 && current > 0 -> {
                        val back = maxOf(available.y, -current)
                        scope.launch { pullOffset.snapTo(current + back) }
                        Offset(0f, back)
                    }
                    else -> Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val pulled = pullOffset.value
                // Fling speed comes from the system's tracker, which some gestures leave at 0;
                // the pull's own average speed backs it up (over 1 px per ms is a flick).
                val ms = (android.os.SystemClock.uptimeMillis() - pullStart).coerceAtLeast(1)
                val flicked = pulled > quickPull && pulled / ms > 1f
                if (pulled > closeAt || flicked || (available.y > flingAt && atTop())) {
                    vm.drawerOpen = false
                    return available
                }
                if (pulled > 0) {
                    pullOffset.animateTo(0f, spring())
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = pullOffset.value * 0.6f
                alpha = 1f - (pullOffset.value / (closeAt * 4)).coerceIn(0f, 0.4f)
            }
            .background(if (prefs.showWallpaper) look.background.copy(alpha = 0.88f) else look.background)
            .windowInsetsPadding(screenInsets(prefs)),
    ) {
        // The search bar's area takes a pull down too; with automatic cutout handling it
        // sits just below a camera hole that would land on it.
        CutoutClear(prefs) {
        Box(
            Modifier.pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { change, dy -> total += dy; change.consume() },
                    onDragEnd = { if (total > closeAt) vm.drawerOpen = false },
                )
            },
        ) {
            SearchPill(query, prefs.drawerKeyboard || vm.searchOnOpen, onChange = { query = it }, onGo = { shown.firstOrNull()?.let { vm.launch(it.key) } })
        }
        }
        // Android's stretch overscroll would take the downward pull for itself at the top of the
        // list, and the drawer would never close; the drawer's own pull replaces it.
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        when (prefs.drawerLayout) {
            DrawerLayout.LIST -> {
                LazyColumn(
                    Modifier.fillMaxSize().nestedScroll(pull),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(shown, key = { it.key.encode() }) { app -> ListRow(app, names[app.key] ?: app.label, prefs) }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(prefs.drawerColumns),
                    modifier = Modifier.fillMaxSize().nestedScroll(pull),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (prefs.drawerLayout == DrawerLayout.GRID) 28.dp else 20.dp),
                ) {
                    items(shown, key = { it.key.encode() }) { app -> GridCell(app, names[app.key] ?: app.label, prefs.drawerLayout == DrawerLayout.GRID_LABELS) }
                }
            }
        }
        }
    }
}

@Composable
private fun SearchPill(query: String, autoFocus: Boolean, onChange: (String) -> Unit, onGo: () -> Unit) {
    val vm = LocalVm.current
    val scheme = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    if (autoFocus) {
        LaunchedEffect(Unit) {
            // Wait for the drawer's slide-in to begin, or the IME may ignore the request.
            delay(80)
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // Closing the drawer (Back, launching an app) takes the keyboard with it.
    DisposableEffect(Unit) {
        onDispose {
            keyboard?.hide()
            vm.searchOnOpen = false
        }
    }
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon("search", 22.dp, tint = scheme.onSurfaceVariant, stroke = 1.75f)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search apps", color = scheme.onSurfaceVariant, fontSize = 17.sp)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 17.sp),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).clickable { vm.openSettings() },
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon("settings", 22.dp, tint = scheme.onSurfaceVariant, stroke = 1.75f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(app: AppEntry, name: String, labels: Boolean) {
    val vm = LocalVm.current
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = { vm.launch(app.key) }, onLongClick = { vm.sheetFor = app.key })
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RailIcon(app.key, 40.dp)
        if (labels) {
            Spacer(Modifier.height(8.dp))
            Text(
                name,
                color = LocalLook.current.text.copy(alpha = 0.9f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(app: AppEntry, name: String, prefs: Prefs) {
    val vm = LocalVm.current
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { vm.launch(app.key) }, onLongClick = { vm.sheetFor = app.key })
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RailIcon(app.key, 32.dp)
        Spacer(Modifier.width(20.dp))
        Text(name, color = LocalLook.current.text, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (app.key in prefs.favorites) GlyphIcon("star", 16.dp, tint = LocalLook.current.dim, stroke = 1.5f)
    }
}
