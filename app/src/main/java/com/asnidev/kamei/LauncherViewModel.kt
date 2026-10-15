package com.asnidev.kamei

import android.app.Application
import android.content.Context
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asnidev.kamei.apps.AppEntry
import com.asnidev.kamei.apps.AppRepository
import com.asnidev.kamei.data.AppKey
import com.asnidev.kamei.data.Gesture
import com.asnidev.kamei.data.GestureKind
import com.asnidev.kamei.data.IconRef
import com.asnidev.kamei.data.Prefs
import com.asnidev.kamei.data.SettingsStore
import com.asnidev.kamei.icons.AppType
import com.asnidev.kamei.icons.Glyphs
import com.asnidev.kamei.icons.IconPacks
import com.asnidev.kamei.icons.IconRenderer
import com.asnidev.kamei.lang.Vocabulary
import com.asnidev.kamei.lang.Words
import com.asnidev.kamei.system.CustomFonts
import com.asnidev.kamei.system.DeviceStatus
import com.asnidev.kamei.system.SystemActions
import com.asnidev.kamei.system.Torch
import com.asnidev.kamei.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val repo = AppRepository(app, viewModelScope)
    private val store = SettingsStore(app)
    val glyphs = Glyphs(app)
    val renderer = IconRenderer()
    val packs = IconPacks(app)
    val weather = WeatherRepository(app)
    val torch = Torch(app)
    val status = DeviceStatus(app)

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    val prefs: StateFlow<Prefs> = store.prefs
        .onEach { _loaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())

    /** Apps not hidden, in label order (the user's renames win). */
    val apps: StateFlow<List<AppEntry>> = combine(repo.apps, prefs) { list, p ->
        list.filter { it.key !in p.hidden }
            .map { e -> p.renames[e.key]?.let { e.copy(label = it) } ?: e }
            .sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Every app's unexpected glyph for the current set and seed. */
    val dealt: StateFlow<Map<AppKey, String>> = combine(repo.apps, prefs.map { it.glyphSet to it.glyphSeed }.distinctUntilChanged()) { list, (set, seed) ->
        val byKey = list.associateBy { it.key }
        glyphs.deal(list.map { it.key }, set, seed) { k -> byKey[k]?.let { appType(it) }?.hints }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ---- UI state ----
    var drawerOpen by mutableStateOf(false)
    var sheetFor by mutableStateOf<AppKey?>(null)
    var pickerFor by mutableStateOf<AppKey?>(null)
    var renameFor by mutableStateOf<AppKey?>(null)
    var translation by mutableStateOf<Words?>(null)
    var forecastOpen by mutableStateOf(false)
    /** The next drawer opening focuses search even if that option is off (the search gesture). */
    var searchOnOpen by mutableStateOf(false)
    /** Whether we may count notifications; checked again on every return home. */
    var notifAccess by mutableStateOf(false)

    /** Settings pages, outermost first; empty when settings are closed. */
    val settings = mutableStateListOf<String>()

    init {
        CustomFonts.load(app)
        Vocabulary.load(app.assets.open("vocab.tsv").bufferedReader().use { it.readText() })
        viewModelScope.launch(Dispatchers.IO) { glyphs.load() }
        // First start: the phone's default apps go into the favourites stack.
        viewModelScope.launch {
            val p = store.prefs.first()
            if (!p.seeded) {
                val apps = repo.apps.first { it.isNotEmpty() }
                store.update { it.copy(favorites = repo.defaults().ifEmpty { apps.take(6).map { a -> a.key } }, seeded = true) }
            }
        }
        // Read the pack's appfilter off the main thread before icons ask for it.
        viewModelScope.launch {
            prefs.map { it.iconPack }.distinctUntilChanged().collect { pkg ->
                if (pkg.isNotEmpty()) kotlinx.coroutines.withContext(Dispatchers.IO) { packs.filter(pkg) }
            }
        }
        // A new source or place means the cached forecast is for something else;
        // at start the cache is good if it's recent.
        viewModelScope.launch {
            var first = true
            store.prefs.map { listOf(it.weatherOn, it.weatherSource, it.locationMode, it.place) }.distinctUntilChanged()
                .collect {
                    weather.refresh(store.prefs.first(), force = !first)
                    first = false
                }
        }
    }

    fun update(transform: (Prefs) -> Prefs) {
        viewModelScope.launch { store.update(transform) }
    }

    /** Uses the stored settings, not [prefs]: at a cold start that still holds the defaults (weather off). */
    fun refreshWeather(force: Boolean = false) {
        viewModelScope.launch { weather.refresh(store.prefs.first(), force) }
    }

    fun closeAll() {
        drawerOpen = false
        searchOnOpen = false
        sheetFor = null
        pickerFor = null
        renameFor = null
        translation = null
        forecastOpen = false
        settings.clear()
    }

    fun openSettings(page: String = "main") {
        settings.clear()
        settings += "main"
        if (page != "main") settings += page
    }

    fun launch(key: AppKey, bounds: Rect? = null) {
        if (repo.launch(key, bounds)) drawerOpen = false
    }

    fun label(key: AppKey): String =
        prefs.value.renames[key] ?: repo.apps.value.firstOrNull { it.key == key }?.label ?: key.pkg

    /** What kind of app it seems to be (for hint glyphs). */
    fun appType(e: AppEntry): AppType? = AppType.guess(e.key.pkg, e.label, e.category)

    fun isFavorite(key: AppKey) = key in prefs.value.favorites

    fun toggleFavorite(key: AppKey) = update { p ->
        p.copy(favorites = if (key in p.favorites) p.favorites - key else p.favorites + key)
    }

    fun setFavorites(order: List<AppKey>) = update { it.copy(favorites = order) }

    fun setIcon(key: AppKey, ref: IconRef?) = update { p ->
        p.copy(customIcons = if (ref == null) p.customIcons - key else p.customIcons + (key to ref.encode()))
    }

    fun rename(key: AppKey, name: String) = update { p ->
        p.copy(renames = if (name.isBlank()) p.renames - key else p.renames + (key to name.trim()))
    }

    fun hide(key: AppKey, hidden: Boolean) = update { p ->
        p.copy(hidden = if (hidden) p.hidden + key else p.hidden - key, favorites = if (hidden) p.favorites - key else p.favorites)
    }

    /** Yr or Drops, whichever is installed (the user's pick first). */
    fun weatherApp(): AppKey? {
        AppKey.decode(prefs.value.weatherApp)?.let { k -> if (repo.apps.value.any { it.key == k }) return k }
        val all = repo.apps.value
        return all.firstOrNull { it.key.pkg.startsWith("no.nrk.yr") || it.label.equals("Yr", ignoreCase = true) }?.key
            ?: all.firstOrNull { it.label.startsWith("Drops", ignoreCase = true) || "drops" in it.key.pkg.lowercase() }?.key
    }

    /** Runs what a home-screen swipe is set to do. */
    fun runGesture(g: Gesture, context: Context) {
        when (g.kind) {
            GestureKind.NONE -> Unit
            GestureKind.APP -> g.app?.let { launch(it) }
            GestureKind.DRAWER -> drawerOpen = true
            GestureKind.SEARCH -> { searchOnOpen = true; drawerOpen = true }
            GestureKind.NOTIFICATIONS -> SystemActions.expandNotifications(context)
            GestureKind.QUICK_SETTINGS -> SystemActions.expandQuickSettings(context)
            GestureKind.WEATHER -> weatherApp()?.let { launch(it) } ?: run { forecastOpen = true }
            GestureKind.CAMERA -> SystemActions.openCamera(context)
            GestureKind.TORCH -> torch.toggle()
            GestureKind.SETTINGS -> openSettings()
        }
    }

    override fun onCleared() {
        status.stop()
        torch.close()
        repo.close()
    }
}
