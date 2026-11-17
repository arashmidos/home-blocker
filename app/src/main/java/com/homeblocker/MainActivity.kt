package com.homeblocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** One screen: service status, box size, and hold time. Changes apply at once. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private val pad by lazy { (16 * resources.displayMetrics.density).toInt() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs.of(this)
        val config = Prefs.load(this)
        val (shortSide, longSide) = Prefs.screenSides(this)

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f }
        column.addView(status)
        column.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        column.addView(slider("Box width", "px", config.widthPx, 1, shortSide) {
            prefs.edit().putInt(Prefs.WIDTH, it).apply()
        })
        column.addView(slider("Box height", "px", config.heightPx, 1, longSide / 4) {
            prefs.edit().putInt(Prefs.HEIGHT, it).apply()
        })
        column.addView(slider("Hold to go Home", "ms", config.holdMs, 200, 2000) {
            prefs.edit().putInt(Prefs.HOLD, it).apply()
        })
        column.addView(CheckBox(this).apply {
            text = "Show the box in red while you adjust it"
            isChecked = config.visible
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(Prefs.SHOW, on).apply() }
        })
        column.addView(Button(this).apply {
            text = "Reset to defaults"
            setOnClickListener {
                prefs.edit().clear().apply()
                recreate()
            }
        })
        column.addView(TextView(this).apply {
            setPadding(0, pad, 0, 0)
            text = "Taps on the box do nothing. Hold it to go Home.\n\n" +
                "To size the box, turn on Developer options > Pointer location and watch " +
                "where the ghost taps land.\n\n" +
                "Keep the height at the navigation bar height or less. A taller box also " +
                "covers the middle of the keyboard (the space bar)."
        })

        setContentView(ScrollView(this).apply {
            fitsSystemWindows = true
            addView(column)
        })
    }

    override fun onResume() {
        super.onResume()
        val on = getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
        status.text = if (on) {
            "Blocker is ON."
        } else {
            "Blocker is OFF. Turn on \"Home Blocker\" in Accessibility settings."
        }
    }

    private fun slider(
        label: String, unit: String, value: Int, min: Int, max: Int, save: (Int) -> Unit,
    ): View {
        val title = TextView(this).apply { text = "$label: $value $unit" }
        val bar = SeekBar(this).apply {
            this.min = min
            this.max = max
            progress = value
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, v: Int, fromUser: Boolean) {
                    title.text = "$label: $v $unit"
                    if (fromUser) save(v)
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, pad, 0, 0)
            addView(title)
            addView(bar)
        }
    }
}
