package com.asnidev.kamei.system

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

object SystemActions {

    /**
     * Sets the phone's own wallpaper to plain black: the home screen's, and the lock
     * screen's too if [lock]. Android shows it for a moment while locking, unlocking
     * and switching apps, whatever the home app draws. Blocking: call it off the main
     * thread. False if the phone refused.
     */
    fun blackWallpaper(context: Context, lock: Boolean): Boolean = runCatching {
        val black = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
        val which = WallpaperManager.FLAG_SYSTEM or if (lock) WallpaperManager.FLAG_LOCK else 0
        WallpaperManager.getInstance(context).setBitmap(black, null, true, which) != 0
    }.onFailure { Log.w("Kamei", "black wallpaper", it) }.getOrDefault(false)

    /** No public API for this; every launcher uses the same StatusBarManager reflection. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        runCatching {
            val sbm = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sbm)
        }
    }

    /** The quick settings tiles, fully open. Same reflection as [expandNotifications]. */
    @SuppressLint("WrongConstant")
    fun expandQuickSettings(context: Context) {
        runCatching {
            val sbm = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandSettingsPanel").invoke(sbm)
        }
    }

    /**
     * Android's "Wallpaper & style", where the phone's colours come from (the wallpaper,
     * or a colour picked there). The Pixel and stock versions, else any wallpaper picker.
     */
    fun openWallpaperAndStyle(context: Context) {
        val picker = "com.android.customization.picker.CustomizationPickerActivity"
        val tried = listOf("com.google.android.apps.wallpaper", "com.android.wallpaper").any { pkg ->
            open(context, Intent(Intent.ACTION_SET_WALLPAPER).setClassName(pkg, picker))
        }
        if (!tried) open(context, Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null))
    }

    fun openCamera(context: Context) = open(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

    fun openSettings(context: Context) = open(context, Intent(Settings.ACTION_SETTINGS))

    fun isDefaultLauncher(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(home, 0)?.activityInfo?.packageName == context.packageName
    }

    /** Asks to become the home app: the system's role dialog where there is one, else the settings page. */
    fun requestDefaultLauncher(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            val roles = activity.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { activity.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 1) }
                    .onSuccess { return }
            }
        }
        open(activity, Intent(Settings.ACTION_HOME_SETTINGS))
    }

    /**
     * Starts [intent]; false when nothing handles it. Settings screens are
     * missing or locked down on some phones, which must not crash the launcher.
     */
    fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) { // ActivityNotFoundException, SecurityException
        Log.w("SystemActions", "couldn't open $intent: $e")
        false
    }
}
