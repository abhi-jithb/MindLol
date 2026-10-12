package com.mindlol.probe.detector

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeShortsDetectorTest {

    private fun assertRectEquals(expected: Rect, actual: Rect?) {
        assertNotNull("Expected non-null Rect", actual)
        actual?.let {
            assertEquals("left bounds mismatch", expected.left, it.left)
            assertEquals("top bounds mismatch", expected.top, it.top)
            assertEquals("right bounds mismatch", expected.right, it.right)
            assertEquals("bottom bounds mismatch", expected.bottom, it.bottom)
        }
    }

    @Test
    fun `test 1 - Verified YouTube Shorts root and recycler are detected`() {
        val recycler = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            className = "androidx.recyclerview.widget.RecyclerView",
            isScrollable = true,
            boundsInScreen = createRect(0, 0, 1080, 2400)
        )
        val fragmentRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root",
            className = "android.widget.FrameLayout",
            boundsInScreen = createRect(0, 0, 1080, 2400),
            childrenList = listOf(recycler)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "android:id/content",
            childrenList = listOf(fragmentRoot)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertTrue("Expected isReel=true but got: $result", result.isReel)
        assertEquals(1.0f, result.confidence, 0.01f)
        assertRectEquals(createRect(0, 0, 1080, 2400), result.activeReelBounds)
        assertEquals("SHORTS_ROOT_AND_RECYCLER_MATCH", result.evidence)
    }

    @Test
    fun `test 2 - Verified YouTube Shorts root and page container are detected`() {
        val pageContainer = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_player_page_container",
            className = "android.widget.FrameLayout",
            boundsInScreen = createRect(0, 0, 1080, 2400)
        )
        val fragmentRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root",
            className = "android.widget.FrameLayout",
            boundsInScreen = createRect(0, 0, 1080, 2400),
            childrenList = listOf(pageContainer)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(fragmentRoot)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertTrue("Expected isReel=true", result.isReel)
        assertEquals(1.0f, result.confidence, 0.01f)
        assertEquals("SHORTS_ROOT_AND_RECYCLER_MATCH", result.evidence)
    }

    @Test
    fun `test 3 - YouTube Shorts direct feed container detected if root transient`() {
        val recycler = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            className = "androidx.recyclerview.widget.RecyclerView",
            isScrollable = true,
            boundsInScreen = createRect(0, 0, 1080, 2400)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(recycler)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertTrue(result.isReel)
        assertEquals(0.95f, result.confidence, 0.01f)
        assertEquals("SHORTS_RECYCLER_DIRECT_MATCH", result.evidence)
    }

    @Test
    fun `test 4 - YouTube Home feed is not detected as Shorts`() {
        val homeFeed = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/results",
            className = "androidx.recyclerview.widget.RecyclerView",
            isScrollable = true,
            boundsInScreen = createRect(0, 150, 1080, 2200)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/pane_fragment_container",
            childrenList = listOf(homeFeed)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertFalse("Expected isReel=false on Home feed", result.isReel)
        assertNull(result.activeReelBounds)
        assertEquals("NO_SHORTS_SURFACE", result.evidence)
    }

    @Test
    fun `test 5 - Regular YouTube video watch page is not detected as Shorts`() {
        val playerView = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/watch_while_layout",
            boundsInScreen = createRect(0, 0, 1080, 800)
        )
        val videoDescription = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/video_metadata_layout",
            boundsInScreen = createRect(0, 800, 1080, 2400)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(playerView, videoDescription)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertFalse("Expected isReel=false on standard video player", result.isReel)
        assertEquals("NO_SHORTS_SURFACE", result.evidence)
    }

    @Test
    fun `test 6 - YouTube search results screen is not detected as Shorts`() {
        val searchResults = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/search_results",
            boundsInScreen = createRect(0, 150, 1080, 2400)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(searchResults)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertFalse("Expected isReel=false on search results", result.isReel)
        assertEquals("NO_SHORTS_SURFACE", result.evidence)
    }

    @Test
    fun `test 7 - Non-YouTube package is rejected immediately`() {
        val instagramRoot = TestNodeInfo(
            packageName = "com.instagram.android",
            viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root"
        )

        val result = YouTubeShortsDetector.detect(instagramRoot)

        assertFalse("Expected isReel=false for non-YouTube package", result.isReel)
        assertTrue(result.evidence.contains("NON_YOUTUBE_PACKAGE"))
    }

    @Test
    fun `test 8 - Null root returns safe negative detection`() {
        val result = YouTubeShortsDetector.detect(null as NodeInfo?)

        assertFalse(result.isReel)
        assertEquals("NULL_ROOT", result.evidence)
    }

    @Test
    fun `test 9 - Invisible or zero-bounds Shorts root is not detected`() {
        val invisibleRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root",
            isVisibleToUser = false,
            boundsInScreen = createRect(0, 0, 0, 0)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(invisibleRoot)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertFalse(result.isReel)
    }

    @Test
    fun `test 10 - Comments overlay open over active Shorts retains Shorts detection`() {
        val commentsPanel = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/engagement_panel",
            boundsInScreen = createRect(0, 800, 1080, 2400)
        )
        val recycler = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_recycler",
            boundsInScreen = createRect(0, 0, 1080, 2400)
        )
        val fragmentRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/reel_watch_fragment_root",
            boundsInScreen = createRect(0, 0, 1080, 2400),
            childrenList = listOf(recycler)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(fragmentRoot, commentsPanel)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertTrue("Expected isReel=true while comments overlay is open over Shorts", result.isReel)
        assertEquals(1.0f, result.confidence, 0.01f)
    }

    @Test
    fun `test 11 - Structural fallback detects right-aligned action column if IDs obfuscated`() {
        val likeBtn = TestNodeInfo(
            packageName = "com.google.android.youtube",
            boundsInScreen = createRect(880, 1000, 1060, 1150)
        )
        val commentBtn = TestNodeInfo(
            packageName = "com.google.android.youtube",
            boundsInScreen = createRect(880, 1200, 1060, 1350)
        )
        val shareBtn = TestNodeInfo(
            packageName = "com.google.android.youtube",
            boundsInScreen = createRect(880, 1400, 1060, 1550)
        )
        val rightActionColumn = TestNodeInfo(
            packageName = "com.google.android.youtube",
            boundsInScreen = createRect(860, 950, 1070, 1800),
            childrenList = listOf(likeBtn, commentBtn, shareBtn)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            boundsInScreen = createRect(0, 0, 1080, 2400),
            childrenList = listOf(rightActionColumn)
        )

        val result = YouTubeShortsDetector.detect(root)

        assertTrue("Expected isReel=true via structural fallback", result.isReel)
        assertEquals(0.85f, result.confidence, 0.01f)
        assertEquals("SHORTS_STRUCTURAL_FALLBACK_MATCH", result.evidence)
    }

    @Test
    fun `test 12 - isCommentsOpen returns true when engagement panel is visible and expanded`() {
        val commentsPanel = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/app_engagement_panel",
            isVisibleToUser = true,
            boundsInScreen = createRect(0, 700, 1080, 2400)
        )
        val root = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(commentsPanel)
        )

        assertTrue(YouTubeShortsDetector.isCommentsOpen(root))
    }

    @Test
    fun `test 13 - isCommentsOpen returns false when engagement panel is absent or invisible`() {
        val normalRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = emptyList()
        )
        assertFalse(YouTubeShortsDetector.isCommentsOpen(normalRoot))

        val collapsedPanel = TestNodeInfo(
            packageName = "com.google.android.youtube",
            viewIdResourceName = "com.google.android.youtube:id/app_engagement_panel",
            isVisibleToUser = false,
            boundsInScreen = createRect(0, 0, 0, 0)
        )
        val collapsedRoot = TestNodeInfo(
            packageName = "com.google.android.youtube",
            childrenList = listOf(collapsedPanel)
        )
        assertFalse(YouTubeShortsDetector.isCommentsOpen(collapsedRoot))
    }
}
