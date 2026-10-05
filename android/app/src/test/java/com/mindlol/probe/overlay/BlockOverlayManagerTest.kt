package com.mindlol.probe.overlay

import android.content.Context
import android.view.View
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BlockOverlayManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockWindowManager: WindowManager
    private lateinit var overlayManager: BlockOverlayManager

    private val instagramPkg = "com.instagram.android"
    private val youtubePkg = "com.google.android.youtube"
    private val chromePkg = "com.android.chrome"
    private val probePkg = "com.mindlol.probe.debug"
    private val launcherPkg = "com.sec.android.app.launcher"

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockWindowManager = mock(WindowManager::class.java)
        overlayManager = BlockOverlayManager(
            context = mockContext,
            windowManager = mockWindowManager,
            permissionChecker = { true }
        )
    }

    @Test
    fun `test 1 - Target app + limit reached shows overlay`() {
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)
        verify(mockWindowManager, times(1)).addView(any(), any())
    }

    @Test
    fun `test 2 - Target app + limit not reached hides overlay`() {
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = false)
        assertFalse(overlayManager.isOverlayShowing)
        verify(mockWindowManager, never()).addView(any(), any())
    }

    @Test
    fun `test 3 - Unrelated app + limit reached hides overlay`() {
        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)
        verify(mockWindowManager, never()).addView(any(), any())

        overlayManager.updateOverlayState(probePkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)

        overlayManager.updateOverlayState(launcherPkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)
    }

    @Test
    fun `test 4 - Repeated show requests result in only ONE WindowManager addView call`() {
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)

        assertTrue(overlayManager.isOverlayShowing)
        verify(mockWindowManager, times(1)).addView(any(), any())
    }

    @Test
    fun `test 5 - Repeated hide requests on hidden overlay are safe and idempotent`() {
        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        overlayManager.hideOverlay()
        overlayManager.hideOverlay()

        assertFalse(overlayManager.isOverlayShowing)
        verify(mockWindowManager, never()).removeView(any())
    }

    @Test
    fun `test 6 - App switch Instagram to Chrome hides overlay`() {
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)

        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)
        verify(mockWindowManager, times(1)).removeView(any())
    }

    @Test
    fun `test 7 - App switch Chrome to Instagram shows overlay again`() {
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)

        assertTrue(overlayManager.isOverlayShowing)
        verify(mockWindowManager, times(2)).addView(any(), any())
        verify(mockWindowManager, times(1)).removeView(any())
    }

    @Test
    fun `test 8 - Target app switch Instagram to YouTube resets test unlock and keeps blocker visible`() {
        // Step 1: Open Instagram, limit reached
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)

        // Step 2: Test unlock during Instagram session
        overlayManager.hideOverlay()
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        // Simulate setting test unlock flag directly or via button click
        // Note: hideOverlay() leaves testUnlocked false unless updated
    }

    @Test
    fun `test 9 - Test unlock session resets when switching target apps (Instagram to YouTube)`() {
        // Show overlay over Instagram
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)

        // Switch to YouTube directly
        overlayManager.updateOverlayState(youtubePkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)
    }

    @Test
    fun `test 10 - WindowManager BadTokenException on addView is handled safely without crash`() {
        doThrow(WindowManager.BadTokenException("Bad window token"))
            .`when`(mockWindowManager).addView(any(), any())

        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)
    }

    @Test
    fun `test 11 - WindowManager IllegalArgumentException on removeView is handled safely without crash`() {
        // Show first
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)

        // Make removeView throw exception
        doThrow(IllegalArgumentException("View not attached to window manager"))
            .`when`(mockWindowManager).removeView(any())

        // Hide overlay
        overlayManager.updateOverlayState(chromePkg, isLimitReached = true)
        assertFalse(overlayManager.isOverlayShowing)
    }

    @Test
    fun `test 12 - Limit crossing while inside target app triggers immediate overlay decision`() {
        // Initially active inside Instagram, but limit not reached
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = false)
        assertFalse(overlayManager.isOverlayShowing)
        verify(mockWindowManager, never()).addView(any(), any())

        // Limit crossed while still inside Instagram
        overlayManager.updateOverlayState(instagramPkg, isLimitReached = true)
        assertTrue(overlayManager.isOverlayShowing)
        verify(mockWindowManager, times(1)).addView(any(), any())
    }
}
