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

    @Test
    fun `test 1 - Initial Reel establishes baseline without incrementing`() {
        val res = counter.processObservation(viewPagerEvent(0))
        assertFalse(res.isCounted)
        assertEquals(0, res.sessionCount)
        assertEquals(ScrollDirection.INITIAL, res.direction)
        assertEquals(InstagramReelCounter.INDEX_UNSET, res.previousIndex)
        assertEquals(0, res.currentIndex)
    }

    @Test
    fun `test 2 - Repeated initial observations do not count`() {
        counter.processObservation(viewPagerEvent(0))
        val res2 = counter.processObservation(viewPagerEvent(0))
        val res3 = counter.processObservation(viewPagerEvent(0))

        assertFalse(res2.isCounted)
        assertFalse(res3.isCounted)
        assertEquals(0, res3.sessionCount)
        assertEquals(ScrollDirection.NONE, res3.direction)
    }

    @Test
    fun `test 3 - Single forward navigation counts once`() {
        counter.processObservation(viewPagerEvent(0))
        val res = counter.processObservation(viewPagerEvent(1))

        assertTrue(res.isCounted)
        assertEquals(1, res.sessionCount)
        assertEquals(ScrollDirection.FORWARD, res.direction)
        assertEquals(0, res.previousIndex)
        assertEquals(1, res.currentIndex)
    }

    @Test
    fun `test 4 - Repeated B events do not produce additional counts`() {
        counter.processObservation(viewPagerEvent(0))
        counter.processObservation(viewPagerEvent(1)) // count 1

        val res2 = counter.processObservation(viewPagerEvent(1))
        val res3 = counter.processObservation(viewPagerEvent(1))

        assertFalse(res2.isCounted)
        assertFalse(res3.isCounted)
        assertEquals(1, res3.sessionCount)
    }

    @Test
    fun `test 5 - Multiple forward navigations increment session count for each new step`() {
        counter.processObservation(viewPagerEvent(0)) // baseline
        val r1 = counter.processObservation(viewPagerEvent(1)) // count 1
        val r2 = counter.processObservation(viewPagerEvent(2)) // count 2
        val r3 = counter.processObservation(viewPagerEvent(3)) // count 3

        assertTrue(r1.isCounted)
        assertTrue(r2.isCounted)
        assertTrue(r3.isCounted)
        assertEquals(3, r3.sessionCount)
    }

    @Test
    fun `test 6 - Backward navigation does not count`() {
        counter.processObservation(viewPagerEvent(0))
        counter.processObservation(viewPagerEvent(1))
        counter.processObservation(viewPagerEvent(2)) // count 2

        val backRes = counter.processObservation(viewPagerEvent(1)) // 2 -> 1

        assertFalse(backRes.isCounted)
        assertEquals(2, backRes.sessionCount)
        assertEquals(ScrollDirection.BACKWARD, backRes.direction)
        assertEquals(2, backRes.previousIndex)
        assertEquals(1, backRes.currentIndex)
    }

    @Test
    fun `test 7 - Multiple backward navigations produce no counts`() {
        counter.processObservation(viewPagerEvent(0))
        counter.processObservation(viewPagerEvent(1))
        counter.processObservation(viewPagerEvent(2)) // count 2

        val back1 = counter.processObservation(viewPagerEvent(1))
        val back2 = counter.processObservation(viewPagerEvent(0))

        assertFalse(back1.isCounted)
        assertFalse(back2.isCounted)
        assertEquals(2, back2.sessionCount)
    }

    @Test
    fun `test 8 - Forward navigation after backward navigation counts as new transition`() {
        counter.processObservation(viewPagerEvent(0)) // baseline
        counter.processObservation(viewPagerEvent(1)) // count 1
        counter.processObservation(viewPagerEvent(2)) // count 2
        counter.processObservation(viewPagerEvent(1)) // back (count 2)
        counter.processObservation(viewPagerEvent(0)) // back (count 2)

        val forwardAgain = counter.processObservation(viewPagerEvent(1)) // 0 -> 1 forward again!

        assertTrue(forwardAgain.isCounted)
        assertEquals(3, forwardAgain.sessionCount)
        assertEquals(ScrollDirection.FORWARD, forwardAgain.direction)
        assertEquals(0, forwardAgain.previousIndex)
        assertEquals(1, forwardAgain.currentIndex)
    }

    @Test
    fun `test 9 - Same index repeated through many events produces no counts`() {
        counter.processObservation(viewPagerEvent(0))
        counter.processObservation(viewPagerEvent(1)) // count 1

        repeat(10) {
            val r = counter.processObservation(viewPagerEvent(1))
            assertFalse(r.isCounted)
        }
        assertEquals(1, counter.processObservation(viewPagerEvent(1)).sessionCount)
    }

    @Test
    fun `test 10 - Invalid index values are ignored`() {
        counter.processObservation(viewPagerEvent(0))
        val invalidObs = ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = -1,
            toIndex = -1
        )
        val res = counter.processObservation(invalidObs)

        assertFalse(res.isCounted)
        assertEquals(0, res.sessionCount)
        assertEquals("NON_VIEW_PAGER_OR_INVALID_EVENT", res.evidence)
    }

    @Test
    fun `test 11 - Temporary detector loss preserves navigation state`() {
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
        assertEquals(0, resAfterGap.previousIndex)
        assertEquals(1, resAfterGap.currentIndex)
    }

    @Test
    fun `test 12 - Detector unavailable repeatedly does not increment count`() {
        val nonReelObs = viewPagerEvent(index = 0, isReel = false)
        repeat(5) {
            val res = counter.processObservation(nonReelObs)
            assertFalse(res.isCounted)
            assertEquals(0, res.sessionCount)
        }
    }

    @Test
    fun `test 13 - Non-Reel Instagram screen produces no counting`() {
        val nonReelScreenObs = ReelObservation(
            isReel = false,
            eventType = "TYPE_WINDOW_CONTENT_CHANGED",
            eventClass = "android.widget.FrameLayout",
            fromIndex = -1,
            toIndex = -1
        )
        val res = counter.processObservation(nonReelScreenObs)

        assertFalse(res.isCounted)
        assertEquals(0, res.sessionCount)
    }

    @Test
    fun `test 14 - RecyclerView scroll event with changing indices is ignored`() {
        counter.processObservation(viewPagerEvent(0)) // baseline

        val recyclerViewObs = ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.recyclerview.widget.RecyclerView",
            fromIndex = 0,
            toIndex = 2
        )
        val res = counter.processObservation(recyclerViewObs)

        assertFalse(res.isCounted)
        assertEquals(0, res.sessionCount)
        assertEquals("NON_VIEW_PAGER_OR_INVALID_EVENT", res.evidence)
    }

    @Test
    fun `test 15 - Large index jump counts as one forward transition step`() {
        counter.processObservation(viewPagerEvent(1)) // baseline index 1

        // Fast fling jump from 1 -> 4
        val jumpRes = counter.processObservation(viewPagerEvent(4))

        assertTrue(jumpRes.isCounted)
        assertEquals(1, jumpRes.sessionCount)
        assertEquals(ScrollDirection.FORWARD, jumpRes.direction)
        assertEquals(1, jumpRes.previousIndex)
        assertEquals(4, jumpRes.currentIndex)
    }
}
