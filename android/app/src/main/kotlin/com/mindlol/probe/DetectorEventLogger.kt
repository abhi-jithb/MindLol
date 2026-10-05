package com.mindlol.probe

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.mindlol.probe.counter.CounterResult
import com.mindlol.probe.detector.DetectionResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Appends chronological compact detection and counter decision records to dedicated log files.
 */
object DetectorEventLogger {

    private const val TAG = "DetectorEventLogger"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun logDetectorEvent(
        event: AccessibilityEvent,
        detectionResult: DetectionResult,
        logFile: File?
    ) {
        if (logFile == null) return

        val timestamp = dateFormat.format(Date())
        val eventType = AccessibilityEvent.eventTypeToString(event.eventType)
        val eventClass = event.className?.toString() ?: "null"

        val line = String.format(
            Locale.US,
            "[%s] event=%s | eventClass=%s | isReel=%b | evidence=%s | pagerBounds=%s | activeBounds=%s | hasAuthor=%b\n",
            timestamp,
            eventType,
            eventClass,
            detectionResult.isReel,
            detectionResult.evidence,
            detectionResult.pagerBounds?.toShortString() ?: "null",
            detectionResult.activeReelBounds?.toShortString() ?: "null",
            detectionResult.authorName != null
        )

        try {
            FileWriter(logFile, true).use { writer ->
                writer.write(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to detector_events.log: ${e.message}", e)
        }
    }

    fun logCounterEvent(
        event: AccessibilityEvent,
        counterResult: CounterResult,
        isReel: Boolean,
        logFile: File?
    ) {
        if (logFile == null) return

        val timestamp = dateFormat.format(Date())
        val eventClass = event.className?.toString() ?: "null"

        val prevIndex = counterResult.previousIndex
        val currIndex = counterResult.currentIndex
        val direction = counterResult.direction.name
        val isCounted = counterResult.isCounted
        val sessionCount = counterResult.sessionCount
        val evidence = counterResult.evidence

        val line = String.format(
            Locale.US,
            "[%s] index=%d -> %d | direction=%s | isReel=%b | counted=%b | sessionCount=%d | eventClass=%s | evidence=%s\n",
            timestamp,
            prevIndex,
            currIndex,
            direction,
            isReel,
            isCounted,
            sessionCount,
            eventClass,
            evidence
        )

        Log.d(TAG, line.trimEnd())

        try {
            FileWriter(logFile, true).use { writer ->
                writer.write(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to counter_events.log: ${e.message}", e)
        }
    }
}
