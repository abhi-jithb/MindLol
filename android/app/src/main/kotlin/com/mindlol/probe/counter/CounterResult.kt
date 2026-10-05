package com.mindlol.probe.counter

enum class ScrollDirection {
    INITIAL,
    NONE,
    FORWARD,
    BACKWARD,
    UNKNOWN
}

/**
 * Immutable decision emitted by [InstagramReelCounter] for a single event observation.
 */
data class CounterResult(
    val isCounted: Boolean,
    val sessionCount: Int,
    val direction: ScrollDirection,
    val previousIndex: Int,
    val currentIndex: Int,
    val evidence: String
)
