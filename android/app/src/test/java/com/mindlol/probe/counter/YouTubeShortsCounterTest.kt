package com.mindlol.probe.counter

import com.mindlol.probe.repository.KeyValueStore
import com.mindlol.probe.repository.ReelCounterRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeShortsCounterTest {

    private lateinit var counter: YouTubeShortsCounter

    @Before
    fun setUp() {
        counter = YouTubeShortsCounter(
            minForwardScrollPx = 400,
            debounceWindowMs = 650L,
            settlingTimeoutMs = 800L,
            surfaceExitTimeoutMs = 10000L
        )
    }

    private fun shortsScrollEvent(
        scrollY: Int,
        timestampMs: Long,
        isShorts: Boolean = true,
        isCommentsOpen: Boolean = false,
        eventType: String = "TYPE_VIEW_SCROLLED",
        className: String = "androidx.recyclerview.widget.RecyclerView"
    ): ShortsObservation {
        return ShortsObservation(
            isShorts = isShorts,
            eventType = eventType,
            eventClass = className,
            scrollX = 0,
            scrollY = scrollY,
            isCommentsOpen = isCommentsOpen,
            timestampMs = timestampMs
        )
    }

    @Test
    fun `Test 1 - Single initial Shorts landing establishes baseline with count 0`() {
        val res = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L))

        assertFalse("Baseline should not increment count", res.isCounted)
        assertEquals(0, res.sessionCount)
        assertEquals(ScrollDirection.INITIAL, res.direction)
        assertEquals("BASELINE_INITIALIZED", res.evidence)
    }

    @Test
    fun `Test 2 - Forward position samples from gesture start through settling counts exactly once`() {
        // Ground truth sequence from state2_forward_swipe_2.log:
        // Baseline -> +533 -> +1075 -> -143 (recoil) -> 0 (settled)
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        val res1 = counter.processObservation(shortsScrollEvent(scrollY = 533, timestampMs = 1500L))
        assertTrue("Displacement >= 400px increments count", res1.isCounted)
        assertEquals(1, res1.sessionCount)
        assertEquals(ScrollDirection.FORWARD, res1.direction)
        assertTrue(res1.evidence.contains("FORWARD_TRANSITION_DISPLACEMENT"))

        // Continuation sample in same gesture
        val res2 = counter.processObservation(shortsScrollEvent(scrollY = 1075, timestampMs = 1550L))
        assertFalse("Same gesture sample must be suppressed", res2.isCounted)
        assertEquals(1, res2.sessionCount)
        assertTrue(res2.evidence.contains("SAME_GESTURE_SAMPLE_SUPPRESSED"))

        // Negative settling recoil overshoot
        val res3 = counter.processObservation(shortsScrollEvent(scrollY = -143, timestampMs = 1650L))
        assertFalse("Negative recoil overshoot must be suppressed", res3.isCounted)
        assertEquals(1, res3.sessionCount)
        assertTrue(res3.evidence.contains("FORWARD_SETTLING_RECOIL_SUPPRESSED"))

        // Final snap settled at rest
        val res4 = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1800L))
        assertFalse("Settled at rest does not increment", res4.isCounted)
        assertEquals(1, res4.sessionCount)
        assertTrue(res4.evidence.contains("SNAP_SETTLED_AT_REST"))
    }

    @Test
    fun `Test 3 - Backward rewatch and positive rebound does not increment count`() {
        // Ground truth sequence from state3_backward_swipe.log:
        // -679 -> -1223 -> +25 (or +279 recoil) -> 0
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        val res1 = counter.processObservation(shortsScrollEvent(scrollY = -679, timestampMs = 1500L))
        assertFalse("Backward swipe must never increment count", res1.isCounted)
        assertEquals(0, res1.sessionCount)
        assertEquals(ScrollDirection.BACKWARD, res1.direction)
        assertTrue(res1.evidence.contains("BACKWARD_GESTURE_START"))

        val res2 = counter.processObservation(shortsScrollEvent(scrollY = -1223, timestampMs = 1600L))
        assertFalse("Backward continuation must not increment", res2.isCounted)
        assertEquals(0, res2.sessionCount)
        assertEquals(ScrollDirection.BACKWARD, res2.direction)

        // Positive rebound recoil (e.g. +279px) must NEVER be treated as forward transition!
        val res3 = counter.processObservation(shortsScrollEvent(scrollY = 279, timestampMs = 1750L))
        assertFalse("Positive rebound following backward swipe must be suppressed", res3.isCounted)
        assertEquals(0, res3.sessionCount)
        assertTrue(res3.evidence.contains("BACKWARD_REBOUND_SUPPRESSED"))

        val res4 = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1900L))
        assertFalse(res4.isCounted)
        assertEquals(0, res4.sessionCount)
    }

    @Test
    fun `Test 4 - Partial or cancelled gesture below threshold snaps to 0 without incrementing`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // User drags partially (+150px)
        val res1 = counter.processObservation(shortsScrollEvent(scrollY = 150, timestampMs = 1500L))
        assertFalse("Sub-threshold drag must not increment", res1.isCounted)
        assertEquals(0, res1.sessionCount)

        // User drags slightly further (+250px, still < 400px)
        val res2 = counter.processObservation(shortsScrollEvent(scrollY = 250, timestampMs = 1550L))
        assertFalse("Sub-threshold drag must not increment", res2.isCounted)
        assertEquals(0, res2.sessionCount)

        // Cancelled gesture snaps back to 0
        val res3 = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1700L))
        assertFalse(res3.isCounted)
        assertEquals(0, res3.sessionCount)
    }

    @Test
    fun `Test 5 - Multiple sub-threshold samples from one slow gesture do not sum to false count`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // 4 samples of 150px each (sum = 600px), but peak is only 150px!
        counter.processObservation(shortsScrollEvent(scrollY = 150, timestampMs = 1500L))
        counter.processObservation(shortsScrollEvent(scrollY = 150, timestampMs = 1550L))
        counter.processObservation(shortsScrollEvent(scrollY = 150, timestampMs = 1600L))
        val res = counter.processObservation(shortsScrollEvent(scrollY = 150, timestampMs = 1650L))

        assertFalse("Absolute displacement never crossed 400px, summing samples is forbidden", res.isCounted)
        assertEquals(0, res.sessionCount)
    }

    @Test
    fun `Test 6 - Multiple samples from one rapid fling count exactly once`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // Rapid fling emits multiple high-displacement samples within 80ms: +503, +775, +1075, +1316
        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 503, timestampMs = 1500L))
        val r2 = counter.processObservation(shortsScrollEvent(scrollY = 775, timestampMs = 1520L))
        val r3 = counter.processObservation(shortsScrollEvent(scrollY = 1075, timestampMs = 1550L))
        val r4 = counter.processObservation(shortsScrollEvent(scrollY = 1316, timestampMs = 1580L))

        assertTrue("First sample above threshold increments", r1.isCounted)
        assertEquals(1, r1.sessionCount)

        assertFalse("Second sample from same gesture suppressed", r2.isCounted)
        assertFalse("Third sample from same gesture suppressed", r3.isCounted)
        assertFalse("Fourth sample from same gesture suppressed", r4.isCounted)
        assertEquals(1, counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1800L)).sessionCount)
    }

    @Test
    fun `Test 7 - Rapid successive gestures properly spaced count each transition`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // Swipe 1
        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1500L))
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1800L))

        // Swipe 2 (750ms after Swipe 1 count, beyond 650ms debounce)
        val r2 = counter.processObservation(shortsScrollEvent(scrollY = 650, timestampMs = 2250L))
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 2500L))

        // Swipe 3
        val r3 = counter.processObservation(shortsScrollEvent(scrollY = 700, timestampMs = 3000L))
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 3300L))

        assertTrue(r1.isCounted)
        assertEquals(1, r1.sessionCount)

        assertTrue(r2.isCounted)
        assertEquals(2, r2.sessionCount)

        assertTrue(r3.isCounted)
        assertEquals(3, r3.sessionCount)
    }

    @Test
    fun `Test 8 - Missing settling event (dropped 0) recovers cleanly on next gesture`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // Gesture 1: +600 -> -143 recoil. The 0 event is DROPPED by the OS/YouTube!
        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1500L))
        val rRecoil = counter.processObservation(shortsScrollEvent(scrollY = -143, timestampMs = 1650L))

        assertTrue(r1.isCounted)
        assertEquals(1, r1.sessionCount)
        assertFalse(rRecoil.isCounted)

        // 900ms later (> 800ms settling timeout, > 650ms debounce), Gesture 2 starts with +700
        val r2 = counter.processObservation(shortsScrollEvent(scrollY = 700, timestampMs = 2550L))
        assertTrue("Next gesture must count even if previous settling 0 was dropped", r2.isCounted)
        assertEquals(2, r2.sessionCount)
    }

    @Test
    fun `Test 9 - Duplicate events within debounce window are suppressed`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1500L))
        val rDup = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1800L))

        assertTrue(r1.isCounted)
        assertEquals(1, r1.sessionCount)

        assertFalse("Duplicate within 650ms debounce window suppressed", rDup.isCounted)
        assertEquals(1, rDup.sessionCount)
    }

    @Test
    fun `Test 10 - Comments panel scrolling is completely excluded`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // While comments panel is open, vertical scrolling occurs in the comments bottom sheet
        val rCommentScroll1 = counter.processObservation(shortsScrollEvent(
            scrollY = 650,
            timestampMs = 1500L,
            isCommentsOpen = true
        ))
        val rCommentScroll2 = counter.processObservation(shortsScrollEvent(
            scrollY = 1200,
            timestampMs = 1700L,
            isCommentsOpen = true
        ))

        assertFalse("Comments panel scroll must be completely ignored", rCommentScroll1.isCounted)
        assertFalse("Comments panel scroll must be completely ignored", rCommentScroll2.isCounted)
        assertEquals(0, rCommentScroll1.sessionCount)
        assertEquals(0, rCommentScroll2.sessionCount)
        assertEquals("COMMENTS_OVERLAY_ACTIVE_IGNORED", rCommentScroll1.evidence)

        // Once comments panel closes (isCommentsOpen = false), normal Shorts navigation resumes
        val rNormalScroll = counter.processObservation(shortsScrollEvent(
            scrollY = 700,
            timestampMs = 2200L,
            isCommentsOpen = false
        ))
        assertTrue("Normal Shorts scroll after comments close counts", rNormalScroll.isCounted)
        assertEquals(1, rNormalScroll.sessionCount)
    }

    @Test
    fun `Test 11 - Home feed and regular video scrolling do not increment`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // Non-shorts events (Home feed, search results, watch player)
        val rHome = counter.processObservation(shortsScrollEvent(scrollY = 800, timestampMs = 1500L, isShorts = false))
        val rWatch = counter.processObservation(shortsScrollEvent(scrollY = 1200, timestampMs = 1800L, isShorts = false))

        assertFalse(rHome.isCounted)
        assertFalse(rWatch.isCounted)
        assertEquals(0, rHome.sessionCount)
        assertEquals(0, rWatch.sessionCount)
        assertEquals("NOT_SHORTS_SURFACE_PRESERVED_STATE", rHome.evidence)
    }

    @Test
    fun `Test 12 - Surface exit tolerance preserves baseline across brief 3s interruptions and resets on extended departure`() {
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline
        counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1500L)) // count = 1
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1800L))

        // Brief 3-second interruption (e.g. ad sheet, share dialog, Play Store sheet)
        val rBriefExit = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 4800L, isShorts = false))
        assertFalse(rBriefExit.isCounted)
        assertEquals(1, rBriefExit.sessionCount)

        // Return within 10s: baseline is PRESERVED, swipe transitions directly to count 2!
        val rResumeSwipe = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 5000L, isShorts = true))
        assertTrue("Baseline was preserved across brief popup, so swipe counts", rResumeSwipe.isCounted)
        assertEquals(2, rResumeSwipe.sessionCount)
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 5300L))

        // Extended departure (> 10s timeout, e.g. away for 15s)
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 21000L, isShorts = false))

        // Re-entering Shorts after 15s: must establish baseline first
        val rReentry = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 21500L, isShorts = true))
        assertFalse("Re-entry after extended absence must establish baseline first", rReentry.isCounted)
        assertEquals(2, rReentry.sessionCount)
        assertEquals(ScrollDirection.INITIAL, rReentry.direction)
        assertEquals("BASELINE_INITIALIZED", rReentry.evidence)

        // Subsequent forward swipe increments count to 3
        val rNext = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 22300L, isShorts = true))
        assertTrue("Subsequent forward swipe counts", rNext.isCounted)
        assertEquals(3, rNext.sessionCount)
    }

    @Test
    fun `Test 13 - Integration with ReelCounterRepository persistence and daily limit`() {
        val fakeStore = FakeKeyValueStore()
        val repository = ReelCounterRepositoryImpl(fakeStore)
        repository.setDailyLimit(2)

        assertEquals(0, repository.getTodayCount())
        assertFalse(repository.isLimitReached())

        // Observation 1: baseline
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L))

        // Observation 2: Short 1 (+1)
        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 1500L))
        if (r1.isCounted) repository.incrementCount()

        assertEquals(1, repository.getTodayCount())
        assertFalse(repository.isLimitReached())

        // Settle
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1800L))

        // Observation 3: Short 2 (+1 to limit reached)
        val r2 = counter.processObservation(shortsScrollEvent(scrollY = 600, timestampMs = 2200L))
        if (r2.isCounted) repository.incrementCount()

        assertEquals(2, repository.getTodayCount())
        assertTrue("Limit should be reached at count 2", repository.isLimitReached())
    }

    @Test
    fun `Test 14 - Instagram regression check and counter independence`() {
        val igCounter = InstagramReelCounter()

        // Instagram baseline
        val igBase = igCounter.processObservation(ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = 0,
            toIndex = 0
        ))
        assertFalse(igBase.isCounted)
        assertEquals(0, igBase.sessionCount)

        // Instagram forward transition
        val igFwd = igCounter.processObservation(ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = 1,
            toIndex = 1
        ))
        assertTrue(igFwd.isCounted)
        assertEquals(1, igFwd.sessionCount)

        // YouTube counter processing concurrently does not interfere with Instagram
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L))
        val ytFwd = counter.processObservation(shortsScrollEvent(scrollY = 500, timestampMs = 1500L))
        assertTrue(ytFwd.isCounted)
        assertEquals(1, ytFwd.sessionCount)

        // Instagram rewatch backwards
        val igRewatch = igCounter.processObservation(ReelObservation(
            isReel = true,
            eventType = "TYPE_VIEW_SCROLLED",
            eventClass = "androidx.viewpager.widget.ViewPager",
            fromIndex = 0,
            toIndex = 0
        ))
        assertFalse(igRewatch.isCounted)
        assertEquals(1, igRewatch.sessionCount)
        assertEquals(1, ytFwd.sessionCount) // YouTube count remains 1
    }

    @Test
    fun `Test 15 - Interleaved non-scroll events (SeekBar, text) during active fling do not cause duplicate count`() {
        // Reproduces the exact real-device Step 47 anomaly:
        // Fling starts (+559px) -> non-scroll progress bar event arrives 450ms later -> late fling inertia (+1094px) arrives
        counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 1000L)) // Baseline

        // Fling sample 1
        val r1 = counter.processObservation(shortsScrollEvent(scrollY = 559, timestampMs = 2000L))
        assertTrue("First fling sample counts", r1.isCounted)
        assertEquals(1, r1.sessionCount)

        // Interleaved non-scroll event (e.g. SeekBar update at 450ms gap)
        val rNonScroll = counter.processObservation(shortsScrollEvent(
            scrollY = 0,
            timestampMs = 2450L,
            eventType = "TYPE_WINDOW_CONTENT_CHANGED",
            className = "android.widget.SeekBar"
        ))
        assertFalse("Non-scroll event does not increment", rNonScroll.isCounted)

        // Late fling inertia sample (+1094px at 566ms from start of fling)
        val rLateSample = counter.processObservation(shortsScrollEvent(scrollY = 1094, timestampMs = 2566L))
        assertFalse("Late inertia sample must be suppressed and NEVER count as duplicate", rLateSample.isCounted)
        assertEquals(1, rLateSample.sessionCount)
        assertTrue(rLateSample.evidence.contains("SAME_GESTURE_SAMPLE_SUPPRESSED"))

        // Final settle
        val rSettle = counter.processObservation(shortsScrollEvent(scrollY = 0, timestampMs = 2800L))
        assertEquals(1, rSettle.sessionCount)
    }

    private class FakeKeyValueStore : KeyValueStore {
        private val data = mutableMapOf<String, Any?>()

        override fun getInt(key: String, defaultValue: Int): Int = (data[key] as? Int) ?: defaultValue
        override fun putInt(key: String, value: Int) { data[key] = value }
        override fun getString(key: String, defaultValue: String?): String? = (data[key] as? String) ?: defaultValue
        override fun putString(key: String, value: String?) { data[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = (data[key] as? Boolean) ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { data[key] = value }
    }
}
