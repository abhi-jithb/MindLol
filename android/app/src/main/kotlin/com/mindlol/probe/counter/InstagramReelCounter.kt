package com.mindlol.probe.counter

/**
 * Pure, stateful direction-aware counter for Instagram Reel ViewPager navigation transitions.
 *
 * Tracks relative ViewPager index progression across accessibility events to determine whether
 * a user has navigated forward to a NEW Reel in the active session.
 *
 * Note on Identity & Semantics:
 * - This counter relies on relative ViewPager index transitions (`toIndex > lastConfirmedIndex`)
 *   within a session.
 * - The ViewPager index is a position marker in the viewer pager, NOT a globally unique Reel ID.
 * - Creator usernames are NOT used as Reel identities (the same creator can post multiple reels).
 *
 * Counter Rules:
 * - First valid Reel: baseline initialization only (count = 0).
 * - Forward transition (`currentIndex > prevIndex`): count +1.
 * - Backward transition (`currentIndex < prevIndex`, rewatching): count +0.
 * - Same index (`currentIndex == prevIndex`): count +0.
 * - Invalid/non-Reel surface (`isReel = false`): preserve state, count +0.
 */
class InstagramReelCounter {

    private var lastConfirmedIndex: Int = INDEX_UNSET
    private var sessionCount: Int = 0
    private var hasBaseline: Boolean = false

    /**
     * Evaluates a single [ReelObservation] and returns an immutable [CounterResult].
     */
    fun processObservation(obs: ReelObservation): CounterResult {
        // 1. Temporary detection loss or non-Reel surface: preserve navigation state
        if (!obs.isReel) {
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = lastConfirmedIndex,
                currentIndex = lastConfirmedIndex,
                evidence = "DETECTOR_NOT_REEL_PRESERVED_STATE"
            )
        }

        // 2. Filter for relevant ViewPager scroll events with valid index data
        if (!isValidViewPagerScrollEvent(obs)) {
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = lastConfirmedIndex,
                currentIndex = lastConfirmedIndex,
                evidence = "NON_VIEW_PAGER_OR_INVALID_EVENT"
            )
        }

        // 3. Comments bottom sheet is open: ignore all comment scrolling and suppress increment
        if (obs.isCommentsOpen) {
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = lastConfirmedIndex,
                currentIndex = lastConfirmedIndex,
                evidence = "COMMENTS_OVERLAY_ACTIVE_IGNORED"
            )
        }

        val currentIndex = obs.toIndex

        // 3. Initial Reel landing: establish baseline without incrementing
        if (!hasBaseline) {
            hasBaseline = true
            lastConfirmedIndex = currentIndex
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.INITIAL,
                previousIndex = INDEX_UNSET,
                currentIndex = currentIndex,
                evidence = "BASELINE_INITIALIZED"
            )
        }

        val prevIndex = lastConfirmedIndex

        // 4. Repeated observations of the same index
        if (currentIndex == prevIndex) {
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = prevIndex,
                currentIndex = currentIndex,
                evidence = "SAME_INDEX_REPEAT"
            )
        }

        // 5. Forward transition (currentIndex > prevIndex)
        if (currentIndex > prevIndex) {
            sessionCount += 1
            lastConfirmedIndex = currentIndex
            return CounterResult(
                isCounted = true,
                sessionCount = sessionCount,
                direction = ScrollDirection.FORWARD,
                previousIndex = prevIndex,
                currentIndex = currentIndex,
                evidence = "FORWARD_TRANSITION"
            )
        }

        // 6. Backward transition (currentIndex < prevIndex)
        lastConfirmedIndex = currentIndex
        return CounterResult(
            isCounted = false,
            sessionCount = sessionCount,
            direction = ScrollDirection.BACKWARD,
            previousIndex = prevIndex,
            currentIndex = currentIndex,
            evidence = "BACKWARD_TRANSITION"
        )
    }

    fun resetSession() {
        lastConfirmedIndex = INDEX_UNSET
        sessionCount = 0
        hasBaseline = false
    }

    private fun isValidViewPagerScrollEvent(obs: ReelObservation): Boolean {
        // Must be a scroll event
        if (obs.eventType != "TYPE_VIEW_SCROLLED" && obs.eventType != "16") return false
        // Must be emitted by ViewPager (not RecyclerView tray or generic view)
        val isViewPagerClass = obs.eventClass.contains("ViewPager") || obs.eventClass.contains("clips_viewer")
        if (!isViewPagerClass) return false
        // Must have valid non-negative index
        if (obs.toIndex < 0) return false
        return true
    }

    companion object {
        const val INDEX_UNSET = -1
    }
}
