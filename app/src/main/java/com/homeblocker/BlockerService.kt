package com.homeblocker

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent

/**
 * Puts a box over the Home button. Accessibility overlays sit above the navigation bar,
 * so the box gets every touch in that area. A tap does nothing. Holding the box goes Home.
 */
class BlockerService : AccessibilityService(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    DisplayManager.DisplayListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private lateinit var displayManager: DisplayManager
    private var box: View? = null
    private var config = BoxConfig(1, 1, 600, false)
    private var rotation = Surface.ROTATION_0

    private val goHome = Runnable {
        box?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WindowManager::class.java)
        displayManager = getSystemService(DisplayManager::class.java)
        Prefs.of(this).registerOnSharedPreferenceChangeListener(this)
        displayManager.registerDisplayListener(this, handler)
        refresh()
    }

    override fun onDestroy() {
        handler.removeCallbacks(goHome)
        if (::windowManager.isInitialized) {
            Prefs.of(this).unregisterOnSharedPreferenceChangeListener(this)
            displayManager.unregisterDisplayListener(this)
            box?.let { windowManager.removeView(it) }
            box = null
        }
        super.onDestroy()
    }

    private fun refresh() {
        config = Prefs.load(this)
        rotation = currentRotation()
        val params = layoutParams()
        val view = box
        if (view == null) {
            box = createBox().also { windowManager.addView(it, params) }
        } else {
            windowManager.updateViewLayout(view, params)
        }
        box?.setBackgroundColor(if (config.visible) BOX_TINT else Color.TRANSPARENT)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBox() = View(this).apply {
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> handler.postDelayed(goHome, config.holdMs.toLong())
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(goHome)
            }
            true // Take every touch, so nothing under the box gets it.
        }
    }

    private fun layoutParams(): LayoutParams {
        // The faulty strip is at the physical bottom of the panel. When the phone turns,
        // that edge moves to a side, and the navigation bar moves with it. Follow that edge.
        val sideways = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        val w = config.widthPx.coerceAtLeast(1)
        val h = config.heightPx.coerceAtLeast(1)
        return LayoutParams(
            if (sideways) h else w,
            if (sideways) w else h,
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                // Without split touch, a ghost tap here during a real touch elsewhere
                // would go to the app under the real finger.
                LayoutParams.FLAG_SPLIT_TOUCH or
                LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = when (rotation) {
                Surface.ROTATION_90 -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
                Surface.ROTATION_180 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                Surface.ROTATION_270 -> Gravity.LEFT or Gravity.CENTER_VERTICAL
                else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            fitInsetsTypes = 0 // Do not push the box up above the navigation bar.
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "HomeBlocker"
        }
    }

    private fun currentRotation() = displayManager.getDisplay(Display.DEFAULT_DISPLAY).rotation

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) = refresh()

    override fun onDisplayChanged(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY && currentRotation() != rotation) refresh()
    }

    override fun onDisplayAdded(displayId: Int) = Unit
    override fun onDisplayRemoved(displayId: Int) = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private companion object {
        const val BOX_TINT = 0x66FF0000
    }
}
