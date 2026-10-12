package com.mindlol.probe.counter

/**
 * Snap-state transition counter for YouTube Shorts vertical scroll navigation.
 *
 * Ground truth from real-device captures (YouTube 21.40.161 on Android 16):
 * - `fromIndex` and `toIndex` are always reported as -1 on YouTube Shorts.
 * - `TYPE_VIEW_SCROLLED` events report `scrollY` representing the INSTANTANEOUS
 *   vertical scroll offset (mScrollY) of the snap-container relative to the snap anchor (0).
 * - At rest / settled on a Short: `scrollY == 0`.
 * - Forward drag / fling (swiping up): `scrollY > 0` (e.g. +503, +775, +1075, +1316).
 * - Forward settling snap: View overshoots past 0 into negative territory (e.g. -143, -201), then settles at 0.
 * - Backward drag / fling (rewatch): `scrollY < 0` (e.g. -467, -679, -718, -812, -933, -1178, -1223).
 * - Backward settling snap: View overshoots past 0 into positive territory (e.g. +25, +273, +279), then settles at 0.
 *
 * State Machine Architecture:
 * - [SnapState.SETTLED]: View is stationary at snap anchor (scrollY == 0).
 * - [SnapState.FORWARD_GESTURE]: View moving forward (scrollY > 0). Tracks peak forward displacement.
 *   Displacement >= [minForwardScrollPx] emits exactly one increment. Subsequent samples from the same
 *   gesture are suppressed via [hasCountedCurrentGesture].
 * - [SnapState.FORWARD_SETTLING]: View undergoing negative recoil bounce (-143px) after a forward transition.
 *   Recoil is suppressed. Missing settling events (dropped 0) recover via timeout or subsequent gesture.
 * - [SnapState.BACKWARD_GESTURE]: View moving backward (scrollY < 0). Locked as rewatch.
 * - [SnapState.BACKWARD_SETTLING]: View undergoing positive recoil bounce (+25px, +273px, +279px) after
 *   a backward swipe. Rebound recoil is explicitly suppressed and never counted as a forward transition.
 * - Comments Panel: Scroll events while comments panel is open are completely ignored.
 * - Non-Scroll Events: Content changed and text events are filtered out and do not disrupt scroll timing.
 * - Surface Exit: Resets gesture state. Baseline is preserved for brief interruptions (< 10s) such as
 *   in-app dialogs or share sheets, and reset only on extended departure.
 */
