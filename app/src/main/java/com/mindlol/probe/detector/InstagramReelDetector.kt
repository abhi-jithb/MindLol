package com.mindlol.probe.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Production-oriented detector for Instagram Reel short-video surfaces.
 *
 * Evaluates accessibility tree structures exposed by `com.instagram.android`
 * to determine whether an active, full-screen Instagram Reel viewer is visible.
 *
 * This component is strictly stateless and read-only: it does NOT maintain counters,
 * calculate swipe directions, trigger overlays, or alter accessibility nodes.
 */
object InstagramReelDetector {

    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val VIEW_PAGER_ID = "com.instagram.android:id/clips_viewer_view_pager"
    const val MEDIA_COMPONENT_ID = "com.instagram.android:id/clips_media_component"
    const val SINGLE_MEDIA_COMPONENT_ID = "com.instagram.android:id/clips_single_media_component"
    const val CONTAINER_ID = "com.instagram.android:id/clips_viewer_container"

    private val AUTHOR_REGEX = Regex("""Reel by\s+([^.]+)(?:\.|\b|$)""", RegexOption.IGNORE_CASE)

    /**
     * Public entrypoint for Android framework [AccessibilityNodeInfo] calls.
     */
    fun detect(rootNode: AccessibilityNodeInfo?): DetectionResult {
        if (rootNode == null) return DetectionResult(isReel = false, evidence = "NULL_ROOT")
        return detect(AndroidNodeInfo(rootNode))
    }

    /**
     * Internal entrypoint taking [NodeInfo] abstraction for testability.
     */
    fun detect(root: NodeInfo?): DetectionResult {
        if (root == null) {
            return DetectionResult(isReel = false, evidence = "NULL_ROOT")
        }

        val packageName = root.packageName
        if (packageName != null && packageName != INSTAGRAM_PACKAGE) {
            return DetectionResult(isReel = false, evidence = "NON_INSTAGRAM_PACKAGE")
        }

        // 1. Locate Instagram Reel ViewPager container node
        val pagerNode = findViewPagerNode(root)
            ?: return DetectionResult(isReel = false, evidence = "NO_VIEW_PAGER")

        val pagerBounds = pagerNode.boundsInScreen

        if (rectIsEmpty(pagerBounds)) {
            recycleNode(pagerNode, root)
            return DetectionResult(isReel = false, evidence = "INVALID_PAGER_BOUNDS")
        }

        // 2. Collect candidate media component nodes within the ViewPager
        val mediaCandidates = ArrayList<NodeInfo>()
        collectMediaCandidates(pagerNode, mediaCandidates)

        // 3. Select the single candidate representing the currently active, visible Reel
        val activeCandidate = selectActiveCandidate(mediaCandidates, pagerBounds)

        recycleNode(pagerNode, root)

        if (activeCandidate == null) {
            recycleNodes(mediaCandidates, root)
            return DetectionResult(
                isReel = false,
                pagerBounds = pagerBounds,
                evidence = "PAGER_FOUND_NO_ACTIVE_MEDIA_CANDIDATE"
            )
        }

        val activeBounds = activeCandidate.boundsInScreen
        val authorName = extractAuthorFromDesc(activeCandidate.contentDescription?.toString())

        recycleNodes(mediaCandidates, root)

        return DetectionResult(
            isReel = true,
            confidence = 1.0f,
            activeReelBounds = activeBounds,
            pagerBounds = pagerBounds,
            authorName = authorName,
            evidence = "CLIPS_VIEW_PAGER_AND_ACTIVE_MEDIA_COMPONENT"
        )
    }

    private fun findViewPagerNode(root: NodeInfo): NodeInfo? {
        val nodesById = root.findByViewId(VIEW_PAGER_ID)
        if (nodesById.isNotEmpty()) {
            val primary = nodesById.first()
            for (i in 1 until nodesById.size) {
                recycleNode(nodesById[i], root)
            }
            return primary
        }

        return searchViewPagerFallback(root)
    }

    private fun searchViewPagerFallback(node: NodeInfo): NodeInfo? {
        val resId = node.viewIdResourceName ?: ""
        val className = node.className ?: ""

        if (resId.contains("clips_viewer_view_pager") ||
            (node.isScrollable && (className.endsWith("ViewPager") || className.contains("RecyclerView")) && resId.contains("clips"))
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchViewPagerFallback(child)
            if (found != null) {
                if (found != child) {
                    recycleNode(child, node)
                }
                return found
            }
            recycleNode(child, node)
        }
        return null
    }

    private fun collectMediaCandidates(node: NodeInfo, results: MutableList<NodeInfo>) {
        val resId = node.viewIdResourceName ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        val isCandidate = resId.contains("clips_media_component") ||
                resId.contains("clips_single_media_component") ||
                desc.startsWith("Reel by", ignoreCase = true)

        if (isCandidate) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectMediaCandidates(child, results)
            if (!results.contains(child)) {
                recycleNode(child, node)
            }
        }
    }

    private fun selectActiveCandidate(
        candidates: List<NodeInfo>,
        pagerBounds: Rect
    ): NodeInfo? {
        var maxOverlapArea = 0
        var bestCandidate: NodeInfo? = null

        for (candidate in candidates) {
            if (!candidate.isVisibleToUser) continue

            val bounds = candidate.boundsInScreen
            if (rectIsEmpty(bounds)) continue

            val overlapArea = getIntersectionArea(bounds, pagerBounds)
            val candidateArea = rectWidth(bounds) * rectHeight(bounds)

            // Require candidate to be substantially visible within pager bounds (> 30% area)
            if (overlapArea > maxOverlapArea && overlapArea >= (candidateArea * 0.3)) {
                maxOverlapArea = overlapArea
                bestCandidate = candidate
            }
        }

        return bestCandidate
    }

    fun extractAuthorFromDesc(desc: String?): String? {
        if (desc.isNullOrBlank()) return null
        val match = AUTHOR_REGEX.find(desc) ?: return null
        return match.groupValues.getOrNull(1)?.trim()
    }

    private fun rectWidth(r: Rect): Int = r.right - r.left
    private fun rectHeight(r: Rect): Int = r.bottom - r.top
    private fun rectIsEmpty(r: Rect): Boolean = rectWidth(r) <= 0 || rectHeight(r) <= 0

    private fun getIntersectionArea(r1: Rect, r2: Rect): Int {
        val left = maxOf(r1.left, r2.left)
        val top = maxOf(r1.top, r2.top)
        val right = minOf(r1.right, r2.right)
        val bottom = minOf(r1.bottom, r2.bottom)

        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return 0
        return w * h
    }

    private fun recycleNodes(nodes: List<NodeInfo>, root: NodeInfo) {
        for (node in nodes) {
            recycleNode(node, root)
        }
    }

    private fun recycleNode(node: NodeInfo, root: NodeInfo) {
        if (node === root) return
        try {
            node.recycle()
        } catch (_: Throwable) {
            // Ignore safe recycling exceptions
        }
    }
}
