package com.asnidev.kamei.system

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/** The camera flash as a torch. Needs no permission; the system tells us when anything else toggles it. */
class Torch(context: Context) {

    private val cameras = context.getSystemService(CameraManager::class.java)
    private val flashId: String? = runCatching {
        cameras?.cameraIdList?.firstOrNull { cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()

    @Volatile
    private var on = false

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashId) on = enabled
        }
    }

    init {
        if (flashId != null) runCatching { cameras?.registerTorchCallback(callback, Handler(Looper.getMainLooper())) }
    }

    val available: Boolean get() = flashId != null

    /** False when the phone has no flash, or the camera is in use. */
    fun toggle(): Boolean {
        val id = flashId ?: return false
        return runCatching { cameras?.setTorchMode(id, !on); true }
            .onFailure { Log.w("Torch", "couldn't switch the torch", it) }.getOrDefault(false)
    }

    fun close() {
        runCatching { cameras?.unregisterTorchCallback(callback) }
    }
}
