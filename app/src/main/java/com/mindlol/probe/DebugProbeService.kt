package com.mindlol.probe

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindlol.probe.counter.InstagramReelCounter
import com.mindlol.probe.counter.ReelObservation
import com.mindlol.probe.detector.DetectionResult
import com.mindlol.probe.detector.InstagramReelDetector
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
    private lateinit var repository: ReelCounterRepository
    private lateinit var overlayManager: BlockOverlayManager
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
            Log.i(TAG, "Log files initialized: tree_dump=${logFile?.absolutePath}, detector_events=${detectorLogFile?.absolutePath}, counter_events=${counterLogFile?.absolutePath}")
            Log.i(TAG, "Repository initialized: todayCount=${repository.getTodayCount()}, dailyLimit=${repository.getDailyLimit()}, limitReached=${repository.isLimitReached()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize log file outputs, repository, or overlay manager", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rawEventPkg = event.packageName?.toString() ?: ""
        val activePkg = resolveActivePackage(event)
        val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)

        // Evaluate overlay decision and state
        if (::repository.isInitialized && ::overlayManager.isInitialized) {
            val isLimitReached = repository.isLimitReached()
            val hasPermission = overlayManager.hasOverlayPermission()
            val isTarget = activePkg != null && activePkg in com.mindlol.probe.overlay.BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES
            val shouldShow = com.mindlol.probe.overlay.BlockOverlayDecisionEngine.shouldShowOverlay(
                activePackageName = activePkg,
                isLimitReached = isLimitReached,
                hasOverlayPermission = hasPermission,
                isTestUnlocked = overlayManager.isTestUnlocked
            )

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || isTarget) {
                Log.d(
                    TAG,
                    "[OVERLAY DEBUG] event=$eventTypeStr | package=$activePkg (raw=$rawEventPkg) | target=$isTarget | limitReached=$isLimitReached | permission=$hasPermission | shouldShow=$shouldShow"
                )
            }

            overlayManager.updateOverlayState(activePkg, isLimitReached)
        }

        if (rawEventPkg != INSTAGRAM_PKG && rawEventPkg != YOUTUBE_PKG) return

        val eventClassStr = event.className?.toString() ?: ""

        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && (now - lastEventTimeMs) < CONTENT_CHANGE_DEBOUNCE_MS) {
            return
        }
        lastEventTimeMs = now

        val activeRoot = rootInActiveWindow
        val rootNode = getBestRootNode(event)
        val rootClassStr = rootNode?.className?.toString() ?: "null"
        val isRootFromActive = (rootNode != null && rootNode == activeRoot)

        Log.d(
            TAG,
            "[Pipeline 1-4] Event=$eventTypeStr | pkg=$rawEventPkg | eventClass=$eventClassStr | activeRoot=${activeRoot != null} | rootClass=$rootClassStr | sourceFallback=${!isRootFromActive}"
        )

        var detectionResult: DetectionResult? = null

        if (rawEventPkg == INSTAGRAM_PKG) {
            val detected = InstagramReelDetector.detect(rootNode)
            detectionResult = detected

            Log.d(
                TAG,
                "[Pipeline 5-7] isReel=${detected.isReel} | evidence=${detected.evidence} | pagerBounds=${detected.pagerBounds?.toShortString()} | activeBounds=${detected.activeReelBounds?.toShortString()}"
            )

            val obs = ReelObservation(
                isReel = detected.isReel,
                eventType = eventTypeStr,
                eventClass = eventClassStr,
                fromIndex = event.fromIndex,
                toIndex = event.toIndex
            )

            val counterResult = reelCounter.processObservation(obs)

            if (counterResult.isCounted && ::repository.isInitialized) {
                repository.incrementCount()
                // Re-evaluate overlay state immediately upon new increment
                if (::overlayManager.isInitialized) {
                    overlayManager.updateOverlayState(activePkg, repository.isLimitReached())
                }
            }

            val todayCount = if (::repository.isInitialized) repository.getTodayCount() else 0
            val dailyLimit = if (::repository.isInitialized) repository.getDailyLimit() else 0
            val limitReached = if (::repository.isInitialized) repository.isLimitReached() else false

            Log.d(
                TAG,
                "[Pipeline 8-10] Obs(isReel=${obs.isReel}, idx=${obs.fromIndex}->${obs.toIndex}) -> CounterResult(isCounted=${counterResult.isCounted}, sessionCount=${counterResult.sessionCount}, todayCount=$todayCount/$dailyLimit, limitReached=$limitReached, dir=${counterResult.direction}, evidence=${counterResult.evidence})"
            )

            if (BuildConfig.DEBUG) {
                DetectorEventLogger.logDetectorEvent(event, detected, detectorLogFile)
                DetectorEventLogger.logCounterEvent(event, counterResult, detected.isReel, counterLogFile)
                Log.d(TAG, "[Pipeline 11] Recorded detection and counter events to debug log files.")
            }
        }

        if (BuildConfig.DEBUG) {
            NodeTreeDumper.dumpEventAndTree(event, rootNode, logFile, detectionResult)
        }

        if (rootNode != null && rootNode != rootInActiveWindow) {
            try {
                rootNode.recycle()
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
            }

            // Fallback to event source node hierarchy if active window root is from SystemUI or null
            val source = event.source ?: return activeRoot
            var current = source
            while (true) {
                val parent = current.parent ?: break
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

    override fun onInterrupt() {
        Log.w(TAG, "DebugProbeService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DebugProbeService destroyed.")
        if (::overlayManager.isInitialized) {
            overlayManager.hideOverlay()
        }
    }

    companion object {
        private const val TAG = "TreeLoggerService"
        private const val INSTAGRAM_PKG = "com.instagram.android"
        private const val YOUTUBE_PKG = "com.google.android.youtube"
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 250L
    }
}
