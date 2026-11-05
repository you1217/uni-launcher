package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 1:1 Native Calendar & Schedule Card for UNI Launcher.
 * Features large digital day, lunar date, weekday, and travel condition pill.
 */
class CalendarCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val dayNumView: TextView
    private val dateFullView: TextView
    private val weekdayView: TextView
    private val lunarView: TextView
    private val travelNoteView: TextView

    init {
        orientation = VERTICAL
        val pad = dpToPx(18f)
        setPadding(pad, pad, pad, pad)

        // 1. Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val titleView = TextView(context).apply {
            text = "日历日程"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val limitPill = TextView(context).apply {
            text = "今日不限行"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(limitPill)
        addView(header)

        // 2. Main Calendar Content Row
        val contentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(10f)
            }
        }

        // Big Day Number
        dayNumView = TextView(context).apply {
            text = "05"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, dpToPx(16f), 0)
        }
        contentRow.addView(dayNumView)

        // Date and Lunar details column
        val detailsCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        dateFullView = TextView(context).apply {
            text = "2026年 11月"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        weekdayView = TextView(context).apply {
            text = "星期四"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            setPadding(0, dpToPx(2f), 0, 0)
        }

        lunarView = TextView(context).apply {
            text = "农历十月廿六 · 节气: 立冬"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 12f
            setPadding(0, dpToPx(4f), 0, 0)
        }

        detailsCol.addView(dateFullView)
        detailsCol.addView(weekdayView)
        detailsCol.addView(lunarView)
        contentRow.addView(detailsCol)

        addView(contentRow)

        // 3. Travel Tip Bottom Row
        travelNoteView = TextView(context).apply {
            text = "🚗 行车备忘: 全车胎压与油量充沛，适合舒适驾驶"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            setPadding(0, dpToPx(6f), 0, 0)
        }
        addView(travelNoteView)

        updateDate()
    }

    private fun updateDate() {
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("yyyy年 M月", Locale.getDefault())
        val weekFormat = SimpleDateFormat("EEEE", Locale.CHINESE)

        dayNumView.text = dayFormat.format(cal.time)
        dateFullView.text = monthYearFormat.format(cal.time)
        weekdayView.text = weekFormat.format(cal.time)
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

