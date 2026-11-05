package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Visual reproduction of UNI Launcher's 3D Perspective Vehicle Status Card.
 *
 * Displays:
 * - 3D front-left perspective artwork of Lynk & Co 05
 * - 4 floating tire pressure capsules mapped precisely to wheel positions
 * - Dynamic door / tailgate open alert markers
 * - Header with Range, Fuel, and Climate status
 */
class VehicleCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val rangeView: TextView
    private val carImageView: ImageView
    private val doorAlertView: TextView
    private val climateSummaryView: TextView

    // 4 Tire Pressure Capsules
    private val flTirePill: TextView
    private val frTirePill: TextView
    private val rlTirePill: TextView
    private val rrTirePill: TextView

    init {
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // 1. Header Layout (Top)
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
            }
        }

        titleView = TextView(context).apply {
            text = "领克 05"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        rangeView = TextView(context).apply {
            text = "续航 480 km"
            setTextColor(Color.parseColor("#3BE2C0")) // Nocturne Mint
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        headerLayout.addView(titleView)
        headerLayout.addView(rangeView)
        addView(headerLayout)

        // 2. Central Car Visual Container
        val carContainer = FrameLayout(context).apply {
            val topMargin = dpToPx(32f)
            val bottomMargin = dpToPx(36f)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                setMargins(0, topMargin, 0, bottomMargin)
            }
        }

        carImageView = ImageView(context).apply {
            setImageResource(R.drawable.vehicle_status_front_left)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        }
        carContainer.addView(carImageView)

        // 3. Floating Tire Pressure Pills (Perspective-aligned)
        // In front-left perspective:
        // Right-Front is top-left in screen space
        // Right-Rear is top-right in screen space
        // Left-Front is bottom-left / center in screen space
        // Left-Rear is bottom-right in screen space
        frTirePill = createTirePill("右前 2.3 bar").apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.TOP
                marginStart = dpToPx(8f)
                topMargin = dpToPx(12f)
            }
            layoutParams = lp
        }
        rrTirePill = createTirePill("右后 2.3 bar").apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END or Gravity.TOP
                marginEnd = dpToPx(8f)
                topMargin = dpToPx(8f)
            }
            layoutParams = lp
        }
        flTirePill = createTirePill("左前 2.3 bar").apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.BOTTOM
                marginStart = dpToPx(16f)
                bottomMargin = dpToPx(6f)
            }
            layoutParams = lp
        }
        rlTirePill = createTirePill("左后 2.3 bar").apply {
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END or Gravity.BOTTOM
                marginEnd = dpToPx(12f)
                bottomMargin = dpToPx(14f)
            }
            layoutParams = lp
        }

        carContainer.addView(frTirePill)
        carContainer.addView(rrTirePill)
        carContainer.addView(flTirePill)
        carContainer.addView(rlTirePill)

        // Door alert marker centered in the car container
        doorAlertView = TextView(context).apply {
            text = "主驾车门开启"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_pill_badge_warning)
            setPadding(dpToPx(12f), dpToPx(4f), dpToPx(12f), dpToPx(4f))
            visibility = View.GONE
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }
        carContainer.addView(doorAlertView)

        addView(carContainer)

        // 4. Bottom Climate & Telemetry Summary
        climateSummaryView = TextView(context).apply {
            text = "空调: 22.5°C | AUTO | 风量 2"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            }
            layoutParams = lp
        }
        addView(climateSummaryView)
    }

    private fun createTirePill(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_pill_badge)
            setPadding(dpToPx(10f), dpToPx(4f), dpToPx(10f), dpToPx(4f))
        }
    }

    fun updateTelemetry(telemetry: JSONObject, climate: JSONObject) {
        val fields = telemetry.optJSONObject("fields") ?: JSONObject()

        // 1. Range & Fuel
        val rangeField = fields.optJSONObject("drivingRange")
        val rangeVal = rangeField?.optDouble("value", -1.0) ?: -1.0
        if (rangeVal > 0) {
            rangeView.text = "续航 %.0f km".format(rangeVal)
        }

        // 2. TPMS values
        val fl = fields.optJSONObject("pressureFrontLeft")?.optDouble("value", 2.3) ?: 2.3
        val fr = fields.optJSONObject("pressureFrontRight")?.optDouble("value", 2.3) ?: 2.3
        val rl = fields.optJSONObject("pressureRearLeft")?.optDouble("value", 2.3) ?: 2.3
        val rr = fields.optJSONObject("pressureRearRight")?.optDouble("value", 2.3) ?: 2.3

        applyTireState(flTirePill, "左前", fl)
        applyTireState(frTirePill, "右前", fr)
        applyTireState(rlTirePill, "左后", rl)
        applyTireState(rrTirePill, "右后", rr)

        // 3. Doors & Trunk
        val df = fields.optJSONObject("doorFrontLeft")?.optBoolean("value", false) ?: false
        val dp = fields.optJSONObject("doorFrontRight")?.optBoolean("value", false) ?: false
        val trunk = fields.optJSONObject("tailgateOpen")?.optBoolean("value", false) ?: false

        if (df || dp || trunk) {
            val alerts = mutableListOf<String>()
            if (df) alerts.add("主驾门")
            if (dp) alerts.add("副驾门")
            if (trunk) alerts.add("尾门")
            doorAlertView.text = "${alerts.joinToString("、")}开启"
            doorAlertView.visibility = View.VISIBLE
        } else {
            doorAlertView.visibility = View.GONE
        }

        // 4. Climate
        val leftTemp = climate.optDouble("left", 22.0)
        val fan = climate.optInt("fan", 2)
        val isAuto = climate.optBoolean("auto", false)
        val isPower = climate.optBoolean("power", true)
        if (isPower) {
            climateSummaryView.text = "空调: %.1f°C | %s | 风量 %d".format(
                leftTemp,
                if (isAuto) "AUTO" else "MANUAL",
                fan
            )
        } else {
            climateSummaryView.text = "空调: 已关闭"
        }
    }

    private fun applyTireState(pill: TextView, label: String, bar: Double) {
        pill.text = "%s %.1f bar".format(label, bar)
        if (bar in 2.0..2.7) {
            pill.setBackgroundResource(R.drawable.bg_pill_badge)
            pill.setTextColor(Color.WHITE)
        } else {
            pill.setBackgroundResource(R.drawable.bg_pill_badge_warning)
            pill.setTextColor(Color.parseColor("#FFFF5722"))
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
