package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.R

/**
 * Native Battery & Energy Card for UNI Launcher (PHEV / EV models).
 * Displays State of Charge (SoC), Electric Range, Energy Flow, and Drive Mode.
 */
class BatteryPowerCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val driveModeBadge: TextView
    private val batteryPercentView: TextView
    private val electricRangeView: TextView
    private val batteryBar: ProgressBar
    private val energyFlowDescView: TextView
    private val chargeStatusView: TextView

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
            text = "动力电池"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        driveModeBadge = TextView(context).apply {
            text = "智能混动"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(driveModeBadge)
        addView(header)

        // 2. Center Battery Percentage Row
        val contentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(6f)
            }
        }

        val socCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val numRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        batteryPercentView = TextView(context).apply {
            text = "85"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

        val unitView = TextView(context).apply {
            text = "%"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 16f
            setPadding(dpToPx(2f), 0, 0, dpToPx(6f))
        }

        numRow.addView(batteryPercentView)
        numRow.addView(unitView)
        socCol.addView(numRow)

        val socSub = TextView(context).apply {
            text = "高压动力电池电量"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
        }
        socCol.addView(socSub)
        contentRow.addView(socCol)

        // Right: Electric Range
        val rangeCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val rangeLabel = TextView(context).apply {
            text = "纯电续航"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            gravity = Gravity.END
        }

        electricRangeView = TextView(context).apply {
            text = "88 km"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.END
            setPadding(0, dpToPx(2f), 0, 0)
        }

        rangeCol.addView(rangeLabel)
        rangeCol.addView(electricRangeView)
        contentRow.addView(rangeCol)

        addView(contentRow)

        // 3. Battery Bar
        batteryBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 85
            progressDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.progress_bar_mint)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(6f)).apply {
                topMargin = dpToPx(4f)
                bottomMargin = dpToPx(8f)
            }
        }
        addView(batteryBar)

        // 4. Bottom Footer: Energy flow & Charge port status
        val footerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        energyFlowDescView = TextView(context).apply {
            text = "驱动电机输出中"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        chargeStatusView = TextView(context).apply {
            text = "充电口已锁止"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
        }

        footerRow.addView(energyFlowDescView)
        footerRow.addView(chargeStatusView)
        addView(footerRow)
    }

    fun updateTelemetry(vehicle: JSONObject) {
        val pct = vehicle.optInt("batteryPercent", 85)
        batteryPercentView.text = "$pct"
        batteryBar.progress = pct
        if (pct <= 20) {
            batteryPercentView.setTextColor(Color.parseColor("#FF5E62"))
            batteryBar.progressDrawable?.setTint(Color.parseColor("#FF5E62"))
        } else {
            batteryPercentView.setTextColor(Color.parseColor("#3BE2C0"))
            batteryBar.progressDrawable?.setTint(Color.parseColor("#3BE2C0"))
        }
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

