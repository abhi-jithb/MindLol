package com.mindlol.probe

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mindlol.probe.detector.DetectionResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Appends chronological compact detection records to `detector_events.log`.
 *
 * Preserves exact chronological ordering without sorting, deduplication, or aggregation
 * to capture repeated detections and transition states on physical hardware.
 */
object DetectorEventLogger {

    private const val TAG = "DetectorEventLogger"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun logEvent(event: AccessibilityEvent, result: DetectionResult, logFile: File?) {
        if (logFile == null) return

        val timestamp = dateFormat.format(Date())
        val eventType = AccessibilityEvent.eventTypeToString(event.eventType)
        val eventClass = event.className?.toString() ?: "null"
        val scrollX = event.scrollX
        val scrollY = event.scrollY
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex
        val itemCount = event.itemCount

        val pagerBoundsStr = result.pagerBounds?.toShortString() ?: "null"
        val activeBoundsStr = result.activeReelBounds?.toShortString() ?: "null"
        val authorStr = result.authorName ?: "null"

        val line = String.format(
            Locale.US,
            "[%s] event=%s | isReel=%b | evidence=%s | pagerBounds=%s | activeBounds=%s | author=%s | eventClass=%s | scrollX=%d | scrollY=%d | fromIndex=%d | toIndex=%d | itemCount=%d\n",
            timestamp,
            eventType,
            result.isReel,
            result.evidence,
            pagerBoundsStr,
            activeBoundsStr,
            authorStr,
            eventClass,
            scrollX,
            scrollY,
            fromIndex,
            toIndex,
            itemCount
        )

        Log.d(TAG, line.trimEnd())

        try {
            FileWriter(logFile, true).use { writer ->
                writer.write(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing detector event to log file: ${e.message}", e)
        }
    }
}
