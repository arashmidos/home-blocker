package com.homeblocker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager

/** Box settings, in pixels and in portrait orientation. */
data class BoxConfig(val widthPx: Int, val heightPx: Int, val holdMs: Int, val visible: Boolean)

/** Settings shared by the settings screen and the service. */
object Prefs {
    const val WIDTH = "width_px"
    const val HEIGHT = "height_px"
    const val HOLD = "hold_ms"
    const val SHOW = "show_box"
    private const val DEFAULT_HOLD_MS = 600

    fun of(context: Context): SharedPreferences =
        context.getSharedPreferences("blocker", Context.MODE_PRIVATE)

    fun load(context: Context): BoxConfig {
        val p = of(context)
        return BoxConfig(
            widthPx = p.getInt(WIDTH, screenSides(context).first / 4),
            heightPx = p.getInt(HEIGHT, navBarHeight(context)),
            holdMs = p.getInt(HOLD, DEFAULT_HOLD_MS),
            visible = p.getBoolean(SHOW, false),
        )
    }

    /** Short side and long side of the whole screen, in pixels. */
    fun screenSides(context: Context): Pair<Int, Int> {
        val b = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return minOf(b.width(), b.height()) to maxOf(b.width(), b.height())
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun navBarHeight(context: Context): Int {
        val res = context.resources
        val id = res.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id != 0) res.getDimensionPixelSize(id) else (48 * res.displayMetrics.density).toInt()
    }
}
