package com.mindlol.probe.detector

import android.graphics.Rect

/**
 * Immutable result model emitted by [InstagramReelDetector].
 *
 * Represents whether an active Instagram Reel surface was detected in the given
 * Accessibility tree structure, alongside bounding box and structural evidence.
 *
 * Note: [authorName] is extracted metadata if available, NOT a unique Reel identifier.
 * No fabricated or permanent Reel IDs are generated.
 */
data class DetectionResult(
    val isReel: Boolean,
    val confidence: Float = 0.0f,
    val activeReelBounds: Rect? = null,
    val pagerBounds: Rect? = null,
    val authorName: String? = null,
    val evidence: String = "NO_REEL_SURFACE"
)
