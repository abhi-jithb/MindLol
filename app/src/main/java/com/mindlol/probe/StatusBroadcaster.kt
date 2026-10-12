package com.mindlol.probe

import android.os.Handler
import android.os.Looper

/**
 * Thread-safe listener interface for global status changes (count increments, limit updates).
 */
fun interface StatusChangeListener {
    fun onStatusChanged()
}

/**
 * Global status change broadcaster connecting native enforcement events (DebugProbeService)
 * with UI layers (Flutter EventChannel, Floating HUD Manager).
 */
object StatusBroadcaster {

    private val listeners = mutableListOf<StatusChangeListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun addListener(listener: StatusChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    @Synchronized
    fun removeListener(listener: StatusChangeListener) {
        listeners.remove(listener)
    }

    fun notifyStatusChanged() {
        mainHandler.post {
            val targets: List<StatusChangeListener>
            synchronized(this) {
                targets = listeners.toList()
            }
            for (listener in targets) {
                try {
                    listener.onStatusChanged()
                } catch (e: Throwable) {
                    // Prevent listener exceptions from breaking event loop
                }
            }
        }
    }
}
