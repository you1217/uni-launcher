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
 * 1:1 Native Top-Down Body Diagnostics Card for Lynk & Co 05.
 * Uses official top outline line-art (lynk_05_top_line.png) with precise door/hood/trunk status.
 */
class BodyDiagnosticsCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val statusPill: TextView
    private val carContainer: FrameLayout

    private val dfPill: TextView
    private val dpPill: TextView
    private val drlPill: TextView
    private val drrPill: TextView
    private val trunkPill: TextView
    private val hoodPill: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // 1. Header Row
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        titleView = TextView(context).apply {
            text = "车身诊断"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        statusPill = TextView(context).apply {
            text = "全车锁闭"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(statusPill)
        addView(header)

        // 2. Central Car Visual + Peripheral Status Markers
        carContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                topMargin = dpToPx(8f)
            }
        }

        val carImageView = ImageView(context).apply {
            setImageResource(R.drawable.lynk_05_top_line)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        carContainer.addView(carImageView)

        // Door status markers positioned around the silhouette
        // Lynk & Co 05 top trace points right:
        // Left side (driver side) is along the TOP lane
        // Right side (passenger side) is along the BOTTOM lane
        // Tailgate/Trunk is at the far LEFT
        // Engine hood is at the far RIGHT
        trunkPill = createStatusPill("尾门 锁闭").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
        }

        hoodPill = createStatusPill("前舱 锁闭").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
        }

        dfPill = createStatusPill("主驾门 关").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                marginStart = dpToPx(36f)
            }
        }

        drlPill = createStatusPill("左后门 关").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = dpToPx(70f)
            }
        }

        dpPill = createStatusPill("副驾门 关").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                marginStart = dpToPx(36f)
            }
        }

        drrPill = createStatusPill("右后门 关").apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = dpToPx(70f)
            }
        }

        carContainer.addView(hoodPill)
        carContainer.addView(dfPill)
        carContainer.addView(dpPill)
        carContainer.addView(drlPill)
        carContainer.addView(drrPill)
        carContainer.addView(trunkPill)

        addView(carContainer)
    }

    private fun createStatusPill(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(4f)
            setPadding(hp, vp, hp, vp)
        }
    }

    fun updateTelemetry(telemetry: JSONObject) {
        val fields = telemetry.optJSONObject("fields") ?: JSONObject()

        val df = fields.optJSONObject("doorFrontLeft")?.optBoolean("value", false) ?: false
        val dp = fields.optJSONObject("doorFrontRight")?.optBoolean("value", false) ?: false
        val drl = fields.optJSONObject("doorRearLeft")?.optBoolean("value", false) ?: false
        val drr = fields.optJSONObject("doorRearRight")?.optBoolean("value", false) ?: false
        val trunk = fields.optJSONObject("tailgateOpen")?.optBoolean("value", false) ?: false
        val hood = fields.optJSONObject("hoodOpen")?.optBoolean("value", false) ?: false

        updatePillState(dfPill, "主驾门", df)
        updatePillState(dpPill, "副驾门", dp)
        updatePillState(drlPill, "左后门", drl)
        updatePillState(drrPill, "右后门", drr)
        updatePillState(trunkPill, "后备箱", trunk)
        updatePillState(hoodPill, "前舱", hood)

        val anyOpen = df || dp || drl || drr || trunk || hood
        if (anyOpen) {
            statusPill.text = "车门未关"
            statusPill.setTextColor(Color.parseColor("#FFFF5722"))
            statusPill.setBackgroundResource(R.drawable.bg_pill_badge_warning)
        } else {
            statusPill.text = "全车锁闭"
            statusPill.setTextColor(Color.parseColor("#3BE2C0"))
            statusPill.setBackgroundResource(R.drawable.bg_tire_chip)
        }
    }

    private fun updatePillState(pill: TextView, label: String, isOpen: Boolean) {
        if (isOpen) {
            pill.text = "$label 开"
            pill.setTextColor(Color.parseColor("#FFFF5722"))
            pill.setBackgroundResource(R.drawable.bg_pill_badge_warning)
        } else {
            pill.text = "$label 关"
            pill.setTextColor(Color.parseColor("#EFF4F6"))
            pill.setBackgroundResource(R.drawable.bg_tire_chip)
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

