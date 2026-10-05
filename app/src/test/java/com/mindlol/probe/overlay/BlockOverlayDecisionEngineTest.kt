package com.mindlol.probe.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockOverlayDecisionEngineTest {

    @Test
    fun `test 1 - Limit false does not show overlay for Instagram`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.instagram.android",
            isLimitReached = false,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(show)
    }

    @Test
    fun `test 2 - Limit false does not show overlay for YouTube`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.google.android.youtube",
            isLimitReached = false,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(show)
    }

    @Test
    fun `test 3 - Limit true and Instagram active shows overlay`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.instagram.android",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertTrue(show)
    }

    @Test
    fun `test 4 - Limit true and YouTube active shows overlay`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.google.android.youtube",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertTrue(show)
    }

    @Test
    fun `test 5 - Limit true and unrelated app active does NOT show overlay`() {
        val chromeShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.android.chrome",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(chromeShow)

        val probeShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.mindlol.probe.debug",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(probeShow)

        val launcherShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.sec.android.app.launcher",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(launcherShow)
    }

    @Test
    fun `test 6 - Permission denied prevents showing overlay even when limit reached`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.instagram.android",
            isLimitReached = true,
            hasOverlayPermission = false,
            isTestUnlocked = false
        )
        assertFalse(show)
    }

    @Test
    fun `test 7 - Test unlocked session prevents showing overlay`() {
        val show = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "com.instagram.android",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = true
        )
        assertFalse(show)
    }

    @Test
    fun `test 8 - Null or blank package name does not show overlay`() {
        val nullShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = null,
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(nullShow)

        val blankShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = "  ",
            isLimitReached = true,
            hasOverlayPermission = true,
            isTestUnlocked = false
        )
        assertFalse(blankShow)
    }
}
