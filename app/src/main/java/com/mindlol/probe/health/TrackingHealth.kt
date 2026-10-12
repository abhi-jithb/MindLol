package com.mindlol.probe.health

import com.mindlol.probe.StatusBroadcaster

enum class TrackingHealthState(val key: String) {
    TRACKING("TRACKING"),
    PERMISSION_REQUIRED("PERMISSION_REQUIRED"),
    STANDBY("STANDBY"),
    TRACKING_UNCERTAIN("TRACKING_UNCERTAIN"),
    TRACKING_PAUSED("TRACKING_PAUSED")
}

data class TrackingHealth(
    val state: TrackingHealthState,
    val activePackageName: String?,
    val isShortVideoSurface: Boolean,
    val title: String,
    val message: String,
    val actionRequired: String? = null
)

/**
 * Thread-safe manager coordinating detector observation signals, OS permissions,
 * and service lifecycle to determine truthful tracking-health states.
 */
object TrackingHealthManager {

    @Volatile
    var isServiceConnected: Boolean = false
        internal set

    @Volatile
    var activePackageName: String? = null
        internal set

    @Volatile
    var isShortVideoSurface: Boolean = false
        internal set

    @Volatile
    var lastDetectionEvidence: String? = null
        internal set

    fun onServiceConnected() {
        isServiceConnected = true
        StatusBroadcaster.notifyStatusChanged()
    }

    fun onServiceDisconnected() {
        isServiceConnected = false
        isShortVideoSurface = false
        activePackageName = null
        lastDetectionEvidence = null
        StatusBroadcaster.notifyStatusChanged()
    }

    fun updateSurfaceState(packageName: String?, isSurfaceActive: Boolean, evidence: String?) {
        val changed = activePackageName != packageName ||
                isShortVideoSurface != isSurfaceActive ||
                lastDetectionEvidence != evidence

        activePackageName = packageName
        isShortVideoSurface = isSurfaceActive
        lastDetectionEvidence = evidence

        if (changed) {
            StatusBroadcaster.notifyStatusChanged()
        }
    }

    fun resolveHealth(
        isAccessibilityServiceEnabled: Boolean,
        isOverlayPermissionGranted: Boolean,
        isInstagramEnabled: Boolean,
        isYouTubeEnabled: Boolean
    ): TrackingHealth {
        // 1. Permission checks
        if (!isAccessibilityServiceEnabled || !isServiceConnected) {
            return TrackingHealth(
                state = TrackingHealthState.PERMISSION_REQUIRED,
                activePackageName = activePackageName,
                isShortVideoSurface = false,
                title = "Accessibility permission needed",
                message = "MindLol needs Accessibility permission to detect Reels and Shorts on your device.",
                actionRequired = "OPEN_ACCESSIBILITY_SETTINGS"
            )
        }

        if (!isOverlayPermissionGranted) {
            return TrackingHealth(
                state = TrackingHealthState.PERMISSION_REQUIRED,
                activePackageName = activePackageName,
                isShortVideoSurface = false,
                title = "Overlay permission needed",
                message = "MindLol needs Display over other apps permission to enforce your daily limit.",
                actionRequired = "OPEN_OVERLAY_SETTINGS"
            )
        }

        // 2. Apps disabled check
        if (!isInstagramEnabled && !isYouTubeEnabled) {
            return TrackingHealth(
                state = TrackingHealthState.TRACKING_PAUSED,
                activePackageName = activePackageName,
                isShortVideoSurface = false,
                title = "Tracking paused",
                message = "Tracking is paused because all supported apps are turned off in Settings.",
                actionRequired = "OPEN_SETTINGS"
            )
        }

        // 3. Active tracking on verified short-form video surface
        if (isShortVideoSurface) {
            val appName = if (activePackageName?.contains("youtube") == true) "YouTube Shorts" else "Instagram Reels"
            return TrackingHealth(
                state = TrackingHealthState.TRACKING,
                activePackageName = activePackageName,
                isShortVideoSurface = true,
                title = "Actively tracking",
                message = "Currently monitoring $appName.",
                actionRequired = null
            )
        }

        // 4. Target app active in foreground, but surface detection is uncertain
        val isTargetApp = activePackageName == "com.instagram.android" || activePackageName == "com.google.android.youtube"
        val evidence = lastDetectionEvidence ?: ""
        if (isTargetApp && (evidence.contains("UNCERTAIN") || evidence.contains("OBFUSCATED") || evidence.contains("INVALID_PAGER_BOUNDS"))) {
            val appName = if (activePackageName?.contains("youtube") == true) "YouTube" else "Instagram"
            return TrackingHealth(
                state = TrackingHealthState.TRACKING_UNCERTAIN,
                activePackageName = activePackageName,
                isShortVideoSurface = false,
                title = "Tracking uncertain",
                message = "MindLol cannot confirm detection on this $appName screen. Your count is preserved.",
                actionRequired = null
            )
        }

        // 5. Standby: permissions are ready, user is on home screen, MindLol, or non-short surface
        return TrackingHealth(
            state = TrackingHealthState.STANDBY,
            activePackageName = activePackageName,
            isShortVideoSurface = false,
            title = "Monitoring ready",
            message = "Tracking activates automatically when you open Reels or Shorts.",
            actionRequired = null
        )
    }

    /**
     * Resets internal state for unit test isolation.
     */
    fun resetForTesting() {
        isServiceConnected = false
        activePackageName = null
        isShortVideoSurface = false
        lastDetectionEvidence = null
    }
}
