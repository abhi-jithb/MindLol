package com.mindlol.probe.health

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackingHealthManagerTest {

    @Before
    fun setUp() {
        TrackingHealthManager.resetForTesting()
    }

    @After
    fun tearDown() {
        TrackingHealthManager.resetForTesting()
    }

    @Test
    fun `Test 1 - Accessibility service disconnected yields PERMISSION_REQUIRED`() {
        // Service not connected
        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = false,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.PERMISSION_REQUIRED, health.state)
        assertEquals("OPEN_ACCESSIBILITY_SETTINGS", health.actionRequired)
        assertTrue(health.message.contains("Accessibility"))
    }

    @Test
    fun `Test 2 - Missing overlay permission yields PERMISSION_REQUIRED with overlay action`() {
        TrackingHealthManager.onServiceConnected()

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = false,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.PERMISSION_REQUIRED, health.state)
        assertEquals("OPEN_OVERLAY_SETTINGS", health.actionRequired)
        assertTrue(health.message.contains("Display over other apps"))
    }

    @Test
    fun `Test 3 - Both supported apps disabled in settings yields TRACKING_PAUSED`() {
        TrackingHealthManager.onServiceConnected()

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = false,
            isYouTubeEnabled = false
        )

        assertEquals(TrackingHealthState.TRACKING_PAUSED, health.state)
        assertEquals("OPEN_SETTINGS", health.actionRequired)
        assertTrue(health.message.contains("turned off in Settings"))
    }

    @Test
    fun `Test 4 - Permissions granted and no target app active yields STANDBY without alarm`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState("com.mindlol.probe", false, "NON_TARGET_APP")

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.STANDBY, health.state)
        assertNull(health.actionRequired)
        assertTrue(health.message.contains("automatically when you open Reels or Shorts"))
        assertFalse(health.isShortVideoSurface)
    }

    @Test
    fun `Test 5 - Instagram Reel active surface yields TRACKING`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState("com.instagram.android", true, "REEL_VIEW_PAGER_FOUND")

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.TRACKING, health.state)
        assertTrue(health.isShortVideoSurface)
        assertEquals("com.instagram.android", health.activePackageName)
        assertTrue(health.message.contains("Instagram Reels"))
    }

    @Test
    fun `Test 6 - YouTube Shorts active surface yields TRACKING`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState("com.google.android.youtube", true, "SHORTS_FRAGMENT_ROOT_FOUND")

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.TRACKING, health.state)
        assertTrue(health.isShortVideoSurface)
        assertEquals("com.google.android.youtube", health.activePackageName)
        assertTrue(health.message.contains("YouTube Shorts"))
    }

    @Test
    fun `Test 7 - Target app foreground with obfuscated or uncertain tree yields TRACKING_UNCERTAIN`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState(
            "com.google.android.youtube",
            false,
            "UNCERTAIN_STRUCTURE_NO_MATCH"
        )

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.TRACKING_UNCERTAIN, health.state)
        assertFalse(health.isShortVideoSurface)
        assertTrue(health.message.contains("preserved"))
    }

    @Test
    fun `Test 8 - Target app normal home feed yields STANDBY without false alarms`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState(
            "com.google.android.youtube",
            false,
            "YOUTUBE_HOME_FEED"
        )

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = true,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.STANDBY, health.state)
        assertFalse(health.isShortVideoSurface)
    }

    @Test
    fun `Test 9 - Disconnecting service transitions back to PERMISSION_REQUIRED`() {
        TrackingHealthManager.onServiceConnected()
        TrackingHealthManager.updateSurfaceState("com.instagram.android", true, "REEL_VIEW_PAGER_FOUND")

        // User revokes or stops service
        TrackingHealthManager.onServiceDisconnected()

        val health = TrackingHealthManager.resolveHealth(
            isAccessibilityServiceEnabled = false,
            isOverlayPermissionGranted = true,
            isInstagramEnabled = true,
            isYouTubeEnabled = true
        )

        assertEquals(TrackingHealthState.PERMISSION_REQUIRED, health.state)
        assertFalse(health.isShortVideoSurface)
    }
}
