package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Nocturne-styled Deep Climate & Seat Control Sheet for Lynk & Co 05.
 * Features dual-zone temperature adjustment, fan speed, A/C, SYNC, recirculation,
 * and 3-stage Driver/Passenger seat heating and ventilation.
 */
class ClimateControlBottomSheet(context: Context) : Dialog(context) {

    private val runtime = LauncherRuntime.get(context)

    private var leftTemp = 22.0
    private var rightTemp = 22.0
    private var fanSpeed = 2
    private var isAuto = false
    private var isAc = true
    private var isRecirc = true
    private var isSync = true
    private var isPower = true

    private var driverHeat = 0
    private var driverVent = 0
    private var passengerHeat = 0

    private val leftTempView: TextView
    private val rightTempView: TextView
    private val fanSpeedView: TextView

    private val autoBtn: TextView
    private val acBtn: TextView
    private val recircBtn: TextView
    private val syncBtn: TextView
    private val powerBtn: TextView

    private val driverHeatPills = mutableListOf<TextView>()
    private val driverVentPills = mutableListOf<TextView>()
    private val passengerHeatPills = mutableListOf<TextView>()

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(880f), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // 1. Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            text = "空调与座椅控制"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTitleView = TextView(context).apply {
            text = "领克 05 官方双温区温控 · 主副驾座椅加热与通风"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            setPadding(0, dpToPx(3f), 0, 0)
        }
        titleCol.addView(titleView)
        titleCol.addView(subTitleView)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            layoutParams = LinearLayout.LayoutParams(dpToPx(38f), dpToPx(38f))
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Dual Zone Temp & Fan Speed Cards
        val tempFanRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
            }
        }

        // Left Temp (Driver)
        val leftCard = createZoneCard("主驾设定温度", leftTemp.toString() + "°C",
            onMinus = {
                leftTemp = (leftTemp - 0.5).coerceAtLeast(16.0)
                updateDisplays()
                sendCommand("temp-down", "left")
            },
            onPlus = {
                leftTemp = (leftTemp + 0.5).coerceAtMost(30.0)
                updateDisplays()
                sendCommand("temp-up", "left")
            }
        )
        leftTempView = leftCard.second
        tempFanRow.addView(leftCard.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dpToPx(8f)
        })

        // Fan Speed
        val fanCard = createZoneCard("出风量", "风量 $fanSpeed 挡",
            onMinus = {
                fanSpeed = (fanSpeed - 1).coerceAtLeast(1)
                updateDisplays()
                sendCommand("fan-down")
            },
            onPlus = {
                fanSpeed = (fanSpeed + 1).coerceAtMost(7)
                updateDisplays()
                sendCommand("fan-up")
            }
        )
        fanSpeedView = fanCard.second
        tempFanRow.addView(fanCard.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(4f)
            marginEnd = dpToPx(4f)
        })

        // Right Temp (Passenger)
        val rightCard = createZoneCard("副驾设定温度", rightTemp.toString() + "°C",
            onMinus = {
                rightTemp = (rightTemp - 0.5).coerceAtLeast(16.0)
                updateDisplays()
                sendCommand("temp-down", "right")
            },
            onPlus = {
                rightTemp = (rightTemp + 0.5).coerceAtMost(30.0)
                updateDisplays()
                sendCommand("temp-up", "right")
            }
        )
        rightTempView = rightCard.second
        tempFanRow.addView(rightCard.first, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(8f)
        })
        root.addView(tempFanRow)

        // 3. Modes Matrix: AUTO | AC | 内循环 | SYNC | 电源
        val modesRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
            }
        }

        autoBtn = createModePill("AUTO") {
            isAuto = !isAuto
            updatePillState(autoBtn, isAuto)
            sendCommand("climate-mode", value = "auto")
        }
        acBtn = createModePill("A/C 制冷") {
            isAc = !isAc
            updatePillState(acBtn, isAc)
            sendCommand("climate-mode", value = "ac")
        }
        recircBtn = createModePill("内循环") {
            isRecirc = !isRecirc
            recircBtn.text = if (isRecirc) "内循环" else "外循环"
            updatePillState(recircBtn, isRecirc)
            sendCommand("climate-mode", value = "recirc")
        }
        syncBtn = createModePill("SYNC 同步") {
            isSync = !isSync
            updatePillState(syncBtn, isSync)
            sendCommand("climate-mode", value = "sync")
        }
        powerBtn = createModePill("电源 开关") {
            isPower = !isPower
            updatePillState(powerBtn, isPower)
            sendCommand("climate-power")
        }

        modesRow.addView(autoBtn, LinearLayout.LayoutParams(0, dpToPx(44f), 1f).apply { marginEnd = dpToPx(6f) })
        modesRow.addView(acBtn, LinearLayout.LayoutParams(0, dpToPx(44f), 1f).apply { marginEnd = dpToPx(6f) })
        modesRow.addView(recircBtn, LinearLayout.LayoutParams(0, dpToPx(44f), 1f).apply { marginEnd = dpToPx(6f) })
        modesRow.addView(syncBtn, LinearLayout.LayoutParams(0, dpToPx(44f), 1f).apply { marginEnd = dpToPx(6f) })
        modesRow.addView(powerBtn, LinearLayout.LayoutParams(0, dpToPx(44f), 1f))
        root.addView(modesRow)

        // 4. Seats Section: Driver (Heat 1-3, Vent 1-3) & Passenger (Heat 1-3)
        val seatTitle = TextView(context).apply {
            text = "💺 领克 05 原车座椅舒适度控制"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(8f))
        }
        root.addView(seatTitle)

        val seatContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Driver Seat Card
        val driverSeatCard = createSeatCard(
            title = "主驾座椅 (加热 & 通风)",
            heatPills = driverHeatPills,
            onHeatSelected = { level ->
                driverHeat = level
                updateSeatPills(driverHeatPills, driverHeat, Color.parseColor("#FFB25C"))
                sendCommand("seat-heat", "left", level.toString())
            },
            ventPills = driverVentPills,
            onVentSelected = { level ->
                driverVent = level
                updateSeatPills(driverVentPills, driverVent, Color.parseColor("#64C8FF"))
                sendCommand("seat-vent", "left", level.toString())
            }
        )
        seatContainer.addView(driverSeatCard, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dpToPx(8f)
        })

        // Passenger Seat Card
        val passengerSeatCard = createSeatCard(
            title = "副驾座椅 (加热)",
            heatPills = passengerHeatPills,
            onHeatSelected = { level ->
                passengerHeat = level
                updateSeatPills(passengerHeatPills, passengerHeat, Color.parseColor("#FFB25C"))
                sendCommand("seat-heat", "right", level.toString())
            },
            ventPills = null,
            onVentSelected = null
        )
        seatContainer.addView(passengerSeatCard, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(8f)
        })
        root.addView(seatContainer)

        setContentView(root)
        updateDisplays()
        readInitialState()
    }

    private fun readInitialState() {
        val snapshot = runtime.snapshot(LauncherRuntime.StaticCursor())
        val climate = snapshot.optJSONObject("climate") ?: return
        leftTemp = climate.optDouble("left", 22.0)
        rightTemp = climate.optDouble("right", 22.0)
        fanSpeed = climate.optInt("fan", 2)
        isAuto = climate.optBoolean("auto", false)
        isAc = climate.optBoolean("ac", true)
        isRecirc = climate.optBoolean("recirc", true)
        isPower = climate.optBoolean("power", true)
        updateDisplays()
    }

    private fun updateDisplays() {
        leftTempView.text = "%.1f°C".format(leftTemp)
        rightTempView.text = "%.1f°C".format(rightTemp)
        fanSpeedView.text = "风量 $fanSpeed 挡"
        updatePillState(autoBtn, isAuto)
        updatePillState(acBtn, isAc)
        updatePillState(recircBtn, isRecirc)
        updatePillState(syncBtn, isSync)
        updatePillState(powerBtn, isPower)
        updateSeatPills(driverHeatPills, driverHeat, Color.parseColor("#FFB25C"))
        updateSeatPills(driverVentPills, driverVent, Color.parseColor("#64C8FF"))
        updateSeatPills(passengerHeatPills, passengerHeat, Color.parseColor("#FFB25C"))
    }

    private fun createZoneCard(
        label: String,
        initialValue: String,
        onMinus: () -> Unit,
        onPlus: () -> Unit
    ): Pair<LinearLayout, TextView> {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
        }

        val headerText = TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
        }
        card.addView(headerText)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8f)
            }
        }

        val minusBtn = TextView(context).apply {
            text = "－"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val size = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(size, size)
            isClickable = true
            isFocusable = true
            setOnClickListener { onMinus() }
        }

        val valueView = TextView(context).apply {
            text = initialValue
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val plusBtn = TextView(context).apply {
            text = "＋"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val size = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(size, size)
            isClickable = true
            isFocusable = true
            setOnClickListener { onPlus() }
        }

        row.addView(minusBtn)
        row.addView(valueView)
        row.addView(plusBtn)
        card.addView(row)

        return Pair(card, valueView)
    }

    private fun createModePill(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun updatePillState(pill: TextView, active: Boolean) {
        if (active) {
            pill.setTextColor(Color.parseColor("#3BE2C0"))
            pill.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            pill.setTextColor(Color.parseColor("#8A99AD"))
            pill.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }
    }

    private fun createSeatCard(
        title: String,
        heatPills: MutableList<TextView>,
        onHeatSelected: (Int) -> Unit,
        ventPills: MutableList<TextView>?,
        onVentSelected: ((Int) -> Unit)?
    ): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
        }

        val titleView = TextView(context).apply {
            text = title
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(8f))
        }
        card.addView(titleView)

        // Heat Row: 关 | 1档 | 2档 | 3档
        val heatLabel = TextView(context).apply {
            text = "🔥 加热"
            setTextColor(Color.parseColor("#FFB25C"))
            textSize = 11f
        }
        card.addView(heatLabel)

        val heatRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(34f)
            ).apply {
                topMargin = dpToPx(4f)
            }
        }
        for (lvl in 0..3) {
            val pill = TextView(context).apply {
                text = if (lvl == 0) "关" else "${lvl}档"
                textSize = 11f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_dock_item_glass)
                isClickable = true
                isFocusable = true
                setOnClickListener { onHeatSelected(lvl) }
            }
            heatPills.add(pill)
            heatRow.addView(pill, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                if (lvl > 0) marginStart = dpToPx(4f)
            })
        }
        card.addView(heatRow)

        // Vent Row (if supported)
        if (ventPills != null && onVentSelected != null) {
            val ventLabel = TextView(context).apply {
                text = "❄️ 通风"
                setTextColor(Color.parseColor("#64C8FF"))
                textSize = 11f
                setPadding(0, dpToPx(8f), 0, 0)
            }
            card.addView(ventLabel)

            val ventRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(34f)
                ).apply {
                    topMargin = dpToPx(4f)
                }
            }
            for (lvl in 0..3) {
                val pill = TextView(context).apply {
                    text = if (lvl == 0) "关" else "${lvl}档"
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_dock_item_glass)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onVentSelected(lvl) }
                }
                ventPills.add(pill)
                ventRow.addView(pill, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (lvl > 0) marginStart = dpToPx(4f)
                })
            }
            card.addView(ventRow)
        }

        return card
    }

    private fun updateSeatPills(pills: List<TextView>, activeLevel: Int, highlightColor: Int) {
        for ((idx, pill) in pills.withIndex()) {
            if (idx == activeLevel) {
                pill.setTextColor(highlightColor)
                pill.setBackgroundResource(R.drawable.bg_dock_pill_accent)
                pill.typeface = Typeface.DEFAULT_BOLD
            } else {
                pill.setTextColor(Color.parseColor("#8A99AD"))
                pill.setBackgroundResource(R.drawable.bg_dock_item_glass)
                pill.typeface = Typeface.DEFAULT
            }
        }
    }

    private fun sendCommand(action: String, zone: String = "", value: String = "") {
        try {
            val cmd = JSONObject().apply {
                put("action", action)
                if (zone.isNotEmpty()) put("zone", zone)
                if (value.isNotEmpty()) put("value", value)
            }
            runtime.handle(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

