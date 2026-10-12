package com.mindlol.probe.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstagramReelCounterTest {

    private lateinit var counter: InstagramReelCounter

    @Before
    fun setUp() {
        counter = InstagramReelCounter()
    }

    private fun viewPagerEvent(
        index: Int,
        isReel: Boolean = true,
        eventType: String = "TYPE_VIEW_SCROLLED",
        className: String = "androidx.viewpager.widget.ViewPager"
    ): ReelObservation {
        return ReelObservation(
            isReel = isReel,
            eventType = eventType,
            eventClass = className,
            fromIndex = index,
            toIndex = index
        )
    }

    // =========================================================================
    // Explicit Phase 3 Required Sequence & Boundary Tests 1 - 10
    // =========================================================================

    @Test
    fun `Test 1 - Single Reel A baseline initialization produces count 0`() {
        val res = counter.processObservation(viewPagerEvent(0))
        assertFalse(res.isCounted)
        assertEquals(0, res.sessionCount)
        assertEquals(ScrollDirection.INITIAL, res.direction)
    }

    @Test
    fun `Test 2 - A to B forward transition produces count 1`() {
        counter.processObservation(viewPagerEvent(0)) // A (baseline)
        val resB = counter.processObservation(viewPagerEvent(1)) // B

        assertTrue(resB.isCounted)
        assertEquals(1, resB.sessionCount)
        assertEquals(ScrollDirection.FORWARD, resB.direction)
    }

    @Test
    fun `Test 3 - A to B to C forward sequence produces count 2`() {
        counter.processObservation(viewPagerEvent(0)) // A
        counter.processObservation(viewPagerEvent(1)) // B (+1)
        val resC = counter.processObservation(viewPagerEvent(2)) // C (+1)

        assertTrue(resC.isCounted)
        assertEquals(2, resC.sessionCount)
        assertEquals(ScrollDirection.FORWARD, resC.direction)
    }

    @Test
    fun `Test 4 - A to B to C to B sequence produces count 2`() {
        counter.processObservation(viewPagerEvent(0)) // A
        counter.processObservation(viewPagerEvent(1)) // B (+1)
        counter.processObservation(viewPagerEvent(2)) // C (+1)
        val resB = counter.processObservation(viewPagerEvent(1)) // B (back)

        assertFalse(resB.isCounted)
        assertEquals(2, resB.sessionCount)
        assertEquals(ScrollDirection.BACKWARD, resB.direction)
    }

    @Test
    fun `Test 5 - A to B to C to B to A sequence produces count 2`() {
        counter.processObservation(viewPagerEvent(0)) // A
        counter.processObservation(viewPagerEvent(1)) // B (+1)
        counter.processObservation(viewPagerEvent(2)) // C (+1)
        counter.processObservation(viewPagerEvent(1)) // B (back)
        val resA = counter.processObservation(viewPagerEvent(0)) // A (back)

        assertFalse(resA.isCounted)
        assertEquals(2, resA.sessionCount)
        assertEquals(ScrollDirection.BACKWARD, resA.direction)
    }

    @Test
    fun `Test 6 - Full validation sequence A to B to C to B to A to B produces count 3`() {
        counter.processObservation(viewPagerEvent(0)) // A (baseline = 0)
        counter.processObservation(viewPagerEvent(1)) // B (+1 -> 1)
        counter.processObservation(viewPagerEvent(2)) // C (+1 -> 2)
        counter.processObservation(viewPagerEvent(1)) // B (back = 2)
        counter.processObservation(viewPagerEvent(0)) // A (back = 2)
        val resB = counter.processObservation(viewPagerEvent(1)) // B (forward again! +1 -> 3)

        assertTrue(resB.isCounted)
        assertEquals(3, resB.sessionCount)
        assertEquals(ScrollDirection.FORWARD, resB.direction)
    }

    @Test
    fun `Test 7 - A to A to A to A repeated baseline events produce count 0`() {
        repeat(4) {
            val res = counter.processObservation(viewPagerEvent(0))
            assertFalse(res.isCounted)
            assertEquals(0, res.sessionCount)
        }
    }

    @Test
    fun `Test 8 - A to B to B to B repeated target events produce count 1`() {
        counter.processObservation(viewPagerEvent(0)) // A baseline
        val r1 = counter.processObservation(viewPagerEvent(1)) // B (+1)
        val r2 = counter.processObservation(viewPagerEvent(1)) // B repeat
        val r3 = counter.processObservation(viewPagerEvent(1)) // B repeat

        assertTrue(r1.isCounted)
        assertFalse(r2.isCounted)
        assertFalse(r3.isCounted)
        assertEquals(1, r3.sessionCount)
    }

    @Test
    fun `Test 9 - Invalid or non-Reel observations must not change count`() {
        counter.processObservation(viewPagerEvent(0)) // A (baseline = 0)
        counter.processObservation(viewPagerEvent(1)) // B (+1 = 1)

        // Non-Reel observation
        val nonReelObs = ReelObservation(
            isReel = false,
            eventType = "TYPE_WINDOW_CONTENT_CHANGED",
            eventClass = "android.widget.FrameLayout",
            fromIndex = -1,
            toIndex = -1
        )
        val resNonReel = counter.processObservation(nonReelObs)
        assertFalse(resNonReel.isCounted)
        assertEquals(1, resNonReel.sessionCount)
        assertEquals("DETECTOR_NOT_REEL_PRESERVED_STATE", resNonReel.evidence)

        // Invalid scroll event class
        val invalidClassObs = ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.recyclerview.widget.RecyclerView",
            fromIndex = 1,
            toIndex = 3
        )
        val resInvalidClass = counter.processObservation(invalidClassObs)
        assertFalse(resInvalidClass.isCounted)
        assertEquals(1, resInvalidClass.sessionCount)
        assertEquals("NON_VIEW_PAGER_OR_INVALID_EVENT", resInvalidClass.evidence)
    }

    @Test
    fun `Test 10 - Repeated AccessibilityEvents for same index must not increase count`() {
        counter.processObservation(viewPagerEvent(0)) // baseline
        counter.processObservation(viewPagerEvent(1)) // count = 1

        repeat(15) {
            val res = counter.processObservation(viewPagerEvent(1))
            assertFalse(res.isCounted)
            assertEquals(1, res.sessionCount)
            assertEquals(ScrollDirection.NONE, res.direction)
        }
    }

    // =========================================================================
    // Additional Edge Case Tests (Gaps, Reset, Large Jump)
    // =========================================================================

    @Test
    fun `Test 11 - Temporary detector gap preserves navigation state and resumes correctly`() {
        counter.processObservation(viewPagerEvent(0)) // baseline index 0

        // Detector gap during swipe animation
        val gapObs = viewPagerEvent(index = -1, isReel = false)
        val gapRes = counter.processObservation(gapObs)

        assertFalse(gapRes.isCounted)
        assertEquals("DETECTOR_NOT_REEL_PRESERVED_STATE", gapRes.evidence)

        // Resumes on next Reel index 1
        val resAfterGap = counter.processObservation(viewPagerEvent(1))

        assertTrue(resAfterGap.isCounted)
        assertEquals(1, resAfterGap.sessionCount)
        assertEquals(ScrollDirection.FORWARD, resAfterGap.direction)
    }

    @Test
    fun `Test 12 - Large index jump counts as one forward transition step`() {
        counter.processObservation(viewPagerEvent(1)) // baseline index 1

        // Fast fling jump from 1 -> 4
        val jumpRes = counter.processObservation(viewPagerEvent(4))

        assertTrue(jumpRes.isCounted)
        assertEquals(1, jumpRes.sessionCount)
        assertEquals(ScrollDirection.FORWARD, jumpRes.direction)
    }

    @Test
    fun `Test 13 - Session reset clears baseline and counter state`() {
        counter.processObservation(viewPagerEvent(0))
        counter.processObservation(viewPagerEvent(1)) // sessionCount = 1

        counter.resetSession()

        val newBase = counter.processObservation(viewPagerEvent(5))
        assertFalse(newBase.isCounted)
        assertEquals(0, newBase.sessionCount)
        assertEquals(ScrollDirection.INITIAL, newBase.direction)
    }

    @Test
    fun `Test 14 - Scrolling while comments overlay is open is suppressed without incrementing`() {
        counter.processObservation(viewPagerEvent(0)) // Baseline
        val res1 = counter.processObservation(viewPagerEvent(1))
        assertTrue(res1.isCounted)
        assertEquals(1, res1.sessionCount)

        // User opens comments sheet and scrolls
        val commentsObs = ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = 1,
            toIndex = 2,
            isCommentsOpen = true
        )
        val commentsRes = counter.processObservation(commentsObs)
        assertFalse(commentsRes.isCounted)
        assertEquals(1, commentsRes.sessionCount)
        assertEquals("COMMENTS_OVERLAY_ACTIVE_IGNORED", commentsRes.evidence)
    }

    @Test
    fun `Test 15 - Resuming forward swipe after closing comments increments count normally`() {
        counter.processObservation(viewPagerEvent(0)) // Baseline
        counter.processObservation(viewPagerEvent(1)) // count = 1

        // Comments opened
        val commentsObs = ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = 1,
            toIndex = 1,
            isCommentsOpen = true
        )
        counter.processObservation(commentsObs)

        // Comments closed and swipe forward to index 2
        val nextReel = counter.processObservation(viewPagerEvent(2))
        assertTrue(nextReel.isCounted)
        assertEquals(2, nextReel.sessionCount)
        assertEquals(ScrollDirection.FORWARD, nextReel.direction)
    }
}
