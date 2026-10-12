package com.mindlol.probe.detector

import org.json.JSONObject
import java.io.InputStreamReader

/**
 * Loads sanitized, privacy-safe accessibility tree fixtures from test resources
 * and transforms them into [TestNodeInfo] hierarchies for deterministic regression testing.
 */
object SanitizedFixtureLoader {

    fun loadFixture(resourcePath: String): NodeInfo {
        val stream = javaClass.classLoader?.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Fixture resource not found: $resourcePath")

        val jsonText = InputStreamReader(stream).use { it.readText() }
        val json = JSONObject(jsonText)
        return parseNode(json)
    }

    private fun parseNode(json: JSONObject): NodeInfo {
        val pkg = if (json.has("packageName") && !json.isNull("packageName")) json.getString("packageName") else null
        val viewId = if (json.has("viewId") && !json.isNull("viewId")) json.getString("viewId") else null
        val className = json.optString("className", "android.view.ViewGroup")
        val visible = json.optBoolean("visible", true)
        val scrollable = json.optBoolean("scrollable", false)

        val boundsArray = json.optJSONArray("bounds")
        val bounds = if (boundsArray != null && boundsArray.length() == 4) {
            createRect(
                boundsArray.getInt(0),
                boundsArray.getInt(1),
                boundsArray.getInt(2),
                boundsArray.getInt(3)
            )
        } else {
            createRect(0, 0, 1080, 2400)
        }

        val childrenList = ArrayList<NodeInfo>()
        val childrenArray = json.optJSONArray("children")
        if (childrenArray != null) {
            for (i in 0 until childrenArray.length()) {
                val childObj = childrenArray.getJSONObject(i)
                childrenList.add(parseNode(childObj))
            }
        }

        return TestNodeInfo(
            packageName = pkg,
            viewIdResourceName = viewId,
            className = className,
            isVisibleToUser = visible,
            isScrollable = scrollable,
            boundsInScreen = bounds,
            childrenList = childrenList
        )
    }
}
