package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Visual reproduction of UNI Launcher's Clock & Weather Combo Card.
 */
class ClockWeatherCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tempView: TextView
    private val conditionView: TextView
    private val aqiPill: TextView
    private val highLowView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // Left: Big Digital Clock + Date
        val leftLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.2f)
        }

        val clockTime = TextClock(context).apply {
            format12Hour = "HH:mm"
            format24Hour = "HH:mm"
            setTextColor(Color.WHITE)
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
        }
        val clockDate = TextClock(context).apply {
            format12Hour = "M月d日 EEEE"
            format24Hour = "M月d日 EEEE"
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 13f
            setPadding(0, dpToPx(2f), 0, 0)
        }
        leftLayout.addView(clockTime)
        leftLayout.addView(clockDate)
        addView(leftLayout)

        // Divider
        val divider = LinearLayout(context).apply {
            layoutParams = LayoutParams(dpToPx(1f), dpToPx(60f)).apply {
                marginStart = dpToPx(12f)
                marginEnd = dpToPx(16f)
            }
            setBackgroundColor(Color.parseColor("#1FFFFFFF"))
        }
        addView(divider)

        // Right: Weather details
        val rightLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        tempView = TextView(context).apply {
            text = "18°C"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        conditionView = TextView(context).apply {
            text = "晴"
            setTextColor(Color.parseColor("#6FD3FF")) // Cyan
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8f), 0, 0, 0)
        }
        topRow.addView(tempView)
        topRow.addView(conditionView)
        rightLayout.addView(topRow)

        highLowView = TextView(context).apply {
            text = "22° / 12°"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 12f
            setPadding(0, dpToPx(2f), 0, dpToPx(4f))
        }
        rightLayout.addView(highLowView)

        aqiPill = TextView(context).apply {
            text = "优 AQI 26"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_pill_badge)
            setPadding(dpToPx(8f), dpToPx(2f), dpToPx(8f), dpToPx(2f))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        rightLayout.addView(aqiPill)

        addView(rightLayout)

        setOnClickListener {
            try {
                LauncherRuntime.get(context).handle(JSONObject().put("action", "weather-forecast-refresh"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateWeather(weather: JSONObject) {
        val temp = weather.optString("temp", "18")
        val cond = weather.optString("weather", "晴")
        val aqi = weather.optString("aqi", "28")
        val high = weather.optString("tempHigh", "22")
        val low = weather.optString("tempLow", "12")

        tempView.text = "$temp°C"
        conditionView.text = cond
        highLowView.text = "$high° / $low°"
        aqiPill.text = "空气质量 $aqi"
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

