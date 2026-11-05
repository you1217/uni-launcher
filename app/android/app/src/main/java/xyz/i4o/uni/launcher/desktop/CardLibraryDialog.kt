package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Modern Automotive Card Library Dialog.
 * Categorized catalog with size badges, high-contrast dark obsidian tiles, and filter pills.
 */
class CardLibraryDialog(
    context: Context,
    private val currentPage: Int,
    private val onCardSelected: (type: String) -> Unit
) : Dialog(context) {

    private data class CardItemDef(
        val title: String,
        val type: String,
        val desc: String,
        val sizeTag: String,
        val category: String
    )

    private val catalog = listOf(
        CardItemDef("实时地图导航", "nav", "高德地图车机版真画中画嵌入与实时路况", "7×6 黄金半屏", "nav"),
        CardItemDef("车辆状态", "vehicle", "领克立体车身、车速与保养里程", "7×6", "vehicle"),
        CardItemDef("车身诊断", "body", "车门车窗开闭与四轮胎压透视", "7×6", "vehicle"),
        CardItemDef("动力电池", "battery", "PHEV/EV电量、能量流与充电状态", "4×2", "vehicle"),
        CardItemDef("燃油续航", "fuel", "剩余油量百分比与燃油续航", "4×2", "vehicle"),
        CardItemDef("智能空调", "climate", "双温区温控、风量与座椅加热", "4×3", "cabin"),
        CardItemDef("音乐播放", "music", "旋转黑胶唱片与全功能控制", "4×3", "cabin"),
        CardItemDef("车内空气", "air", "PM2.5、AQS阻隔与CleanZone净化", "4×2", "cabin"),
        CardItemDef("高德红绿灯", "trafficLight", "实时路口绿波与变色倒计时", "4×2", "nav"),
        CardItemDef("时钟与天气", "clockWeather", "大字时钟与气象空气质量组合", "4×2", "daily"),
        CardItemDef("日历日程", "calendar", "月份日历、农历节气与行车备忘", "4×2", "daily"),
        CardItemDef("常用应用", "shortcuts", "自选高频车载应用启动网格", "4×3", "daily"),
        CardItemDef("系统清理", "cleanup", "运存实时占用与后台缓存释放", "4×2", "daily"),
        CardItemDef("极简时钟", "clock", "极简大字时钟、动态秒跳与农历", "4×2", "daily"),
        CardItemDef("独立天气", "weather", "城市定位、今日气象与多日预报", "4×2", "daily"),
        CardItemDef("领克账号", "member", "Co-Club尊享会员与车主权益中心", "4×2", "daily")
    )

    private val filterButtons = mutableListOf<TextView>()
    private var activeCategory = "all"
    private lateinit var gridContainer: GridLayout

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(880f), dpToPx(620f))
        }

        // 1. Header (Title, Subtitle, Close Button)
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
            text = "添加卡片"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTitleView = TextView(context).apply {
            text = "领克官方数据通道 · 硬件加速渲染"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            setPadding(0, dpToPx(4f), 0, 0)
        }
        titleCol.addView(titleView)
        titleCol.addView(subTitleView)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val s = dpToPx(36f)
            layoutParams = LinearLayout.LayoutParams(s, s)
            isClickable = true
            isFocusable = true
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Category Filter Pills Row
        val filterScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
                bottomMargin = dpToPx(12f)
            }
        }

        val filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            Pair("all", "全部 (${catalog.size})"),
            Pair("nav", "行车与导航"),
            Pair("vehicle", "车身与能耗"),
            Pair("cabin", "智能座舱"),
            Pair("daily", "日常与效率")
        )

        for ((catId, label) in categories) {
            val pill = TextView(context).apply {
                text = label
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                val isSelected = catId == activeCategory
                setTextColor(if (isSelected) Color.parseColor("#3BE2C0") else Color.parseColor("#8A99AD"))
                setBackgroundResource(if (isSelected) R.drawable.bg_dock_pill_accent else R.drawable.bg_dock_item_glass)
                val hp = dpToPx(14f)
                val vp = dpToPx(8f)
                setPadding(hp, vp, hp, vp)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dpToPx(8f)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    activeCategory = catId
                    updateFilterStyles(catId)
                    populateGrid()
                }
            }
            filterButtons.add(pill)
            filterRow.addView(pill)
        }
        filterScroll.addView(filterRow)
        root.addView(filterScroll)

        // 3. Card Catalog Scroll & Grid
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isVerticalScrollBarEnabled = false
        }

        gridContainer = GridLayout(context).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        populateGrid()
        scroll.addView(gridContainer)
        root.addView(scroll)

        setContentView(root)
    }

    private fun updateFilterStyles(selectedId: String) {
        val categories = listOf("all", "nav", "vehicle", "cabin", "daily")
        for (i in categories.indices) {
            val pill = filterButtons[i]
            val isSelected = categories[i] == selectedId
            pill.setTextColor(if (isSelected) Color.parseColor("#3BE2C0") else Color.parseColor("#8A99AD"))
            pill.setBackgroundResource(if (isSelected) R.drawable.bg_dock_pill_accent else R.drawable.bg_dock_item_glass)
        }
    }

    private fun populateGrid() {
        gridContainer.removeAllViews()
        val filtered = if (activeCategory == "all") catalog else catalog.filter { it.category == activeCategory }

        for (item in filtered) {
            val tile = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_dock_item_glass)
                val p = dpToPx(12f)
                setPadding(p, p, p, p)
                isClickable = true
                isFocusable = true

                // Top row: Title + Size badge
                val topRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val name = TextView(context).apply {
                    text = item.title
                    setTextColor(Color.parseColor("#EFF4F6"))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                topRow.addView(name)

                val sizeBadge = TextView(context).apply {
                    text = item.sizeTag
                    setTextColor(Color.parseColor("#3BE2C0"))
                    textSize = 10f
                    typeface = Typeface.DEFAULT_BOLD
                    setBackgroundResource(R.drawable.bg_tire_chip)
                    setPadding(dpToPx(6f), dpToPx(2f), dpToPx(6f), dpToPx(2f))
                }
                topRow.addView(sizeBadge)
                addView(topRow)

                val sub = TextView(context).apply {
                    text = item.desc
                    setTextColor(Color.parseColor("#8A99AD"))
                    textSize = 11f
                    setPadding(0, dpToPx(6f), 0, 0)
                }
                addView(sub)

                setOnClickListener {
                    onCardSelected(item.type)
                    dismiss()
                }
            }

            val param = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(6f), dpToPx(6f), dpToPx(6f), dpToPx(6f))
            }
            gridContainer.addView(tile, param)
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
