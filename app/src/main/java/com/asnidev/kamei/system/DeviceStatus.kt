package com.asnidev.kamei.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Battery(val percent: Int, val charging: Boolean)

/** What the mobile network looks like; [bars] is 0..4 when there's service. */
sealed interface Signal {
    /** No SIM, no phone radio, or airplane mode: nothing worth warning about. */
    data object Quiet : Signal
    data object NoService : Signal
    data class Bars(val bars: Int) : Signal
}

/**
 * Battery level and mobile signal for the home screen's warning lines. Listens
 * only between [start] and [stop] (while home is visible) and needs no permission.
 */
class DeviceStatus(private val context: Context) {

    private val _battery = MutableStateFlow<Battery?>(null)
    val battery: StateFlow<Battery?> = _battery
    private val _signal = MutableStateFlow<Signal>(Signal.Quiet)
    val signal: StateFlow<Signal> = _signal

    private val telephony: TelephonyManager? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) context.getSystemService(TelephonyManager::class.java) else null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = readBattery(i)
    }

    private var callback: Any? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        // Sticky: registering hands back the current state straight away.
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let(::readBattery)
        startSignal()
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { context.unregisterReceiver(batteryReceiver) }
        stopSignal()
    }

    private fun readBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        if (level < 0) return
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        _battery.value = Battery(level * 100 / scale, plugged)
    }

    private fun startSignal() {
        val tm = telephony ?: return
        if (Build.VERSION.SDK_INT >= 31) {
            val cb = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener, TelephonyCallback.ServiceStateListener {
                override fun onSignalStrengthsChanged(s: SignalStrength) { strength = s; publishSignal() }
                override fun onServiceStateChanged(s: ServiceState) { service = s.state; publishSignal() }
            }
            runCatching { tm.registerTelephonyCallback(context.mainExecutor, cb) }.onSuccess { callback = cb }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onSignalStrengthsChanged(s: SignalStrength) { strength = s; publishSignal() }
                @Deprecated("Deprecated in Java")
                override fun onServiceStateChanged(s: ServiceState) { service = s.state; publishSignal() }
            }
            @Suppress("DEPRECATION")
            runCatching { tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE) }
                .onSuccess { callback = listener }
        }
    }

    private fun stopSignal() {
        val tm = telephony ?: return
        // TelephonyCallback doesn't exist before Android 12: even a type check on it would fail there.
        if (Build.VERSION.SDK_INT >= 31) {
            (callback as? TelephonyCallback)?.let { runCatching { tm.unregisterTelephonyCallback(it) } }
        } else {
            @Suppress("DEPRECATION")
            (callback as? PhoneStateListener)?.let { runCatching { tm.listen(it, PhoneStateListener.LISTEN_NONE) } }
        }
        callback = null
    }

    @Volatile private var strength: SignalStrength? = null
    @Volatile private var service: Int = ServiceState.STATE_IN_SERVICE

    /**
     * Service state decides "no service" (the radio can still report old signal
     * bars after losing the network); otherwise the bars count.
     */
    private fun publishSignal() {
        val tm = telephony
        val airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        val s = strength
        _signal.value = when {
            tm == null || airplane || tm.simState != TelephonyManager.SIM_STATE_READY || service == ServiceState.STATE_POWER_OFF -> Signal.Quiet
            service == ServiceState.STATE_OUT_OF_SERVICE || service == ServiceState.STATE_EMERGENCY_ONLY -> Signal.NoService
            s == null -> Signal.Quiet
            // No cell measured at all: out of range too.
            Build.VERSION.SDK_INT >= 29 && s.cellSignalStrengths.isEmpty() -> Signal.NoService
            else -> Signal.Bars(s.level.coerceIn(0, 4))
        }
    }
}
