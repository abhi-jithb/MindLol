package com.mindlol.probe.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.mindlol.probe.StatusBroadcaster
import com.mindlol.probe.StatusChangeListener
import com.mindlol.probe.repository.ReelCounterRepository

/**
 * Manages the floating MindLol Reel Count HUD lifecycle and overlay surface.
 *
 * Requirements:
 * - Appears ONLY when inside a supported short-video surface (Instagram Reels, YouTube Shorts).
 * - Automatically hides when leaving supported surfaces (Chrome, Home, Settings, MindLol Flutter App, normal feeds).
 * - Non-interactive (FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE) so it NEVER steals Reel gestures or taps.
 * - Displays the single authoritative count from [ReelCounterRepository].
 * - Dynamically updates text when [StatusBroadcaster] emits count changes.
 */
class FloatingCountHudManager(
    private val context: Context,
    private val repository: ReelCounterRepository,
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager,
    private val permissionChecker: () -> Boolean = { Settings.canDrawOverlays(context) }
) : StatusChangeListener {

    var isHudShowing: Boolean = false
        private set

    private var hudView: View? = null
    private var countTextView: TextView? = null
    private var isSubscribed: Boolean = false

    init {
        StatusBroadcaster.addListener(this)
        isSubscribed = true
    }

    override fun onStatusChanged() {
        updateHudCount()
    }

    /**
     * Evaluates current active package and short-video surface state to show or hide the HUD.
     */
    @Synchronized
    fun updateHudVisibility(
        activePackageName: String?,
        isShortVideoSurface: Boolean,
        isLimitReached: Boolean,
        isTestUnlocked: Boolean
    ) {
        val hasPermission = permissionChecker()
        val isSupportedApp = activePackageName != null &&
            activePackageName in BlockOverlayDecisionEngine.DEFAULT_TARGET_PACKAGES

        // Show HUD only on supported short-video surface when allowed (either limit not reached, or test unlocked)
        val shouldShow = isSupportedApp &&
            isShortVideoSurface &&
            hasPermission &&
            (!isLimitReached || isTestUnlocked)

        if (shouldShow && !isHudShowing) {
            showHud()
        } else if (!shouldShow && isHudShowing) {
            hideHud()
        } else if (isHudShowing) {
            updateHudCount()
        }
    }

    @Synchronized
    fun showHud() {
        if (isHudShowing || hudView != null) return
        if (!permissionChecker()) return

        try {
            val view = createHudView()
            val params = createLayoutParams()
            windowManager.addView(view, params)
            hudView = view
            isHudShowing = true
            updateHudCount()
            Log.d(TAG, "[HUD DEBUG] Floating Count HUD attached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "[HUD DEBUG] Failed to attach Floating Count HUD: ${e.message}", e)
            hudView = null
            isHudShowing = false
        }
    }

    @Synchronized
    fun hideHud() {
        val view = hudView ?: run {
            isHudShowing = false
            return
        }

        try {
            windowManager.removeView(view)
            Log.d(TAG, "[HUD DEBUG] Floating Count HUD detached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "[HUD DEBUG] Failed to remove Floating Count HUD: ${e.message}", e)
        } finally {
            hudView = null
            countTextView = null
            isHudShowing = false
        }
    }

    fun cleanup() {
        if (isSubscribed) {
            StatusBroadcaster.removeListener(this)
            isSubscribed = false
        }
        hideHud()
    }

    private fun updateHudCount() {
        val count = repository.getTodayCount()
        val text = "$count Reels today"
        countTextView?.text = text
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dpToPx(16)
            y = dpToPx(56) // Position below status bar / notch
        }
    }

    private fun createHudView(): View {
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(14).toFloat()
            setColor(Color.parseColor("#E60F6E56")) // Deep teal with 90% opacity
            setStroke(dpToPx(1), Color.parseColor("#4DFAFAF8"))
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = backgroundDrawable
            val paddingH = dpToPx(12)
            val paddingV = dpToPx(6)
            setPadding(paddingH, paddingV, paddingH, paddingV)
        }

        val dotView = View(context).apply {
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4ADE80")) // Soft active green dot
            }
            background = dotDrawable
            val dotSize = dpToPx(6)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                setMargins(0, 0, dpToPx(6), 0)
            }
        }
        container.addView(dotView)

        val tv = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            text = "${repository.getTodayCount()} Reels today"
            includeFontPadding = false
        }
        countTextView = tv
        container.addView(tv)

        return container
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    companion object {
        private const val TAG = "FloatingCountHudManager"
    }
}
