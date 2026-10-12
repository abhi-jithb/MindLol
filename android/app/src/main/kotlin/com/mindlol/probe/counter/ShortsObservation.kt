package com.mindlol.probe.counter

/**
 * Normalized navigation observation model passed into [YouTubeShortsCounter].
 *
 * @param isShorts Whether an active YouTube Shorts surface is detected.
 * @param eventType Accessibility event type string or integer ID.
 * @param eventClass Class name of the source or event.
 * @param scrollX Horizontal scroll coordinate/offset.
 * @param scrollY Instantaneous vertical scroll offset (mScrollY) relative to snap anchor.
 * @param isCommentsOpen Whether the comments bottom-sheet engagement panel is currently visible.
 * @param timestampMs Event timestamp in epoch milliseconds.
 */
data class ShortsObservation(
    val isShorts: Boolean,
    val eventType: String,
    val eventClass: String,
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val isCommentsOpen: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)
