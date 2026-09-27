package com.mindlol.probe.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Helper function to instantiate [Rect] with public field assignment,
 * ensuring compatibility with unit test JVM stub environments.
 */
fun createRect(left: Int, top: Int, right: Int, bottom: Int): Rect {
    val r = Rect()
    r.left = left
    r.top = top
    r.right = right
    r.bottom = bottom
    return r
}

/**
 * Abstraction over AccessibilityNodeInfo for clean testability and structural decoupling.
 */
interface NodeInfo {
    val packageName: String?
    val viewIdResourceName: String?
    val className: String?
    val isVisibleToUser: Boolean
    val isScrollable: Boolean
    val contentDescription: CharSequence?
    val boundsInScreen: Rect
    val childCount: Int

    fun getChild(index: Int): NodeInfo?
    fun findByViewId(viewId: String): List<NodeInfo>
    fun recycle()
}

/**
 * Production implementation wrapping real Android [AccessibilityNodeInfo].
 */
class AndroidNodeInfo(val node: AccessibilityNodeInfo) : NodeInfo {
    override val packageName: String?
        get() = node.packageName?.toString()

    override val viewIdResourceName: String?
        get() = node.viewIdResourceName

    override val className: String?
        get() = node.className?.toString()

    override val isVisibleToUser: Boolean
        get() = node.isVisibleToUser

    override val isScrollable: Boolean
        get() = node.isScrollable

    override val contentDescription: CharSequence?
        get() = node.contentDescription

    override val boundsInScreen: Rect
        get() {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            return rect
        }

    override val childCount: Int
        get() = node.childCount

    override fun getChild(index: Int): NodeInfo? {
        val child = node.getChild(index) ?: return null
        return AndroidNodeInfo(child)
    }

    override fun findByViewId(viewId: String): List<NodeInfo> {
        val list = node.findAccessibilityNodeInfosByViewId(viewId) ?: return emptyList()
        return list.map { AndroidNodeInfo(it) }
    }

    override fun recycle() {
        try {
            node.recycle()
        } catch (_: Throwable) {
            // Ignore safe recycling exceptions
        }
    }
}

/**
 * Synthetic node implementation for unit testing without Android framework dependency.
 */
data class TestNodeInfo(
    override val packageName: String? = "com.instagram.android",
    override val viewIdResourceName: String? = null,
    override val className: String? = "android.view.ViewGroup",
    override val isVisibleToUser: Boolean = true,
    override val isScrollable: Boolean = false,
    override val contentDescription: CharSequence? = null,
    override val boundsInScreen: Rect = createRect(0, 110, 1080, 2211),
    val childrenList: List<NodeInfo> = emptyList()
) : NodeInfo {

    override val childCount: Int
        get() = childrenList.size

    override fun getChild(index: Int): NodeInfo? {
        return childrenList.getOrNull(index)
    }

    override fun findByViewId(viewId: String): List<NodeInfo> {
        val results = ArrayList<NodeInfo>()
        searchByViewId(this, viewId, results)
        return results
    }

    private fun searchByViewId(node: NodeInfo, viewId: String, results: MutableList<NodeInfo>) {
        if (node.viewIdResourceName == viewId) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            searchByViewId(child, viewId, results)
        }
    }

    override fun recycle() {
        // No-op for synthetic test nodes
    }
}
