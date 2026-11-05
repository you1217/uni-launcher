package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.R

/**
 * Native Air Quality Card for UNI Launcher.
 * Displays vehicle cabin PM2.5 reading, AQI grade, and Air Purifier status.
 */
class AirQualityCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val aqiPillView: TextView
    private val pm25ValueView: TextView
    private val aqiLevelDescView: TextView
    private val purifierProgressBar: ProgressBar
    private val purifierStatusView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(18f)
        setPadding(pad, pad, pad, pad)

        // 1. Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        titleView = TextView(context).apply {
            text = "车内空气"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        aqiPillView = TextView(context).apply {
            text = "空气优"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 12f
            setBackgroundResource(R.drawable.bg_pill_badge)
            val hp = dpToPx(10f)
            val vp = dpToPx(4f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(aqiPillView)
        addView(header)

        // 2. Center PM2.5 Big Number Row
        val contentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(8f)
            }
        }

        val pm25Col = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val pmLabel = TextView(context).apply {
            text = "PM2.5 浓度"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
        }

        val numRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        pm25ValueView = TextView(context).apply {
            text = "12"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

        val unitView = TextView(context).apply {
            text = " μg/m³"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
            setPadding(0, 0, 0, dpToPx(6f))
        }

        numRow.addView(pm25ValueView)
        numRow.addView(unitView)
        pm25Col.addView(pmLabel)
        pm25Col.addView(numRow)
        contentRow.addView(pm25Col)

        // Right side: AQI & AQS status
        val aqiCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val aqiLabel = TextView(context).apply {
            text = "车外 AQS"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
            gravity = Gravity.END
        }

        aqiLevelDescView = TextView(context).apply {
            text = "智能阻隔已开启"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            setPadding(0, dpToPx(4f), 0, 0)
        }

        aqiCol.addView(aqiLabel)
        aqiCol.addView(aqiLevelDescView)
        contentRow.addView(aqiCol)

        addView(contentRow)

        // 3. Bottom: Air Purifier Progress Bar & Status
        val bottomSection = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        purifierProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 88
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(6f)).apply {
                bottomMargin = dpToPx(6f)
            }
        }

        val footerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val footerLeft = TextView(context).apply {
            text = "主动座舱清洁系统 (Clean Zone)"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 11f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        purifierStatusView = TextView(context).apply {
            text = "运行中"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
        }

        footerRow.addView(footerLeft)
        footerRow.addView(purifierStatusView)

        bottomSection.addView(purifierProgressBar)
        bottomSection.addView(footerRow)
        addView(bottomSection)
    }

    fun updateTelemetry(vehicle: JSONObject) {
        val pmVal = vehicle.optInt("pm25", 12)
        pm25ValueView.text = "$pmVal"
        if (pmVal <= 35) {
            pm25ValueView.setTextColor(Color.parseColor("#3BE2C0"))
            aqiPillView.text = "空气优"
            aqiPillView.setTextColor(Color.parseColor("#3BE2C0"))
        } else if (pmVal <= 75) {
            pm25ValueView.setTextColor(Color.parseColor("#F5A623"))
            aqiPillView.text = "空气良"
            aqiPillView.setTextColor(Color.parseColor("#F5A623"))
        } else {
            pm25ValueView.setTextColor(Color.parseColor("#FF5E62"))
            aqiPillView.text = "空气轻度污染"
            aqiPillView.setTextColor(Color.parseColor("#FF5E62"))
        }
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

