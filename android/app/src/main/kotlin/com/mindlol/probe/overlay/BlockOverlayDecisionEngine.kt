package com.mindlol.probe.overlay

/**
 * Pure decision engine governing whether the blocking overlay should be displayed.
 *
 * Fully decoupled from Android WindowManager framework calls for clean JVM unit testability.
 */
object BlockOverlayDecisionEngine {

    val DEFAULT_TARGET_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube"
    )

    /**
     * Determines whether the blocking overlay should be currently visible on screen.
     *
     * Rules:
     * - Must have system overlay permission (`SYSTEM_ALERT_WINDOW`).
     * - Must NOT be temporarily test-unlocked for the current session.
     * - Daily limit MUST be reached (`isLimitReached == true`).
     * - Active foreground package MUST be a supported target app (Instagram or YouTube Shorts).
     * - Unrelated packages (Chrome, Launcher, MindLol itself) produce `false`.
     */
    fun shouldShowOverlay(
        activePackageName: String?,
        isLimitReached: Boolean,
        hasOverlayPermission: Boolean,
        isTestUnlocked: Boolean,
        targetPackages: Set<String> = DEFAULT_TARGET_PACKAGES
    ): Boolean {
        if (!hasOverlayPermission) return false
        if (isTestUnlocked) return false
        if (!isLimitReached) return false
        if (activePackageName.isNullOrBlank() || activePackageName !in targetPackages) return false
        return true
    }
}
