package com.mindlol.probe.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Production-oriented detector for YouTube Shorts short-video surfaces.
 *
 * Evaluates accessibility tree structures exposed by `com.google.android.youtube`
 * to determine whether an active, full-screen YouTube Shorts player is visible.
 *
 * Strictly stateless and read-only.
 *
 * Ground truth from real-device captures (YouTube 21.40.161 on Android 16):
 * - Shorts Root: `com.google.android.youtube:id/reel_watch_fragment_root`
 * - Feed Container: `com.google.android.youtube:id/reel_recycler`
 * - Page Container: `com.google.android.youtube:id/reel_player_page_container`
 * - Video Surface: `com.google.android.youtube:id/reel_player_underlay` with `SurfaceView`
 * - Action Column: `reel_player_overlay_container` with like/comment/share/remix buttons
 */
object YouTubeShortsDetector {

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val SHORTS_FRAGMENT_ROOT_ID = "com.google.android.youtube:id/reel_watch_fragment_root"
    const val SHORTS_RECYCLER_ID = "com.google.android.youtube:id/reel_recycler"
    const val SHORTS_PAGE_CONTAINER_ID = "com.google.android.youtube:id/reel_player_page_container"
    const val SHORTS_OVERLAY_CONTAINER_ID = "com.google.android.youtube:id/reel_player_overlay_container"
    const val ENGAGEMENT_PANEL_ID = "com.google.android.youtube:id/app_engagement_panel"
    const val ENGAGEMENT_PANEL_WRAPPER_ID = "com.google.android.youtube:id/app_engagement_panel_wrapper"
    const val ENGAGEMENT_PANEL_CONTENT_ID = "com.google.android.youtube:id/engagement_panel_content"

    /**
     * Public entrypoint taking Android framework [AccessibilityNodeInfo].
     */
    fun detect(rootNode: AccessibilityNodeInfo?): DetectionResult {
        if (rootNode == null) return DetectionResult(isReel = false, evidence = "NULL_ROOT")
        return detect(AndroidNodeInfo(rootNode))
    }

    /**
     * Internal entrypoint taking [NodeInfo] abstraction for clean JVM unit testability.
     */
    fun detect(root: NodeInfo?): DetectionResult {
        if (root == null) {
            return DetectionResult(isReel = false, evidence = "NULL_ROOT")
        }

        val packageName = root.packageName
        if (packageName != null && packageName != YOUTUBE_PACKAGE && packageName != "android") {
            return DetectionResult(isReel = false, evidence = "NON_YOUTUBE_PACKAGE ($packageName)")
        }

        // 1. Primary check: Search for reel_watch_fragment_root
        val shortsRoots = root.findByViewId(SHORTS_FRAGMENT_ROOT_ID)
        for (shortsRoot in shortsRoots) {
            val bounds = shortsRoot.boundsInScreen
            if (!rectIsEmpty(bounds) && shortsRoot.isVisibleToUser) {
                // Confirm presence of reel_recycler or page container
                val recyclers = root.findByViewId(SHORTS_RECYCLER_ID)
                val pages = root.findByViewId(SHORTS_PAGE_CONTAINER_ID)
                if (recyclers.isNotEmpty() || pages.isNotEmpty()) {
                    return DetectionResult(
                        isReel = true,
                        confidence = 1.0f,
                        activeReelBounds = bounds,
                        pagerBounds = bounds,
                        evidence = "SHORTS_ROOT_AND_RECYCLER_MATCH"
                    )
                }
            }
        }

        // 2. Direct feed container check (e.g. if root is detached/transient)
        val recyclers = root.findByViewId(SHORTS_RECYCLER_ID)
        for (recycler in recyclers) {
            val bounds = recycler.boundsInScreen
            if (!rectIsEmpty(bounds) && recycler.isVisibleToUser) {
                return DetectionResult(
                    isReel = true,
                    confidence = 0.95f,
                    activeReelBounds = bounds,
                    pagerBounds = bounds,
                    evidence = "SHORTS_RECYCLER_DIRECT_MATCH"
                )
            }
        }

        // 3. Structural fallback for release builds with obfuscated resource IDs
        if (hasStructuralShortsHierarchy(root)) {
            return DetectionResult(
                isReel = true,
                confidence = 0.85f,
                activeReelBounds = root.boundsInScreen,
                pagerBounds = root.boundsInScreen,
                evidence = "SHORTS_STRUCTURAL_FALLBACK_MATCH"
            )
        }

        return DetectionResult(isReel = false, evidence = "NO_SHORTS_SURFACE")
    }

    private fun hasStructuralShortsHierarchy(node: NodeInfo): Boolean {
        if (node.packageName != null && node.packageName != YOUTUBE_PACKAGE && node.packageName != "android") return false
        return findShortsStructuralMarker(node, 0)
    }

    private fun findShortsStructuralMarker(node: NodeInfo, depth: Int): Boolean {
        if (depth > 12) return false
        val bounds = node.boundsInScreen
        // Look for full-screen right-aligned vertical action column
        val isRightActionColumn = bounds.left >= 850 && bounds.right <= 1080 && (bounds.bottom - bounds.top) > 400
        if (isRightActionColumn && node.childCount >= 3) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findShortsStructuralMarker(child, depth + 1)
                child.recycle()
                if (found) return true
            }
        }
        return false
    }

    private fun rectIsEmpty(rect: Rect): Boolean {
        return rect.left >= rect.right || rect.top >= rect.bottom
    }

    /**
     * Checks whether a comments engagement panel is currently visible over Shorts.
     */
    fun isCommentsOpen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        return isCommentsOpen(AndroidNodeInfo(rootNode))
    }

    /**
     * Internal check for comments panel presence on testable [NodeInfo].
     */
    fun isCommentsOpen(root: NodeInfo?): Boolean {
        if (root == null) return false
        val panelList = root.findByViewId(ENGAGEMENT_PANEL_ID) +
                root.findByViewId(ENGAGEMENT_PANEL_WRAPPER_ID) +
                root.findByViewId(ENGAGEMENT_PANEL_CONTENT_ID)
        for (panel in panelList) {
            val bounds = panel.boundsInScreen
            val height = bounds.bottom - bounds.top
            if (panel.isVisibleToUser && height > 200) {
                return true
            }
        }
        return false
    }
}
