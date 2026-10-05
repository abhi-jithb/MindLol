package com.mindlol.probe

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.mindlol.probe.detector.DetectionResult
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NodeTreeDumper {
    private const val TAG = "TreeLoggerService"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun dumpEventAndTree(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        logFile: File?,
        detectionResult: DetectionResult? = null
    ) {
        val timestamp = dateFormat.format(Date())
        val packageName = event.packageName?.toString() ?: "unknown"
        val eventTypeStr = AccessibilityEvent.eventTypeToString(event.eventType)
        val eventClassName = event.className?.toString() ?: "null"
        val fromIndex = event.fromIndex
        val toIndex = event.toIndex
        val scrollX = event.scrollX
        val scrollY = event.scrollY

        val header = StringBuilder().apply {
            append("========================================================================\n")
            append("TIMESTAMP: $timestamp\n")
            append("PACKAGE: $packageName\n")
            append("EVENT_TYPE: $eventTypeStr\n")
            append("EVENT_CLASS: $eventClassName\n")
            append("EVENT_INDEXES: fromIndex=$fromIndex, toIndex=$toIndex\n")
            append("EVENT_SCROLL: scrollX=$scrollX, scrollY=$scrollY\n")
            detectionResult?.let {
                append("DETECTION_RESULT: isReel=${it.isReel}, confidence=${it.confidence}, author=${it.authorName}, activeBounds=${it.activeReelBounds}, evidence=${it.evidence}\n")
            }
            append("------------------------------------------------------------------------\n")
        }.toString()

        Log.d(TAG, header)

        val treeBuilder = StringBuilder()
        treeBuilder.append(header)

        if (rootNode == null) {
            val emptyMsg = "ROOT NODE IS NULL (Unable to retrieve active window root)\n"
            Log.w(TAG, emptyMsg)
            treeBuilder.append(emptyMsg)
        } else {
            dumpNodeRecursive(rootNode, depth = 0, builder = treeBuilder)
        }

        treeBuilder.append("========================================================================\n\n")

        logFile?.let { file ->
            try {
                FileWriter(file, true).use { writer ->
                    writer.write(treeBuilder.toString())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing tree dump to log file: ${e.message}", e)
            }
        }
    }

    private fun dumpNodeRecursive(node: AccessibilityNodeInfo, depth: Int, builder: StringBuilder) {
        val indent = "  ".repeat(depth)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val rawResId = node.viewIdResourceName
        val resIdStr = when {
            rawResId == null -> "null"
            rawResId.contains("0_resource_name_obfuscated") -> "OBFUSCATED ($rawResId)"
            else -> rawResId
        }

        val className = node.className?.toString() ?: "null"
        val textStr = sanitizeString(node.text?.toString())
        val contentDescStr = sanitizeString(node.contentDescription?.toString())
        val isVisible = node.isVisibleToUser
        val isScrollable = node.isScrollable
        val childCount = node.childCount

        val nodeLine = String.format(
            Locale.US,
            "%s[%s] id=%s | text=\"%s\" | desc=\"%s\" | bounds=%s | visible=%b | scrollable=%b | children=%d\n",
            indent,
            className,
            resIdStr,
            textStr,
            contentDescStr,
            bounds.toShortString(),
            isVisible,
            isScrollable,
            childCount
        )

        Log.d(TAG, nodeLine.trimEnd())
        builder.append(nodeLine)

        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                dumpNodeRecursive(child, depth + 1, builder)
                child.recycle()
            }
        }
    }

    private fun sanitizeString(input: String?): String {
        if (input == null) return "null"
        return input.replace("\n", "\\n").replace("\r", "\\r").take(100)
    }
}
