package com.mindlol.probe.counter

/**
 * Normalized navigation observation model passed into [InstagramReelCounter].
 */
data class ReelObservation(
    val isReel: Boolean,
    val eventType: String,
    val eventClass: String,
    val fromIndex: Int,
    val toIndex: Int
)
