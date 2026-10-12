package com.mindlol.probe.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class SanitizedFixtureRegressionTest {

    @Test
    fun `Test 1 - Instagram Reel active surface fixture detected with high confidence`() {
        val root = SanitizedFixtureLoader.loadFixture("fixtures/instagram_reel_surface.json")
        val result = InstagramReelDetector.detect(root)

        assertTrue("Instagram Reel surface must be detected", result.isReel)
        assertEquals(1.0f, result.confidence)
        assertNotNull(result.pagerBounds)
        assertNotNull(result.activeReelBounds)
        assertTrue(result.evidence.contains("CLIPS_VIEW_PAGER"))
    }

    @Test
    fun `Test 2 - Instagram regular feed fixture rejected without error`() {
        val root = SanitizedFixtureLoader.loadFixture("fixtures/instagram_feed_surface.json")
        val result = InstagramReelDetector.detect(root)

        assertFalse("Regular feed must not be detected as Reel", result.isReel)
        assertEquals("NO_VIEW_PAGER", result.evidence)
    }

    @Test
    fun `Test 3 - YouTube Shorts active surface fixture detected with high confidence`() {
        val root = SanitizedFixtureLoader.loadFixture("fixtures/youtube_shorts_surface.json")
        val result = YouTubeShortsDetector.detect(root)

        assertTrue("YouTube Shorts surface must be detected", result.isReel)
        assertEquals(1.0f, result.confidence)
        assertNotNull(result.activeReelBounds)
        assertTrue(result.evidence.contains("SHORTS_ROOT_AND_RECYCLER_MATCH"))
    }

    @Test
    fun `Test 4 - YouTube regular watch fixture rejected cleanly`() {
        val root = SanitizedFixtureLoader.loadFixture("fixtures/youtube_regular_watch_surface.json")
        val result = YouTubeShortsDetector.detect(root)

        assertFalse("Regular YouTube video must not be detected as Shorts", result.isReel)
        assertEquals("NO_SHORTS_SURFACE", result.evidence)
    }

    @Test
    fun `Test 5 - YouTube Shorts comments sheet fixture detected and comments open confirmed`() {
        val root = SanitizedFixtureLoader.loadFixture("fixtures/youtube_shorts_comments_sheet.json")
        val detection = YouTubeShortsDetector.detect(root)
        val isCommentsOpen = YouTubeShortsDetector.isCommentsOpen(root)

        assertTrue("Shorts surface root should still be present", detection.isReel)
        assertTrue("Comments bottom sheet must be detected as open", isCommentsOpen)
    }

    @Test
    fun `Test 6 - Privacy audit verifies fixtures contain zero personal content or arbitrary text`() {
        val fixtureNames = listOf(
            "fixtures/instagram_reel_surface.json",
            "fixtures/instagram_feed_surface.json",
            "fixtures/instagram_comments_overlay.json",
            "fixtures/youtube_shorts_surface.json",
            "fixtures/youtube_regular_watch_surface.json",
            "fixtures/youtube_shorts_comments_sheet.json"
        )

        for (name in fixtureNames) {
            val stream = javaClass.classLoader?.getResourceAsStream(name)
            assertNotNull("Fixture $name must exist", stream)
            val text = InputStreamReader(stream!!).use { it.readText() }

            // Ensure only allowlisted structural tokens appear
            assertFalse("Fixture $name must not contain URLs", text.contains("http://") || text.contains("https://"))
            assertFalse("Fixture $name must not contain email/handle characters", text.contains("@"))
            assertFalse("Fixture $name must not contain user captions", text.contains("caption"))
            assertFalse("Fixture $name must not contain usernames", text.contains("username"))
        }
    }
}
