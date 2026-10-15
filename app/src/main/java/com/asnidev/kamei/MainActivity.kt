package com.asnidev.kamei

import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.asnidev.kamei.data.GlyphSet
import com.asnidev.kamei.data.HomeTheme
import com.asnidev.kamei.data.IconStyle
import com.asnidev.kamei.data.InfoAlign
import com.asnidev.kamei.data.SettingsStyle
import com.asnidev.kamei.data.LocationMode
import com.asnidev.kamei.data.Place
import com.asnidev.kamei.lang.DisplayLang
import com.asnidev.kamei.system.NotifListener
import com.asnidev.kamei.ui.AppSheet
import com.asnidev.kamei.ui.Drawer
import com.asnidev.kamei.ui.ForecastSheet
import com.asnidev.kamei.ui.Home
import com.asnidev.kamei.ui.IconPicker
import com.asnidev.kamei.ui.LocalLook
import com.asnidev.kamei.ui.rememberLook
import com.asnidev.kamei.ui.themeIsDark
import com.asnidev.kamei.ui.LocalVm
import com.asnidev.kamei.ui.RenameDialog
import com.asnidev.kamei.ui.SettingsScreen
import com.asnidev.kamei.ui.SystemLook
import com.asnidev.kamei.ui.TranslationPopup

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light icons on the black home screen, whatever the phone's theme.
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        debugExtras(intent)
        setContent { Root(vm, this) }
    }

    /** Pressing Home while already home closes whatever is open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.closeAll()
        debugExtras(intent)
    }

    override fun onStart() {
        super.onStart()
        vm.status.start()
    }

    override fun onResume() {
        super.onResume()
        vm.repo.refresh()
        vm.notifAccess = NotifListener.hasAccess(this)
        vm.refreshWeather()
    }

    /** Coming back from an app finds the bare home screen. */
    override fun onStop() {
        super.onStop()
        vm.status.stop()
        vm.closeAll()
    }

    /**
     * Debug builds only, for checking looks over adb: `--es icons line|obscure|…`,
     * `--es lang ja|zh|ko|el`, `--ez drawer true`, `--es settings <page>`,
     * `--es place "56.95,24.11,Rīga"` (weather at a fixed place), `--es glyphs hints|mixed|…`,
     * `--ez wallpaper true|false`, `--ez searchnow true|false`, `--es align top|middle|bottom`,
     * `--ez phonetic true|false`, `--es theme original|material|tokyo`. These are saved. Not saved: `--es fakecutout "l,t,r,b"` (or "none").
     */
    private fun debugExtras(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("icons")?.let { name ->
            IconStyle.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { s -> vm.update { it.copy(iconStyle = s) } }
        }
        intent.getStringExtra("lang")?.let { tag ->
            DisplayLang.entries.firstOrNull { it.tag == tag }?.let { l -> vm.update { it.copy(displayLang = l) } }
        }
        intent.getStringExtra("glyphs")?.let { name ->
            GlyphSet.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { g -> vm.update { it.copy(glyphSet = g) } }
        }
        if (intent.hasExtra("wallpaper")) intent.getBooleanExtra("wallpaper", false).let { on -> vm.update { it.copy(showWallpaper = on) } }
        if (intent.hasExtra("searchnow")) intent.getBooleanExtra("searchnow", false).let { on -> vm.update { it.copy(drawerKeyboard = on) } }
        intent.getStringExtra("align")?.let { name ->
            InfoAlign.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { a -> vm.update { it.copy(infoAlign = a) } }
        }
        if (intent.hasExtra("phonetic")) intent.getBooleanExtra("phonetic", false).let { on -> vm.update { it.copy(phoneticLabels = on) } }
        intent.getStringExtra("fakecutout")?.let { v ->
            val n = v.split(',').mapNotNull { it.trim().toIntOrNull() }
            com.asnidev.kamei.ui.fakeCutout = if (n.size == 4) android.graphics.Rect(n[0], n[1], n[2], n[3]) else null
        }
        intent.getStringExtra("theme")?.let { name ->
            HomeTheme.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { t -> vm.update { it.copy(theme = t) } }
        }
        intent.getStringExtra("settingsstyle")?.let { name ->
            SettingsStyle.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { st -> vm.update { it.copy(settingsStyle = st) } }
        }
        if (intent.getBooleanExtra("drawer", false)) vm.drawerOpen = true
        intent.getStringExtra("settings")?.let { vm.openSettings(it) }
        // "lat,lon,name": weather on, at that fixed place.
        intent.getStringExtra("place")?.split(',')?.takeIf { it.size >= 3 }?.let { f ->
            val lat = f[0].toDoubleOrNull() ?: return@let
            val lon = f[1].toDoubleOrNull() ?: return@let
            vm.update { it.copy(weatherOn = true, locationMode = LocationMode.FIXED, place = Place(f.drop(2).joinToString(","), lat, lon)) }
        }
    }
}

@Composable
private fun Root(vm: LauncherViewModel, activity: ComponentActivity) {
    val prefs by vm.prefs.collectAsState()
    val loaded by vm.loaded.collectAsState()

    LaunchedEffect(prefs.hideStatusBar) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (prefs.hideStatusBar) controller.hide(WindowInsetsCompat.Type.statusBars())
        else controller.show(WindowInsetsCompat.Type.statusBars())
    }

    // The system draws the wallpaper behind a window that asks for it and has no background of its own.
    LaunchedEffect(prefs.showWallpaper) {
        val w = activity.window
        if (prefs.showWallpaper) {
            // The surface must become translucent too: one created opaque (black theme
            // background) is never cleared between frames, and old content ghosts through.
            w.setFormat(PixelFormat.TRANSLUCENT)
            w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            w.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        } else {
            w.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            w.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
            w.setFormat(PixelFormat.OPAQUE)
        }
    }

    // A launcher never backs out of itself; Back closes the topmost thing.
    BackHandler {
        when {
            vm.translation != null -> vm.translation = null
            vm.renameFor != null -> vm.renameFor = null
            vm.pickerFor != null -> vm.pickerFor = null
            vm.sheetFor != null -> vm.sheetFor = null
            vm.settings.isNotEmpty() -> vm.settings.removeAt(vm.settings.lastIndex)
            vm.forecastOpen -> vm.forecastOpen = false
            vm.drawerOpen -> vm.drawerOpen = false
        }
    }

    val battery by vm.status.battery.collectAsState()
    val look = rememberLook(prefs, battery?.percent)
    // Light themes need dark status and navigation bar icons.
    LaunchedEffect(look.light) {
        val style = if (look.light) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        activity.enableEdgeToEdge(style, style)
    }

    CompositionLocalProvider(LocalVm provides vm, LocalLook provides look) {
        SystemLook(dark = themeIsDark(prefs.theme)) {
            Box(Modifier.fillMaxSize().background(if (prefs.showWallpaper) Color.Transparent else look.background)) {
                if (!loaded) return@Box
                Home(prefs)
                AnimatedVisibility(vm.drawerOpen, enter = slideInVertically { it / 3 } + fadeIn(), exit = slideOutVertically { it / 3 } + fadeOut()) {
                    Drawer(prefs)
                }
                AnimatedVisibility(vm.settings.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    SettingsScreen(prefs)
                }
                vm.pickerFor?.let { IconPicker(it) }
                vm.sheetFor?.let { AppSheet(it) }
                vm.renameFor?.let { RenameDialog(it) }
                vm.translation?.let { TranslationPopup(it, prefs) }
                if (vm.forecastOpen) ForecastSheet(prefs)
            }
        }
    }
}
