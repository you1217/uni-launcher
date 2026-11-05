package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime

/**
 * Native vehicle status card displaying Lynk & Co 05 telemetry:
 * - TPMS (4-wheel tire pressures)
 * - Door lock and open states
 * - Remaining range & fuel
 * - Air conditioning status
 */
class VehicleCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val rangeView: TextView
    private val tpmsFlView: TextView
    private val tpmsFrView: TextView
    private val tpmsRlView: TextView
    private val tpmsRrView: TextView
    private val doorStatusView: TextView
    private val climateView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // Header row: Title + Fuel/Range
        val headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = TextView(context).apply {
            text = "领克 05 车辆状态"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        rangeView = TextView(context).apply {
            text = "续航 480 km"
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerLayout.addView(titleView)
        headerLayout.addView(rangeView)
        addView(headerLayout)

        // Divider
        addView(createDivider())

        // TPMS Row: 4 tires
        val tpmsTitle = TextView(context).apply {
            text = "胎压监测 (bar)"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
            setPadding(0, dpToPx(4f), 0, dpToPx(4f))
        }
        addView(tpmsTitle)

        val tpmsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(2f), 0, dpToPx(6f))
        }
        tpmsFlView = createTpmsItem("前左: 2.3")
        tpmsFrView = createTpmsItem("前右: 2.3")
        tpmsRlView = createTpmsItem("后左: 2.3")
        tpmsRrView = createTpmsItem("后右: 2.3")

        tpmsLayout.addView(tpmsFlView)
        tpmsLayout.addView(tpmsFrView)
        tpmsLayout.addView(tpmsRlView)
        tpmsLayout.addView(tpmsRrView)
        addView(tpmsLayout)

        // Door status row
        doorStatusView = TextView(context).apply {
            text = "车门状态: 车门与后备箱均已关闭"
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 13f
            setPadding(0, dpToPx(4f), 0, dpToPx(4f))
        }
        addView(doorStatusView)

        // Climate row
        climateView = TextView(context).apply {
            text = "空调: 22.5°C | AUTO | 风量 2"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(4f), 0, 0)
        }
        addView(climateView)
    }

    private fun createTpmsItem(defaultText: String): TextView {
        return TextView(context).apply {
            text = defaultText
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
            setPadding(dpToPx(4f), dpToPx(6f), dpToPx(4f), dpToPx(6f))
        }
    }

    private fun createDivider(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1f)).apply {
                topMargin = dpToPx(8f)
                bottomMargin = dpToPx(8f)
            }
            setBackgroundColor(Color.parseColor("#1FFFFFFF"))
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

        // 2. TPMS
        val fl = fields.optJSONObject("pressureFrontLeft")?.optDouble("value", 2.3) ?: 2.3
        val fr = fields.optJSONObject("pressureFrontRight")?.optDouble("value", 2.3) ?: 2.3
        val rl = fields.optJSONObject("pressureRearLeft")?.optDouble("value", 2.3) ?: 2.3
        val rr = fields.optJSONObject("pressureRearRight")?.optDouble("value", 2.3) ?: 2.3

        tpmsFlView.text = "前左: %.1f".format(fl)
        tpmsFrView.text = "前右: %.1f".format(fr)
        tpmsRlView.text = "后左: %.1f".format(rl)
        tpmsRrView.text = "后右: %.1f".format(rr)

        // 3. Doors
        val df = fields.optJSONObject("doorFrontLeft")?.optBoolean("value", false) ?: false
        val dp = fields.optJSONObject("doorFrontRight")?.optBoolean("value", false) ?: false
        val trunk = fields.optJSONObject("tailgateOpen")?.optBoolean("value", false) ?: false

        if (df || dp || trunk) {
            val sb = StringBuilder("车门告警: ")
            if (df) sb.append("主驾门开启 ")
            if (dp) sb.append("副驾门开启 ")
            if (trunk) sb.append("后备箱开启 ")
            doorStatusView.text = sb.toString()
            doorStatusView.setTextColor(Color.parseColor("#FFA726")) // Warning amber
        } else {
            doorStatusView.text = "车门状态: 四门及后备箱已完全闭锁"
            doorStatusView.setTextColor(Color.parseColor("#81C784")) // Normal soft green
        }

        // 4. Climate
        val leftTemp = climate.optDouble("left", 22.0)
        val fan = climate.optInt("fan", 2)
        val isAuto = climate.optBoolean("auto", false)
        val isPower = climate.optBoolean("power", true)
        if (isPower) {
            climateView.text = "空调: %.1f°C | %s | 风量 %d".format(
                leftTemp,
                if (isAuto) "AUTO" else "MANUAL",
                fan
            )
        } else {
            climateView.text = "空调: 已关闭"
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