class YouTubeShortsCounter(
    private val minForwardScrollPx: Int = DEFAULT_MIN_FORWARD_SCROLL_PX,
    private val debounceWindowMs: Long = DEFAULT_DEBOUNCE_WINDOW_MS,
    private val settlingTimeoutMs: Long = DEFAULT_SETTLING_TIMEOUT_MS,
    private val surfaceExitTimeoutMs: Long = SURFACE_EXIT_TIMEOUT_MS
) {

    enum class SnapState {
        SETTLED,
        FORWARD_GESTURE,
        FORWARD_SETTLING,
        BACKWARD_GESTURE,
        BACKWARD_SETTLING
    }

    private var sessionCount: Int = 0
    private var hasBaseline: Boolean = false
    private var currentState: SnapState = SnapState.SETTLED
    private var peakForwardY: Int = 0
    private var peakBackwardY: Int = 0
    private var hasCountedCurrentGesture: Boolean = false
    private var lastCountTimestampMs: Long = 0L
    private var lastScrollTimestampMs: Long = 0L
    private var lastSurfaceObservedMs: Long = 0L
    private var lastBackwardTimestampMs: Long = 0L

    /**
     * Evaluates a single [ShortsObservation] and returns an immutable [CounterResult].
     */
    fun processObservation(obs: ShortsObservation): CounterResult {
        val now = obs.timestampMs

        // 1. Non-Shorts surface: invalidates pending gesture and preserves session count
        if (!obs.isShorts) {
            resetGestureState()
            if (hasBaseline && lastSurfaceObservedMs > 0L && (now - lastSurfaceObservedMs) > surfaceExitTimeoutMs) {
                hasBaseline = false
            }
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = -1,
                currentIndex = -1,
                evidence = "NOT_SHORTS_SURFACE_PRESERVED_STATE"
            )
        }

        lastSurfaceObservedMs = now

        // 2. Initial Shorts landing: establish baseline without incrementing
        if (!hasBaseline) {
            hasBaseline = true
            resetGestureState()
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.INITIAL,
                previousIndex = -1,
                currentIndex = sessionCount,
                evidence = "BASELINE_INITIALIZED"
            )
        }

        // 3. Comments bottom sheet is open: ignore all comment scrolling and reset pending gesture
        if (obs.isCommentsOpen) {
            resetGestureState()
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = -1,
                currentIndex = sessionCount,
                evidence = "COMMENTS_OVERLAY_ACTIVE_IGNORED"
            )
        }

        // 4. Filter for valid scroll events: non-scroll events (e.g. text/progress bar)
        // must not mutate or disrupt the scroll state machine.
        if (!isValidShortsScrollEvent(obs)) {
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = -1,
                currentIndex = sessionCount,
                evidence = "NON_SCROLL_OR_INVALID_EVENT"
            )
        }

        val scrollY = obs.scrollY
        val timeSinceLastScroll = if (lastScrollTimestampMs > 0L) now - lastScrollTimestampMs else 0L
        lastScrollTimestampMs = now

        // If no scroll activity for longer than settlingTimeoutMs, any pending gesture has settled to rest
        if (currentState != SnapState.SETTLED && timeSinceLastScroll >= settlingTimeoutMs) {
            resetGestureState()
        }

        // 5. Settled at rest (scrollY == 0)
        if (scrollY == 0) {
            val wasState = currentState
            resetGestureState()
            return CounterResult(
                isCounted = false,
                sessionCount = sessionCount,
                direction = ScrollDirection.NONE,
                previousIndex = -1,
                currentIndex = sessionCount,
                evidence = "SNAP_SETTLED_AT_REST (fromState=$wasState)"
            )
        }

        // 6. State Machine Evaluation
        when (currentState) {
            SnapState.SETTLED -> {
                if (scrollY > 0) {
                    // Start forward gesture
                    currentState = SnapState.FORWARD_GESTURE
                    peakForwardY = scrollY
                    hasCountedCurrentGesture = false

                    if (peakForwardY >= minForwardScrollPx && (now - lastCountTimestampMs) >= debounceWindowMs) {
                        sessionCount += 1
                        hasCountedCurrentGesture = true
                        lastCountTimestampMs = now
                        return CounterResult(
                            isCounted = true,
                            sessionCount = sessionCount,
                            direction = ScrollDirection.FORWARD,
                            previousIndex = sessionCount - 1,
                            currentIndex = sessionCount,
                            evidence = "FORWARD_TRANSITION_DISPLACEMENT (peak=$peakForwardY)"
                        )
                    }

                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.FORWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "FORWARD_GESTURE_TRACKING (peak=$peakForwardY)"
                    )
                } else {
                    // Start backward gesture (rewatch)
                    currentState = SnapState.BACKWARD_GESTURE
                    peakBackwardY = scrollY
                    lastBackwardTimestampMs = now
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.BACKWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "BACKWARD_GESTURE_START (peak=$peakBackwardY)"
                    )
                }
            }

            SnapState.FORWARD_GESTURE -> {
                if (scrollY > 0) {
                    peakForwardY = maxOf(peakForwardY, scrollY)

                    // If not yet counted and crosses threshold:
                    if (!hasCountedCurrentGesture && peakForwardY >= minForwardScrollPx) {
                        if ((now - lastCountTimestampMs) >= debounceWindowMs) {
                            sessionCount += 1
                            hasCountedCurrentGesture = true
                            lastCountTimestampMs = now
                            return CounterResult(
                                isCounted = true,
                                sessionCount = sessionCount,
                                direction = ScrollDirection.FORWARD,
                                previousIndex = sessionCount - 1,
                                currentIndex = sessionCount,
                                evidence = "FORWARD_TRANSITION_DISPLACEMENT (peak=$peakForwardY)"
                            )
                        } else {
                            return CounterResult(
                                isCounted = false,
                                sessionCount = sessionCount,
                                direction = ScrollDirection.FORWARD,
                                previousIndex = -1,
                                currentIndex = sessionCount,
                                evidence = "DEBOUNCE_ACTIVE (elapsed=${now - lastCountTimestampMs}ms)"
                            )
                        }
                    }

                    // Already counted or still below threshold
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.FORWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = if (hasCountedCurrentGesture) "SAME_GESTURE_SAMPLE_SUPPRESSED (peak=$peakForwardY)"
                                   else "FORWARD_GESTURE_TRACKING (peak=$peakForwardY)"
                    )
                } else {
                    // Negative recoil bounce observed during forward settling (e.g. -143px)
                    currentState = SnapState.FORWARD_SETTLING
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.NONE,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "FORWARD_SETTLING_RECOIL_SUPPRESSED (delta=$scrollY)"
                    )
                }
            }

            SnapState.FORWARD_SETTLING -> {
                if (scrollY > 0 && (now - lastCountTimestampMs) >= debounceWindowMs) {
                    // New forward gesture started even if explicit 0 was omitted by the system
                    currentState = SnapState.FORWARD_GESTURE
                    peakForwardY = scrollY
                    hasCountedCurrentGesture = false
                    if (peakForwardY >= minForwardScrollPx) {
                        sessionCount += 1
                        hasCountedCurrentGesture = true
                        lastCountTimestampMs = now
                        return CounterResult(
                            isCounted = true,
                            sessionCount = sessionCount,
                            direction = ScrollDirection.FORWARD,
                            previousIndex = sessionCount - 1,
                            currentIndex = sessionCount,
                            evidence = "FORWARD_TRANSITION_DISPLACEMENT (peak=$peakForwardY)"
                        )
                    }
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.FORWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "FORWARD_GESTURE_TRACKING (peak=$peakForwardY)"
                    )
                } else if (scrollY < 0 && (now - lastCountTimestampMs) >= debounceWindowMs) {
                    // New backward gesture started
                    currentState = SnapState.BACKWARD_GESTURE
                    peakBackwardY = scrollY
                    lastBackwardTimestampMs = now
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.BACKWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "BACKWARD_GESTURE_START (peak=$peakBackwardY)"
                    )
                }
                // Ignore any further recoil bounce until view settles at 0 or timeout expires
                return CounterResult(
                    isCounted = false,
                    sessionCount = sessionCount,
                    direction = ScrollDirection.NONE,
                    previousIndex = -1,
                    currentIndex = sessionCount,
                    evidence = "FORWARD_SETTLING_IN_PROGRESS (delta=$scrollY)"
                )
            }

            SnapState.BACKWARD_GESTURE -> {
                if (scrollY < 0) {
                    peakBackwardY = minOf(peakBackwardY, scrollY)
                    lastBackwardTimestampMs = now
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.BACKWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "BACKWARD_GESTURE_TRACKING (peak=$peakBackwardY)"
                    )
                } else {
                    // Positive rebound recoil observed following backward swipe (e.g. +25, +273, +279)
                    currentState = SnapState.BACKWARD_SETTLING
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.NONE,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "BACKWARD_REBOUND_SUPPRESSED (rebound=$scrollY)"
                    )
                }
            }

            SnapState.BACKWARD_SETTLING -> {
                if (scrollY < 0) {
                    // Continuing or new backward gesture
                    currentState = SnapState.BACKWARD_GESTURE
                    peakBackwardY = scrollY
                    lastBackwardTimestampMs = now
                    return CounterResult(
                        isCounted = false,
                        sessionCount = sessionCount,
                        direction = ScrollDirection.BACKWARD,
                        previousIndex = -1,
                        currentIndex = sessionCount,
                        evidence = "BACKWARD_GESTURE_START (peak=$peakBackwardY)"
                    )
                } else if (scrollY > 0) {
                    // If within backward debounce window, suppress as rebound recoil
                    if ((now - lastBackwardTimestampMs) < debounceWindowMs) {
                        return CounterResult(
                            isCounted = false,
                            sessionCount = sessionCount,
                            direction = ScrollDirection.NONE,
                            previousIndex = -1,
                            currentIndex = sessionCount,
                            evidence = "BACKWARD_REBOUND_SUPPRESSED (rebound=$scrollY)"
                        )
                    } else {
                        // Debounce expired: valid subsequent forward gesture
                        currentState = SnapState.FORWARD_GESTURE
                        peakForwardY = scrollY
                        hasCountedCurrentGesture = false
                        if (peakForwardY >= minForwardScrollPx && (now - lastCountTimestampMs) >= debounceWindowMs) {
                            sessionCount += 1
                            hasCountedCurrentGesture = true
                            lastCountTimestampMs = now
                            return CounterResult(
                                isCounted = true,
                                sessionCount = sessionCount,
                                direction = ScrollDirection.FORWARD,
                                previousIndex = sessionCount - 1,
                                currentIndex = sessionCount,
                                evidence = "FORWARD_TRANSITION_DISPLACEMENT (peak=$peakForwardY)"
                            )
                        }
                        return CounterResult(
                            isCounted = false,
                            sessionCount = sessionCount,
                            direction = ScrollDirection.FORWARD,
                            previousIndex = -1,
                            currentIndex = sessionCount,
                            evidence = "FORWARD_GESTURE_TRACKING (peak=$peakForwardY)"
                        )
                    }
                }
                return CounterResult(
                    isCounted = false,
                    sessionCount = sessionCount,
                    direction = ScrollDirection.NONE,
                    previousIndex = -1,
                    currentIndex = sessionCount,
                    evidence = "BACKWARD_SETTLING_IN_PROGRESS (rebound=$scrollY)"
                )
            }
        }
    }

    fun resetSession() {
        sessionCount = 0
        hasBaseline = false
        resetGestureState()
        lastCountTimestampMs = 0L
        lastScrollTimestampMs = 0L
        lastSurfaceObservedMs = 0L
        lastBackwardTimestampMs = 0L
    }

    private fun resetGestureState() {
        currentState = SnapState.SETTLED
        peakForwardY = 0
        peakBackwardY = 0
        hasCountedCurrentGesture = false
    }

    private fun isValidShortsScrollEvent(obs: ShortsObservation): Boolean {
        if (obs.eventType != "TYPE_VIEW_SCROLLED" && obs.eventType != "16") return false
        val cls = obs.eventClass
        return cls.contains("FrameLayout") || cls.contains("RecyclerView") || cls.contains("ViewGroup")
    }

    companion object {
        /**
         * Minimum forward displacement (px) required to register a forward video transition.
         * Real probe captures showed forward gestures peak at +434 to +1316 px.
         * Partial gestures peaked at max +303 px. Backward recoil overshoots peaked at +279 px.
         * 400px cleanly separates genuine forward navigation from partial drags and recoil bounce.
         */
        const val DEFAULT_MIN_FORWARD_SCROLL_PX = 400

        /**
         * Minimum interval between consecutive count increments.
         * Long flings on real device decelerate across up to ~550ms. 650ms guarantees that late
         * inertia samples cannot be counted as separate gestures, while human new-video swipes
         * (> 800ms) pass freely.
         */
        const val DEFAULT_DEBOUNCE_WINDOW_MS = 650L

        /**
         * Inactivity timeout of scroll events after which an uncompleted snap is treated as settled.
         * 800ms ensures that gaps between deceleration events during a single fling do not
         * prematurely trigger state reset.
         */
        const val DEFAULT_SETTLING_TIMEOUT_MS = 800L

        /**
         * Inactivity timeout away from Shorts surface before re-entry requires establishing
         * a new baseline. 10000ms (10 seconds) protects against brief in-app popups, share sheets,
         * or store dialogs destroying the session baseline.
         */
        const val SURFACE_EXIT_TIMEOUT_MS = 10000L
    }
}
