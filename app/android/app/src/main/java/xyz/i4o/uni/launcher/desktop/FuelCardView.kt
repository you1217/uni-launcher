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
 * 1:1 Reproduction of UNI Launcher's Fuel & Range Card.
 */
class FuelCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val percentView: TextView
    private val rangeView: TextView
    private val progressBar: ProgressBar
    private val subTextView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)
        gravity = Gravity.CENTER_VERTICAL

        // Top Row: Title + %
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = TextView(context).apply {
            text = "燃油与续航"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        percentView = TextView(context).apply {
            text = "85%"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        topRow.addView(titleView)
        topRow.addView(percentView)
        addView(topRow)

        // Middle: Large Range Number
        rangeView = TextView(context).apply {
            text = "520 km"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(6f), 0, dpToPx(4f))
        }
        addView(rangeView)

        // Progress bar
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(6f)).apply {
                bottomMargin = dpToPx(8f)
            }
            max = 100
            progress = 85
            progressDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.progress_bar_mint)
        }
        addView(progressBar)

        // Bottom: Status subtext
        subTextView = TextView(context).apply {
            text = "平均油耗 7.2 L/100km | 油量充沛"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
        }
        addView(subTextView)
    }

    fun updateTelemetry(telemetry: JSONObject) {
        val fields = telemetry.optJSONObject("fields") ?: JSONObject()
        val rangeField = fields.optJSONObject("drivingRange")
        val rangeVal = rangeField?.optDouble("value", -1.0) ?: -1.0
        if (rangeVal > 0) {
            rangeView.text = "%.0f km".format(rangeVal)
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

