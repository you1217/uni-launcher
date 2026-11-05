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

/**
 * Traffic light card displaying real-time Amap navigation traffic light countdowns.
 */
class TrafficLightCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val statusView: TextView
    private val countdownView: TextView
    private val subTextView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)
        gravity = Gravity.CENTER

        titleView = TextView(context).apply {
            text = "高德红绿灯倒计时"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
        }
        addView(titleView)

        countdownView = TextView(context).apply {
            text = "--"
            setTextColor(Color.WHITE)
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4f), 0, dpToPx(4f))
        }
        addView(countdownView)

        statusView = TextView(context).apply {
            text = "高德导航未开启或无路口数据"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 13f
            gravity = Gravity.CENTER
        }
        addView(statusView)

        subTextView = TextView(context).apply {
            text = "点击启动高德地图"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(6f), 0, 0)
        }
        addView(subTextView)

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
                        countdownView.setTextColor(Color.parseColor("#4CAF50"))
                        statusView.text = "绿灯通行中"
                        statusView.setTextColor(Color.parseColor("#81C784"))
                    }
                    "red" -> {
                        countdownView.setTextColor(Color.parseColor("#F44336"))
                        statusView.text = "红灯等待"
                        statusView.setTextColor(Color.parseColor("#E57373"))
                    }
                    else -> {
                        countdownView.setTextColor(Color.parseColor("#FFEB3B"))
                        statusView.text = "黄灯注意"
                        statusView.setTextColor(Color.parseColor("#FFF176"))
                    }
                }
                subTextView.text = "前方路口实时状态"
                return
            }
        }

        countdownView.text = "--"
        countdownView.setTextColor(Color.WHITE)
        statusView.text = "暂无前方红绿灯"
        statusView.setTextColor(Color.parseColor("#80FFFFFF"))
        subTextView.text = "点击启动高德地图"
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

