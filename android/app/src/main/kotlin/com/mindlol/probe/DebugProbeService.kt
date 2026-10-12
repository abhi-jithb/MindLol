package com.mindlol.probe

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindlol.probe.counter.InstagramReelCounter
import com.mindlol.probe.counter.ReelObservation
import com.mindlol.probe.counter.ShortsObservation
import com.mindlol.probe.counter.YouTubeShortsCounter
import com.mindlol.probe.detector.DetectionResult
import com.mindlol.probe.detector.InstagramReelDetector
import com.mindlol.probe.detector.YouTubeShortsDetector
import com.mindlol.probe.overlay.BlockOverlayManager
import com.mindlol.probe.repository.ReelCounterRepository
import com.mindlol.probe.repository.ReelCounterRepositoryImpl
import com.mindlol.probe.repository.SharedPreferencesStore
import java.io.File

class DebugProbeService : AccessibilityService() {

    private var logFile: File? = null
    private var detectorLogFile: File? = null
    private var counterLogFile: File? = null
    private var lastEventTimeMs = 0L

    private val reelCounter = InstagramReelCounter()
    private val shortsCounter = YouTubeShortsCounter()
    private lateinit var repository: ReelCounterRepository
    private lateinit var overlayManager: BlockOverlayManager
    private lateinit var floatingHudManager: com.mindlol.probe.overlay.FloatingCountHudManager
    private var currentActivePackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "DebugProbeService connected and initialized.")

        try {
            val externalDir = getExternalFilesDir(null) ?: filesDir
            logFile = File(externalDir, "tree_dump.log")
            detectorLogFile = File(externalDir, "detector_events.log")
            counterLogFile = File(externalDir, "counter_events.log")
            repository = ReelCounterRepositoryImpl(SharedPreferencesStore(this))
            overlayManager = BlockOverlayManager(this)
            floatingHudManager = com.mindlol.probe.overlay.FloatingCountHudManager(this, repository)
            com.mindlol.probe.health.TrackingHealthManager.onServiceConnected()
            Log.i(TAG, "Log files initialized: tree_dump=${logFile?.absolutePath}, detector_events=${detectorLogFile?.absolutePath}, counter_events=${counterLogFile?.absolutePath}")
            Log.i(TAG, "Repository initialized: todayCount=${repository.getTodayCount()}, dailyLimit=${repository.getDailyLimit()}, limitReached=${repository.isLimitReached()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize log file outputs, repository, overlay manager, or HUD manager", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rawEventPkg = event.packageName?.toString() ?: ""
        val activePkg = resolveActivePackage(event)
        val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)

        // Update overlay and HUD decision state
        if (::repository.isInitialized && ::overlayManager.isInitialized) {
            val isLimitReached = repository.isLimitReached()
            val appEnabled = isAppEnabled(activePkg)
            val isTarget = activePkg != null &&
                activePkg in com.mindlol.probe.overlay.BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES &&
                appEnabled

            val effectivePkg = if (appEnabled) activePkg else null
            overlayManager.updateOverlayState(effectivePkg, isLimitReached)

            // Hide HUD immediately if user leaves target packages or app is disabled
            if (!isTarget && ::floatingHudManager.isInitialized) {
                floatingHudManager.updateHudVisibility(
                    activePackageName = effectivePkg,
                    isShortVideoSurface = false,
                    isLimitReached = isLimitReached,
                    isTestUnlocked = overlayManager.isTestUnlocked
                )
            }
        }

        if (rawEventPkg == "android" || rawEventPkg == "com.android.systemui" || rawEventPkg == packageName) {
            // Transient system framework or MindLol HUD/overlay event; do not drop target surface state
            return
        }

        if (!isAppEnabled(rawEventPkg)) {
            com.mindlol.probe.health.TrackingHealthManager.updateSurfaceState(activePkg, false, "NON_TARGET_APP")
            return
        }

        val eventClassStr = event.className?.toString() ?: ""

        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && (now - lastEventTimeMs) < CONTENT_CHANGE_DEBOUNCE_MS) {
            return
        }
        lastEventTimeMs = now

        val rootNode = getBestRootNode(event)
        try {
            val rootClassStr = rootNode?.className?.toString() ?: "null"

            Log.d(
                TAG,
                "[Pipeline 1-4] Event=$eventTypeStr | pkg=$rawEventPkg | eventClass=$eventClassStr | rootClass=$rootClassStr"
            )

            var detectionResult: DetectionResult? = null

            if (rawEventPkg == INSTAGRAM_PKG) {
                val detected = InstagramReelDetector.detect(rootNode)
                detectionResult = detected
                com.mindlol.probe.health.TrackingHealthManager.updateSurfaceState(rawEventPkg, detected.isReel, detected.evidence)

                Log.d(
                    TAG,
                    "[Pipeline 5-7] isReel=${detected.isReel} | evidence=${detected.evidence} | pagerBounds=${detected.pagerBounds?.toShortString()} | activeBounds=${detected.activeReelBounds?.toShortString()}"
                )

                if (::floatingHudManager.isInitialized && ::repository.isInitialized) {
                    floatingHudManager.updateHudVisibility(
                        activePackageName = activePkg,
                        isShortVideoSurface = detected.isReel,
                        isLimitReached = repository.isLimitReached(),
                        isTestUnlocked = overlayManager.isTestUnlocked
                    )
                }

                val isCommentsOpen = InstagramReelDetector.isCommentsOpen(rootNode)
                val obs = ReelObservation(
                    isReel = detected.isReel,
                    eventType = eventTypeStr,
                    eventClass = eventClassStr,
                    fromIndex = event.fromIndex,
                    toIndex = event.toIndex,
                    isCommentsOpen = isCommentsOpen
                )

                val counterResult = reelCounter.processObservation(obs)

                if (counterResult.isCounted && ::repository.isInitialized) {
                    repository.incrementCount()
                    // Re-evaluate overlay and HUD state immediately upon new increment
                    if (::overlayManager.isInitialized) {
                        overlayManager.updateOverlayState(activePkg, repository.isLimitReached())
                    }
                    if (::floatingHudManager.isInitialized) {
                        floatingHudManager.updateHudVisibility(
                            activePackageName = activePkg,
                            isShortVideoSurface = detected.isReel,
                            isLimitReached = repository.isLimitReached(),
                            isTestUnlocked = overlayManager.isTestUnlocked
                        )
                    }
                }

                val todayCount = if (::repository.isInitialized) repository.getTodayCount() else 0
                val dailyLimit = if (::repository.isInitialized) repository.getDailyLimit() else 0
                val limitReached = if (::repository.isInitialized) repository.isLimitReached() else false

                Log.d(
                    TAG,
                    "[Pipeline 8-10] Obs(isReel=${obs.isReel}, idx=${obs.fromIndex}->${obs.toIndex}, commentsOpen=$isCommentsOpen) -> CounterResult(isCounted=${counterResult.isCounted}, sessionCount=${counterResult.sessionCount}, todayCount=$todayCount/$dailyLimit, limitReached=$limitReached, dir=${counterResult.direction}, evidence=${counterResult.evidence})"
                )

                if (BuildConfig.DEBUG) {
                    DetectorEventLogger.logDetectorEvent(event, detected, detectorLogFile)
                    DetectorEventLogger.logCounterEvent(event, counterResult, detected.isReel, counterLogFile)
                    Log.d(TAG, "[Pipeline 11] Recorded detection and counter events to debug log files.")
                }
            } else if (rawEventPkg == YOUTUBE_PKG) {
                val detected = com.mindlol.probe.detector.YouTubeShortsDetector.detect(rootNode)
                detectionResult = detected
                com.mindlol.probe.health.TrackingHealthManager.updateSurfaceState(rawEventPkg, detected.isReel, detected.evidence)

                Log.d(
                    TAG,
                    "[YouTube Pipeline 5-7] isShorts=${detected.isReel} | evidence=${detected.evidence} | bounds=${detected.activeReelBounds?.toShortString()}"
                )

                if (::floatingHudManager.isInitialized && ::repository.isInitialized) {
                    floatingHudManager.updateHudVisibility(
                        activePackageName = activePkg,
                        isShortVideoSurface = detected.isReel,
                        isLimitReached = repository.isLimitReached(),
                        isTestUnlocked = overlayManager.isTestUnlocked
                    )
                }

                val isCommentsOpen = YouTubeShortsDetector.isCommentsOpen(rootNode)
                val obs = ShortsObservation(
                    isShorts = detected.isReel,
                    eventType = eventTypeStr,
                    eventClass = eventClassStr,
                    scrollX = event.scrollX,
                    scrollY = event.scrollY,
                    isCommentsOpen = isCommentsOpen,
                    timestampMs = System.currentTimeMillis()
                )

                val counterResult = shortsCounter.processObservation(obs)

                if (counterResult.isCounted && ::repository.isInitialized) {
                    repository.incrementCount()
                    // Re-evaluate overlay and HUD state immediately upon new increment
                    if (::overlayManager.isInitialized) {
                        overlayManager.updateOverlayState(activePkg, repository.isLimitReached())
                    }
                    if (::floatingHudManager.isInitialized) {
                        floatingHudManager.updateHudVisibility(
                            activePackageName = activePkg,
                            isShortVideoSurface = detected.isReel,
                            isLimitReached = repository.isLimitReached(),
                            isTestUnlocked = overlayManager.isTestUnlocked
                        )
                    }
                }

                val todayCount = if (::repository.isInitialized) repository.getTodayCount() else 0
                val dailyLimit = if (::repository.isInitialized) repository.getDailyLimit() else 0
                val limitReached = if (::repository.isInitialized) repository.isLimitReached() else false

                Log.d(
                    TAG,
                    "[YouTube Pipeline 8-10] Obs(isShorts=${obs.isShorts}, scrollY=${obs.scrollY}) -> CounterResult(isCounted=${counterResult.isCounted}, sessionCount=${counterResult.sessionCount}, todayCount=$todayCount/$dailyLimit, limitReached=$limitReached, dir=${counterResult.direction}, evidence=${counterResult.evidence})"
                )

                if (BuildConfig.DEBUG) {
                    DetectorEventLogger.logDetectorEvent(event, detected, detectorLogFile)
                    DetectorEventLogger.logCounterEvent(event, counterResult, detected.isReel, counterLogFile)
                    Log.d(TAG, "[YouTube Pipeline 11] Recorded detection and counter events to debug log files.")
                }
            }

            if (BuildConfig.DEBUG) {
                NodeTreeDumper.dumpEventAndTree(event, rootNode, logFile, detectionResult)
            }
        } finally {
            try {
                rootNode?.recycle()
            } catch (_: Throwable) {
                // Ignore safe recycling exception
            }
        }
    }

    private fun getBestRootNode(event: AccessibilityEvent): AccessibilityNodeInfo? {
        return try {
            val targetPkg = event.packageName?.toString()
            val activeRoot = rootInActiveWindow
            if (activeRoot != null) {
                val rootPkg = activeRoot.packageName?.toString()
                if (rootPkg == targetPkg || rootPkg == "android" || rootPkg == INSTAGRAM_PKG || rootPkg == YOUTUBE_PKG) {
                    return activeRoot
                }
                // activeRoot does not match target package, recycle it before falling back
                try {
                    activeRoot.recycle()
                } catch (_: Throwable) {}
            }

            // Fallback to event source node hierarchy if active window root is from SystemUI or null
            var current: AccessibilityNodeInfo = event.source ?: return null
            while (true) {
                val parent = current.parent ?: break
                try {
                    current.recycle()
                } catch (_: Throwable) {}
                current = parent
            }
            current
        } catch (e: Throwable) {
            Log.w(TAG, "Safely caught exception while acquiring root node: ${e.message}")
            null
        }
    }

    private fun resolveActivePackage(event: AccessibilityEvent): String? {
        val resolved = com.mindlol.probe.overlay.ActivePackageResolver.resolveActivePackage(
            eventPackageName = event.packageName?.toString(),
            currentActivePackageName = currentActivePackageName,
            selfPackageName = packageName
        )
        currentActivePackageName = resolved
        return resolved
    }

    private fun isAppEnabled(pkgName: String?): Boolean {
        if (!::repository.isInitialized || pkgName == null) return false
        return when (pkgName) {
            INSTAGRAM_PKG -> repository.isInstagramEnabled()
            YOUTUBE_PKG -> repository.isYouTubeEnabled()
            else -> false
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "DebugProbeService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DebugProbeService destroyed.")
        if (::overlayManager.isInitialized) {
            overlayManager.hideOverlay()
        }
        if (::floatingHudManager.isInitialized) {
            floatingHudManager.cleanup()
        }
        com.mindlol.probe.health.TrackingHealthManager.onServiceDisconnected()
    }

    companion object {
        private const val TAG = "TreeLoggerService"
        private const val INSTAGRAM_PKG = "com.instagram.android"
        private const val YOUTUBE_PKG = "com.google.android.youtube"
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 250L
    }
}
