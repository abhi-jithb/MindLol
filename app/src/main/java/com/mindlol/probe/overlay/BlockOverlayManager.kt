package com.mindlol.probe.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.mindlol.probe.BuildConfig

/**
 * Manages the WindowManager blocking overlay lifecycle and surface.
 *
 * Responsibilities:
 * - Owns WindowManager overlay view creation, attachment, and detachment.
 * - Does NOT calculate reel counts or modify repository state.
 * - Does NOT execute autonomous navigation or close target apps (Google Accessibility Policy compliant).
 */
open class BlockOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
    private val permissionChecker: () -> Boolean = { Settings.canDrawOverlays(context) }
) {

    var isOverlayShowing: Boolean = false
        private set

    var isTestUnlocked: Boolean = false
        private set

    private var lastTargetPackage: String? = null
    private var overlayView: View? = null

    /**
     * Evaluates current application and limit state, showing or hiding the overlay as appropriate.
     * Safe to call repeatedly on every accessibility or window state event.
     */
    @Synchronized
    fun updateOverlayState(activePackageName: String?, isLimitReached: Boolean) {
        // Track target package switches and session changes for test unlock
        if (activePackageName != null &&
            activePackageName != "android" &&
            activePackageName != "com.android.systemui" &&
            activePackageName != context.packageName
        ) {
            if (activePackageName in BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES) {
                if (lastTargetPackage != null && lastTargetPackage != activePackageName) {
                    // Target package changed (e.g. Instagram -> YouTube), reset test unlock for new session
                    isTestUnlocked = false
                }
                lastTargetPackage = activePackageName
            } else {
                // User left target app (e.g. Home launcher, Chrome, MindLol probe), reset test unlock and last target
                isTestUnlocked = false
                lastTargetPackage = null
            }
        }

        val hasPermission = hasOverlayPermission()
        val shouldShow = BlockOverlayDecisionEngine.shouldShowOverlay(
            activePackageName = activePackageName,
            isLimitReached = isLimitReached,
            hasOverlayPermission = hasPermission,
            isTestUnlocked = isTestUnlocked
        )

        if (shouldShow && !isOverlayShowing) {
            showOverlay()
        } else if (!shouldShow && isOverlayShowing) {
            hideOverlay()
        }
    }

    /**
     * Checks whether SYSTEM_ALERT_WINDOW permission is currently granted.
     */
    fun hasOverlayPermission(): Boolean {
        return permissionChecker()
    }

    /**
     * Creates and attaches the blocking overlay view to WindowManager.
     * Duplicate-safe and exception-guarded.
     */
    @Synchronized
    fun showOverlay() {
        if (isOverlayShowing || overlayView != null) return
        if (!hasOverlayPermission()) {
            Log.w(TAG, "[OVERLAY DEBUG] Cannot show overlay: SYSTEM_ALERT_WINDOW permission not granted.")
            return
        }

        try {
            Log.d(TAG, "[OVERLAY DEBUG] showOverlay() called")
            val view = createMinimalOverlayView()
            val params = createLayoutParams()

            Log.d(TAG, "[OVERLAY DEBUG] calling WindowManager.addView()")
            windowManager.addView(view, params)
            overlayView = view
            isOverlayShowing = true
            Log.d(TAG, "[OVERLAY DEBUG] WindowManager.addView() succeeded")
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "[OVERLAY DEBUG] WindowManager.addView() failed: BadTokenException", e)
            overlayView = null
            isOverlayShowing = false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "[OVERLAY DEBUG] WindowManager.addView() failed: IllegalStateException (view already attached)", e)
            overlayView = null
            isOverlayShowing = false
        } catch (e: Exception) {
            Log.e(TAG, "[OVERLAY DEBUG] WindowManager.addView() failed: unexpected ${e.javaClass.simpleName}: ${e.message}", e)
            overlayView = null
            isOverlayShowing = false
        }
    }

    /**
     * Detaches and removes the blocking overlay view from WindowManager.
     * Idempotent, duplicate-safe, and exception-guarded.
     */
    @Synchronized
    fun hideOverlay() {
        val view = overlayView ?: run {
            isOverlayShowing = false
            return
        }

        try {
            Log.d(TAG, "[OVERLAY DEBUG] hideOverlay() called")
            windowManager.removeView(view)
            Log.d(TAG, "[OVERLAY DEBUG] WindowManager.removeView() succeeded")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "[OVERLAY DEBUG] WindowManager.removeView() failed: View not attached to WindowManager", e)
        } catch (e: Exception) {
            Log.e(TAG, "[OVERLAY DEBUG] WindowManager.removeView() failed: unexpected ${e.javaClass.simpleName}: ${e.message}", e)
        } finally {
            overlayView = null
            isOverlayShowing = false
        }
    }

    /**
     * Resets temporary developer test unlock state.
     */
    fun resetTestUnlock() {
        isTestUnlocked = false
        lastTargetPackage = null
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    /**
     * Constructs a minimal, non-distracting test blocker layout.
     */
    private fun createMinimalOverlayView(): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F20F172A")) // Opaque dark slate
            setPadding(64, 64, 64, 64)
            isClickable = true
            isFocusable = true
        }

        val titleView = TextView(context).apply {
            text = "Daily Limit Reached"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(titleView)

        val descView = TextView(context).apply {
            text = "You've reached your Reel limit for today."
            setTextColor(Color.parseColor("#94A3B8")) // Light slate grey
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }
        rootLayout.addView(descView)

        // Temporary test unlock control for development validation only.
        // Gated behind BuildConfig.DEBUG so production release builds do not expose developer unlock.
        if (BuildConfig.DEBUG) {
            val testUnlockBtn = Button(context).apply {
                text = "[ TEST UNLOCK ]"
                setOnClickListener {
                    Log.i(TAG, "Temporary test unlock button clicked by user.")
                    isTestUnlocked = true
                    hideOverlay()
                }
            }
            rootLayout.addView(testUnlockBtn)
        }

        return rootLayout
    }

    companion object {
        private const val TAG = "BlockOverlayManager"
    }
}
