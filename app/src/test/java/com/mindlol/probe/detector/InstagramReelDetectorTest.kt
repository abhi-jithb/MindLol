package com.mindlol.probe.detector

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramReelDetectorTest {

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
    fun `test 1 - Normal Instagram Reel tree is detected`() {
        val mediaComponent = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            contentDescription = "Reel by creator_john. Double tap to play or pause.",
            boundsInScreen = createRect(0, 110, 1080, 2211)
        )
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            className = "androidx.viewpager.widget.ViewPager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = listOf(mediaComponent)
        )
        val root = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_container",
            childrenList = listOf(viewPager)
        )

        val result = InstagramReelDetector.detect(root)

        assertTrue("Expected isReel=true but got: $result", result.isReel)
        assertEquals(1.0f, result.confidence, 0.01f)
        assertEquals("creator_john", result.authorName)
        assertRectEquals(createRect(0, 110, 1080, 2211), result.activeReelBounds)
        assertRectEquals(createRect(0, 110, 1080, 2211), result.pagerBounds)
        assertEquals("CLIPS_VIEW_PAGER_AND_ACTIVE_MEDIA_COMPONENT", result.evidence)
    }

    @Test
    fun `test 2 - Instagram non-Reel home feed screen is not detected`() {
        val reelsTray = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/reels_tray_container",
            contentDescription = "reels tray container",
            boundsInScreen = createRect(0, 265, 1080, 611)
        )
        val homePager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/swipeable_tab_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 112, 1080, 2206),
            childrenList = listOf(reelsTray)
        )
        val root = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/action_bar_root",
            childrenList = listOf(homePager)
        )

        val result = InstagramReelDetector.detect(root)

        assertFalse(result.isReel)
        assertNull(result.activeReelBounds)
        assertEquals("NO_VIEW_PAGER", result.evidence)
    }

    @Test
    fun `test 3 - Reel tree with invisible offscreen media component does not select it`() {
        val invisibleMedia = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            isVisibleToUser = false,
            contentDescription = "Reel by offscreen_user. Double tap.",
            boundsInScreen = createRect(0, 2211, 1080, 4312)
        )
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = listOf(invisibleMedia)
        )
        val root = TestNodeInfo(childrenList = listOf(viewPager))

        val result = InstagramReelDetector.detect(root)

        assertFalse("Expected isReel=false but got: $result", result.isReel)
        assertNull(result.activeReelBounds)
        assertEquals("PAGER_FOUND_NO_ACTIVE_MEDIA_CANDIDATE", result.evidence)
    }

    @Test
    fun `test 4 - Reel tree with multiple pager children selects active visible candidate only`() {
        val previousMedia = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            isVisibleToUser = true,
            contentDescription = "Reel by prev_user.",
            boundsInScreen = createRect(0, -2000, 1080, -10)
        )
        val activeMedia = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            isVisibleToUser = true,
            contentDescription = "Reel by active_user.",
            boundsInScreen = createRect(0, 110, 1080, 2211)
        )
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = listOf(previousMedia, activeMedia)
        )
        val root = TestNodeInfo(childrenList = listOf(viewPager))

        val result = InstagramReelDetector.detect(root)

        assertTrue("Expected isReel=true but got: $result", result.isReel)
        assertEquals("active_user", result.authorName)
        assertRectEquals(createRect(0, 110, 1080, 2211), result.activeReelBounds)
    }

    @Test
    fun `test 5 - Missing clips_viewer_view_pager returns isReel false`() {
        val root = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/random_layout",
            childrenList = emptyList()
        )

        val result = InstagramReelDetector.detect(root)

        assertFalse(result.isReel)
        assertEquals("NO_VIEW_PAGER", result.evidence)
    }

    @Test
    fun `test 6 - Missing clips_media_component returns isReel false`() {
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = emptyList()
        )
        val root = TestNodeInfo(childrenList = listOf(viewPager))

        val result = InstagramReelDetector.detect(root)

        assertFalse(result.isReel)
        assertEquals("PAGER_FOUND_NO_ACTIVE_MEDIA_CANDIDATE", result.evidence)
    }

    @Test
    fun `test 7 - Zero-width candidate is rejected`() {
        val zeroWidthMedia = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            isVisibleToUser = true,
            boundsInScreen = createRect(0, 110, 0, 2211)
        )
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = listOf(zeroWidthMedia)
        )
        val root = TestNodeInfo(childrenList = listOf(viewPager))

        val result = InstagramReelDetector.detect(root)

        assertFalse(result.isReel)
        assertEquals("PAGER_FOUND_NO_ACTIVE_MEDIA_CANDIDATE", result.evidence)
    }

    @Test
    fun `test 8 - Partial candidate with insignificant overlap is rejected`() {
        val partialMedia = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_media_component",
            isVisibleToUser = true,
            boundsInScreen = createRect(0, 2200, 1080, 4300)
        )
        val viewPager = TestNodeInfo(
            viewIdResourceName = "com.instagram.android:id/clips_viewer_view_pager",
            isScrollable = true,
            boundsInScreen = createRect(0, 110, 1080, 2211),
            childrenList = listOf(partialMedia)
        )
        val root = TestNodeInfo(childrenList = listOf(viewPager))

        val result = InstagramReelDetector.detect(root)

        assertFalse(result.isReel)
        assertEquals("PAGER_FOUND_NO_ACTIVE_MEDIA_CANDIDATE", result.evidence)
    }

    @Test
    fun `test author extraction logic`() {
        val desc1 = "Reel by rdriyaaa. Double tap to play or pause."
        val desc2 = "Reel by john_doe"
        val desc3 = "Random text description"

        assertEquals("rdriyaaa", InstagramReelDetector.extractAuthorFromDesc(desc1))
        assertEquals("john_doe", InstagramReelDetector.extractAuthorFromDesc(desc2))
        assertNull(InstagramReelDetector.extractAuthorFromDesc(desc3))
    }
}
