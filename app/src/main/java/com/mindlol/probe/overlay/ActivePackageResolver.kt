package com.mindlol.probe.overlay

/**
 * Pure helper for resolving the active foreground application package name across accessibility events.
 *
 * Prevents system framework background events (SystemUI, Android OS, self-events) from prematurely
 * overwriting or clearing the active target application state.
 */
object ActivePackageResolver {

    /**
     * Resolves the active foreground application package name.
     *
     * Rules:
     * - If [eventPackageName] is null or blank, return [currentActivePackageName].
     * - If [eventPackageName] is a system framework or self package ("android", "com.android.systemui", [selfPackageName]),
     *   preserve and return [currentActivePackageName].
     * - Otherwise, update and return [eventPackageName].
     */
    fun resolveActivePackage(
        eventPackageName: String?,
        currentActivePackageName: String?,
        selfPackageName: String
    ): String? {
        if (eventPackageName.isNullOrBlank()) return currentActivePackageName
        if (eventPackageName == "android" ||
            eventPackageName == "com.android.systemui" ||
            eventPackageName == selfPackageName
        ) {
            return currentActivePackageName
        }
        return eventPackageName
    }
}
