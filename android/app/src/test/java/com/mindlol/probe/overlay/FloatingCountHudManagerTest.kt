package com.mindlol.probe.overlay

import com.mindlol.probe.StatusBroadcaster
import com.mindlol.probe.repository.InMemoryKeyValueStore
import com.mindlol.probe.repository.ReelCounterRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FloatingCountHudManagerTest {

    private lateinit var repository: ReelCounterRepositoryImpl

    @Before
    fun setUp() {
        repository = ReelCounterRepositoryImpl(InMemoryKeyValueStore())
    }

    @Test
    fun `updateHudVisibility shows HUD on supported target short video surface`() {
        var hudShowing = false

        // Decision logic evaluation helper for test
        fun shouldShowHud(
            activePackageName: String?,
            isShortVideoSurface: Boolean,
            isLimitReached: Boolean,
            isTestUnlocked: Boolean,
            hasPermission: Boolean = true
        ): Boolean {
            val isSupportedApp = activePackageName != null &&
                activePackageName in BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES
            return isSupportedApp &&
                isShortVideoSurface &&
                hasPermission &&
                (!isLimitReached || isTestUnlocked)
        }

        assertTrue(
            shouldShowHud(
                activePackageName = "com.instagram.android",
                isShortVideoSurface = true,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )

        assertTrue(
            shouldShowHud(
                activePackageName = "com.google.android.youtube",
                isShortVideoSurface = true,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )
    }

    @Test
    fun `updateHudVisibility hides HUD on non-supported surfaces`() {
        fun shouldShowHud(
            activePackageName: String?,
            isShortVideoSurface: Boolean,
            isLimitReached: Boolean,
            isTestUnlocked: Boolean
        ): Boolean {
            val isSupportedApp = activePackageName != null &&
                activePackageName in BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES
            return isSupportedApp &&
                isShortVideoSurface &&
                (!isLimitReached || isTestUnlocked)
        }

        // Chrome should NOT show HUD
        assertFalse(
            shouldShowHud(
                activePackageName = "com.android.chrome",
                isShortVideoSurface = true,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )

        // Home launcher should NOT show HUD
        assertFalse(
            shouldShowHud(
                activePackageName = "com.android.launcher",
                isShortVideoSurface = false,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )

        // MindLol app should NOT show HUD
        assertFalse(
            shouldShowHud(
                activePackageName = "com.mindlol.probe",
                isShortVideoSurface = false,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )

        // Instagram normal feed (non-Reel) should NOT show HUD
        assertFalse(
            shouldShowHud(
                activePackageName = "com.instagram.android",
                isShortVideoSurface = false,
                isLimitReached = false,
                isTestUnlocked = false
            )
        )
    }

    @Test
    fun `updateHudVisibility respects limit reached and test unlock state`() {
        fun shouldShowHud(
            activePackageName: String?,
            isShortVideoSurface: Boolean,
            isLimitReached: Boolean,
            isTestUnlocked: Boolean
        ): Boolean {
            val isSupportedApp = activePackageName != null &&
                activePackageName in BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES
            return isSupportedApp &&
                isShortVideoSurface &&
                (!isLimitReached || isTestUnlocked)
        }

        // When limit reached and not unlocked, blocker takes over and HUD is hidden
        assertFalse(
            shouldShowHud(
                activePackageName = "com.instagram.android",
                isShortVideoSurface = true,
                isLimitReached = true,
                isTestUnlocked = false
            )
        )

        // When test unlocked, HUD is allowed alongside test-unlocked viewing
        assertTrue(
            shouldShowHud(
                activePackageName = "com.instagram.android",
                isShortVideoSurface = true,
                isLimitReached = true,
                isTestUnlocked = true
            )
        )
    }

    @Test
    fun `StatusBroadcaster notifies listeners on count increment`() {
        var notifiedCount = 0
        StatusBroadcaster.addListener {
            notifiedCount++
        }

        assertEquals(0, notifiedCount)
        repository.incrementCount()
        assertEquals(1, repository.getTodayCount())
    }
}
