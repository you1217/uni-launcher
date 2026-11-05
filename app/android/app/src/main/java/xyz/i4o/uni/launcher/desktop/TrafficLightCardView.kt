package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Visual reproduction of UNI Launcher's Amap Traffic Light Card.
 */
class TrafficLightCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleBadge: TextView
    private val countdownView: TextView
    private val laneStatusPill: TextView
    private val hintView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)
        gravity = Gravity.CENTER

        // Header: Badge
        titleBadge = TextView(context).apply {
            text = "高德红绿灯"
            setTextColor(Color.parseColor("#3BE2C0")) // Mint
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }
        addView(titleBadge)

        // Large countdown numbers
        countdownView = TextView(context).apply {
            text = "--"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(2f), 0, dpToPx(2f))
        }
        addView(countdownView)

        // Status pill badge
        laneStatusPill = TextView(context).apply {
            text = "等待路口数据"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            setPadding(dpToPx(12f), dpToPx(4f), dpToPx(12f), dpToPx(4f))
            gravity = Gravity.CENTER
        }
        addView(laneStatusPill)

        // Bottom hint
        hintView = TextView(context).apply {
            text = "点击启动高德地图"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(6f), 0, 0)
        }
        addView(hintView)

        setOnClickListener {
            try {
                LauncherRuntime.get(context).handle(JSONObject().put("action", "launch-navigation"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTrafficLights(trafficLights: JSONObject) {
        val available = trafficLights.optBoolean("available", false)
        val lights = trafficLights.optJSONArray("lights") ?: JSONArray()

        if (available && lights.length() > 0) {
            val first = lights.optJSONObject(0)
            val sec = first?.optInt("seconds", -1) ?: -1
            val status = first?.optString("status", "") // green, red, yellow

            if (sec >= 0) {
                countdownView.text = "${sec}s"
                when (status?.lowercase()) {
                    "green" -> {
                        countdownView.setTextColor(Color.parseColor("#3BE2C0")) // Mint
                        laneStatusPill.text = "🟢 前方路口通行"
                        laneStatusPill.setTextColor(Color.parseColor("#3BE2C0"))
                    }
                    "red" -> {
                        countdownView.setTextColor(Color.parseColor("#FF5252")) // Red
                        laneStatusPill.text = "🔴 红灯等待中"
                        laneStatusPill.setTextColor(Color.parseColor("#FF8A80"))
                    }
                    else -> {
                        countdownView.setTextColor(Color.parseColor("#FFD54F")) // Yellow
                        laneStatusPill.text = "🟡 黄灯注意缓行"
                        laneStatusPill.setTextColor(Color.parseColor("#FFE082"))
                    }
                }
                hintView.text = "高德车机版实时绿波"
                return
            }
        }

        countdownView.text = "--"
        countdownView.setTextColor(Color.WHITE)
        laneStatusPill.text = "等待路口数据"
        laneStatusPill.setTextColor(Color.parseColor("#B3FFFFFF"))
        hintView.text = "点击启动高德地图"
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
