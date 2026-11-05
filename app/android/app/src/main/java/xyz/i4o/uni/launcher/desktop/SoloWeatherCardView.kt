package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * Native Weather Details Card for UNI Launcher.
 * Features city locator, large temperature, weather condition icon, humidity, wind, and forecast.
 */
class SoloWeatherCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val cityView: TextView
    private val conditionPill: TextView
    private val tempView: TextView
    private val conditionDescView: TextView
    private val tempRangeView: TextView
    private val forecastStrip: LinearLayout

    init {
        orientation = VERTICAL
        val pad = dpToPx(18f)
        setPadding(pad, pad, pad, pad)

        // 1. Header: City & Weather Source
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        cityView = TextView(context).apply {
            text = "杭州市 · 滨江区"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        conditionPill = TextView(context).apply {
            text = "空气优 24"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(cityView)
        header.addView(conditionPill)
        addView(header)

        // 2. Center Temp Row
        val centerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(6f)
            }
        }

        val tempCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val numRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        tempView = TextView(context).apply {
            text = "18"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

        val degreeUnit = TextView(context).apply {
            text = "°C"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 16f
            setPadding(dpToPx(2f), 0, 0, dpToPx(6f))
        }

        numRow.addView(tempView)
        numRow.addView(degreeUnit)
        tempCol.addView(numRow)

        tempRangeView = TextView(context).apply {
            text = "今日 12° ~ 21° · 东北风 2级"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
        }
        tempCol.addView(tempRangeView)
        centerRow.addView(tempCol)

        // Right side: Weather Condition Badge
        val condCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        conditionDescView = TextView(context).apply {
            text = "晴朗"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        }

        condCol.addView(conditionDescView)
        centerRow.addView(condCol)

        addView(centerRow)

        // 3. Mini Forecast Strip
        forecastStrip = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(4f)
            }
        }

        val days = listOf("周五" to "多云 19°", "周六" to "小雨 16°", "周日" to "多云 18°", "周一" to "晴朗 20°")
        for ((d, w) in days) {
            val item = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            val dView = TextView(context).apply {
                text = d
                setTextColor(Color.parseColor("#8A99AD"))
                textSize = 11f
            }
            val wView = TextView(context).apply {
                text = w
                setTextColor(Color.parseColor("#EFF4F6"))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dpToPx(2f), 0, 0)
            }
            item.addView(dView)
            item.addView(wView)
            forecastStrip.addView(item)
        }
        addView(forecastStrip)
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

