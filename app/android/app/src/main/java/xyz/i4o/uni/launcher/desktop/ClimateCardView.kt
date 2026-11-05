package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Reproduction of UNI Launcher's Air Conditioning & Seat Climate Card.
 */
class ClimateCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val tempDisplay: TextView
    private val acToggleBtn: TextView
    private val autoToggleBtn: TextView
    private val recircBtn: TextView
    private val fanDisplay: TextView
    private val seatHeatBtn: TextView

    private var currentTemp = 22.0
    private var currentFan = 2
    private var isAcOn = true
    private var isAutoOn = true
    private var isRecirc = true
    private var seatHeatLevel = 0

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // Header: Title + Power status
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = TextView(context).apply {
            text = "智能空调"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                ClimateControlBottomSheet(context).show()
            }
        }
        header.addView(titleView)

        val detailBtn = TextView(context).apply {
            text = "座椅/更多 ⚙️"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dpToPx(6f)
            }
            setOnClickListener {
                ClimateControlBottomSheet(context).show()
            }
        }
        header.addView(detailBtn)

        val powerBtn = TextView(context).apply {
            text = "电源"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setPadding(dpToPx(10f), dpToPx(4f), dpToPx(10f), dpToPx(4f))
            setOnClickListener { sendClimateAction("climate-power", "") }
        }
        header.addView(powerBtn)
        addView(header)

        // Row 1: Temperature adjustment (+ / -)
        val tempRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8f), 0, dpToPx(8f))
        }

        val minusBtn = createCircleButton("－") {
            currentTemp = (currentTemp - 0.5).coerceAtLeast(16.0)
            updateUi()
            sendClimateAction("climate-temp", currentTemp.toString())
        }
        tempDisplay = TextView(context).apply {
            text = "22.5°C"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val plusBtn = createCircleButton("＋") {
            currentTemp = (currentTemp + 0.5).coerceAtMost(31.0)
            updateUi()
            sendClimateAction("climate-temp", currentTemp.toString())
        }

        tempRow.addView(minusBtn)
        tempRow.addView(tempDisplay)
        tempRow.addView(plusBtn)
        addView(tempRow)

        // Row 2: Toggles (AUTO, A/C, 内循环, 主驾加热)
        val toggleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }

        autoToggleBtn = createTogglePill("AUTO") {
            isAutoOn = !isAutoOn
            updateUi()
            sendClimateAction("climate-auto", if (isAutoOn) "1" else "0")
        }
        acToggleBtn = createTogglePill("A/C") {
            isAcOn = !isAcOn
            updateUi()
            sendClimateAction("climate-ac", if (isAcOn) "1" else "0")
        }
        recircBtn = createTogglePill("内循环") {
            isRecirc = !isRecirc
            updateUi()
            sendClimateAction("climate-circulation", if (isRecirc) "1" else "0")
        }
        seatHeatBtn = createTogglePill("座椅加热") {
            seatHeatLevel = (seatHeatLevel + 1) % 4
            updateUi()
            sendClimateAction("climate-heat", seatHeatLevel.toString())
        }

        toggleRow.addView(autoToggleBtn)
        toggleRow.addView(acToggleBtn)
        toggleRow.addView(recircBtn)
        toggleRow.addView(seatHeatBtn)
        addView(toggleRow)

        // Bottom row: Fan speed control
        val fanRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8f), 0, 0)
        }
        val fanDownBtn = createCircleButton("◁") {
            currentFan = (currentFan - 1).coerceAtLeast(1)
            updateUi()
            sendClimateAction("climate-fan", currentFan.toString())
        }
        fanDisplay = TextView(context).apply {
            text = "风量: 2 挡"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val fanUpBtn = createCircleButton("▷") {
            currentFan = (currentFan + 1).coerceAtMost(7)
            updateUi()
            sendClimateAction("climate-fan", currentFan.toString())
        }
        fanRow.addView(fanDownBtn)
        fanRow.addView(fanDisplay)
        fanRow.addView(fanUpBtn)
        addView(fanRow)

        updateUi()
    }

    private fun createCircleButton(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 16f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val btnSize = dpToPx(38f)
            layoutParams = LayoutParams(btnSize, btnSize)
            setOnClickListener { onClick() }
        }
    }

    private fun createTogglePill(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(3f)
                marginEnd = dpToPx(3f)
            }
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun sendClimateAction(action: String, value: String) {
        try {
            val cmd = JSONObject().put("action", action).put("value", value)
            LauncherRuntime.get(context).handle(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUi() {
        tempDisplay.text = "%.1f°C".format(currentTemp)
        fanDisplay.text = "风量: %d 挡".format(currentFan)
        if (isAcOn) {
            acToggleBtn.setTextColor(Color.parseColor("#3BE2C0"))
            acToggleBtn.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            acToggleBtn.setTextColor(Color.parseColor("#8A99AD"))
            acToggleBtn.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }
        if (isAutoOn) {
            autoToggleBtn.setTextColor(Color.parseColor("#3BE2C0"))
            autoToggleBtn.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            autoToggleBtn.setTextColor(Color.parseColor("#8A99AD"))
            autoToggleBtn.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }
        recircBtn.text = if (isRecirc) "内循环" else "外循环"
        if (isRecirc) {
            recircBtn.setTextColor(Color.parseColor("#3BE2C0"))
            recircBtn.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            recircBtn.setTextColor(Color.parseColor("#8A99AD"))
            recircBtn.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }
        seatHeatBtn.text = if (seatHeatLevel > 0) "加热 ${seatHeatLevel}挡" else "加热 关"
        if (seatHeatLevel > 0) {
            seatHeatBtn.setTextColor(Color.parseColor("#FFB057"))
            seatHeatBtn.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            seatHeatBtn.setTextColor(Color.parseColor("#8A99AD"))
            seatHeatBtn.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }
    }

    fun updateFromTelemetry(climate: JSONObject) {
        val left = climate.optDouble("left", currentTemp)
        val fan = climate.optInt("fan", currentFan)
        val ac = climate.optBoolean("ac", isAcOn)
        val auto = climate.optBoolean("auto", isAutoOn)
        val recirc = climate.optBoolean("recirc", isRecirc)
        val heat = climate.optInt("driverHeat", seatHeatLevel)

        currentTemp = left
        currentFan = fan
        isAcOn = ac
        isAutoOn = auto
        isRecirc = recirc
        seatHeatLevel = heat
        updateUi()
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

