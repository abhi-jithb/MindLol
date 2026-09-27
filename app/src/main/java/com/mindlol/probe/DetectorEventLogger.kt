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
 * Appends chronological compact detection and counter decision records to `detector_events.log`.
 */
object DetectorEventLogger {

    private const val TAG = "DetectorEventLogger"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun logEvent(
        event: AccessibilityEvent,
        detectionResult: DetectionResult,
        counterResult: CounterResult?,
        logFile: File?
    ) {
        if (logFile == null) return

        val timestamp = dateFormat.format(Date())
        val eventType = AccessibilityEvent.eventTypeToString(event.eventType)
        val eventClass = event.className?.toString() ?: "null"

        val isReel = detectionResult.isReel
        val isCounted = counterResult?.isCounted ?: false
        val sessionCount = counterResult?.sessionCount ?: 0
        val direction = counterResult?.direction?.name ?: "NONE"
        val prevIndex = counterResult?.previousIndex ?: -1
        val currIndex = counterResult?.currentIndex ?: -1
        val evidence = counterResult?.evidence ?: detectionResult.evidence

        val line = String.format(
            Locale.US,
            "[%s] event=%s | eventClass=%s | isReel=%b | index=%d -> %d | direction=%s | counted=%b | sessionCount=%d | evidence=%s\n",
            timestamp,
            eventType,
            eventClass,
            isReel,
            prevIndex,
            currIndex,
            direction,
            isCounted,
            sessionCount,
            evidence
        )

        Log.d(TAG, line.trimEnd())

        try {
            FileWriter(logFile, true).use { writer ->
                writer.write(line)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to detector_events.log: ${e.message}", e)
        }
    }
}
