package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * Card library modal dialog for selecting and adding new cards to the desktop.
 */
class CardLibraryDialog(
    context: Context,
    private val currentPage: Int,
    private val onCardSelected: (type: String) -> Unit
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(720f), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Header Title
        val titleView = TextView(context).apply {
            text = "添加卡片"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTitleView = TextView(context).apply {
            text = "原车卡片使用官方直连数据通道，统一暗夜玻璃质感"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }
        root.addView(titleView)
        root.addView(subTitleView)

        // Card Catalog Grid
        val grid = GridLayout(context).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val catalog = listOf(
            Triple("🚗 车辆状态", "vehicle", "领克05立体车身与四轮胎压"),
            Triple("🚘 车身诊断", "body", "车门车窗开闭与俯视车身"),
            Triple("❄️ 智能空调", "climate", "双温区温控、风量与座椅加热"),
            Triple("🎵 音乐播放", "music", "旋转黑胶唱片与全功能控制"),
            Triple("🚦 高德红绿灯", "trafficLight", "实时路口绿波与变色倒计时"),
            Triple("⏰ 时钟与天气", "clockWeather", "大字时钟与气象空气质量"),
            Triple("📅 日历日程", "calendar", "月份日历、农历节气与行车备忘"),
            Triple("📱 常用应用", "shortcuts", "自选高频车载应用启动网格"),
            Triple("⛽ 燃油续航", "fuel", "剩余油量百分比与续航里程")
        )

        for ((title, type, desc) in catalog) {
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_pill_badge)
                val p = dpToPx(12f)
                setPadding(p, p, p, p)
                isClickable = true
                isFocusable = true

                val name = TextView(context).apply {
                    text = title
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                }
                val sub = TextView(context).apply {
                    text = desc
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    textSize = 11f
                    setPadding(0, dpToPx(4f), 0, 0)
                }
                addView(name)
                addView(sub)

                setOnClickListener {
                    onCardSelected(type)
                    dismiss()
                }
            }

            val param = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(6f), dpToPx(6f), dpToPx(6f), dpToPx(6f))
            }
            grid.addView(item, param)
        }
        root.addView(grid)

        // Close Button
        val closeBtn = TextView(context).apply {
            text = "关闭"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            setPadding(0, dpToPx(10f), 0, dpToPx(10f))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
            }
            layoutParams = lp
            setOnClickListener { dismiss() }
        }
        root.addView(closeBtn)

        setContentView(root)
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

