package com.mindlol.probe

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mindlol.probe.counter.CounterResult
import com.mindlol.probe.counter.InstagramReelCounter
import com.mindlol.probe.counter.ReelObservation
import com.mindlol.probe.detector.DetectionResult
import com.mindlol.probe.detector.InstagramReelDetector
import java.io.File

class DebugProbeService : AccessibilityService() {

    private var logFile: File? = null
    private var detectorLogFile: File? = null
    private var lastEventTimeMs = 0L

    private val reelCounter = InstagramReelCounter()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "DebugProbeService connected and initialized.")

        try {
            val externalDir = getExternalFilesDir(null) ?: filesDir
            logFile = File(externalDir, "tree_dump.log")
            detectorLogFile = File(externalDir, "detector_events.log")
            Log.i(TAG, "Log file locations: tree_dump=${logFile?.absolutePath}, detector_events=${detectorLogFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize log file output", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg != INSTAGRAM_PKG && pkg != YOUTUBE_PKG) return

        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && (now - lastEventTimeMs) < CONTENT_CHANGE_DEBOUNCE_MS) {
            return
        }
        lastEventTimeMs = now

        val rootNode = rootInActiveWindow

        var detectionResult: DetectionResult? = null
        var counterResult: CounterResult? = null

        if (pkg == INSTAGRAM_PKG && rootNode != null) {
            val detected = InstagramReelDetector.detect(rootNode)
            detectionResult = detected

            val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)
            val eventClassStr = event.className?.toString() ?: ""

            val obs = ReelObservation(
                isReel = detected.isReel,
                eventType = eventTypeStr,
                eventClass = eventClassStr,
                fromIndex = event.fromIndex,
                toIndex = event.toIndex
            )

            counterResult = reelCounter.processObservation(obs)
            DetectorEventLogger.logEvent(event, detected, counterResult, detectorLogFile)
        }

        NodeTreeDumper.dumpEventAndTree(event, rootNode, logFile, detectionResult)

        rootNode?.recycle()
    }

    override fun onInterrupt() {
        Log.w(TAG, "DebugProbeService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DebugProbeService destroyed.")
    }

    companion object {
        private const val TAG = "TreeLoggerService"
        private const val INSTAGRAM_PKG = "com.instagram.android"
        private const val YOUTUBE_PKG = "com.google.android.youtube"
        private const val CONTENT_CHANGE_DEBOUNCE_MS = 250L
    }
}
