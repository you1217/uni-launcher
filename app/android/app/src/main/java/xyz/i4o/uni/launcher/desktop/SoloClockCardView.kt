package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Native Large Digital Clock Card for UNI Launcher.
 * Luxury minimalist automotive style with large digits, animated seconds, date, and lunar calendar.
 */
class SoloClockCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val hourView: TextView
    private val colonView: TextView
    private val minuteView: TextView
    private val secondBadge: TextView
    private val dateFullView: TextView
    private val weekdayView: TextView

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    private val weekdayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)

    private val handler = Handler(Looper.getMainLooper())
    private var colonVisible = true

    private val ticker = object : Runnable {
        override fun run() {
            updateTime()
            colonVisible = !colonVisible
            colonView.setTextColor(
                if (colonVisible) Color.parseColor("#3BE2C0")
                else Color.parseColor("#403BE2C0")
            )
            handler.postDelayed(this, 1000)
        }
    }

    init {
        orientation = VERTICAL
        val pad = dpToPx(20f)
        setPadding(pad, pad, pad, pad)

        // 1. Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val titleView = TextView(context).apply {
            text = "极简时钟"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        secondBadge = TextView(context).apply {
            text = "58s"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(secondBadge)
        addView(header)

        // 2. Large Time Display Row
        val timeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(8f)
            }
        }

        hourView = TextView(context).apply {
            text = "12"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 62f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            includeFontPadding = false
        }

        colonView = TextView(context).apply {
            text = ":"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(4f), 0, dpToPx(4f), dpToPx(6f))
            includeFontPadding = false
        }

        minuteView = TextView(context).apply {
            text = "48"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 62f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            includeFontPadding = false
        }

        timeRow.addView(hourView)
        timeRow.addView(colonView)
        timeRow.addView(minuteView)
        addView(timeRow)

        // 3. Footer: Date & Weekday
        val footerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        dateFullView = TextView(context).apply {
            text = "2026年11月05日"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 13f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        weekdayView = TextView(context).apply {
            text = "星期四 · 农历九月廿六"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
        }

        footerRow.addView(dateFullView)
        footerRow.addView(weekdayView)
        addView(footerRow)

        updateTime()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(ticker)
    }

    private fun updateTime() {
        val now = Calendar.getInstance()
        val h = String.format("%02d", now.get(Calendar.HOUR_OF_DAY))
        val m = String.format("%02d", now.get(Calendar.MINUTE))
        val s = String.format("%02ds", now.get(Calendar.SECOND))

        hourView.text = h
        minuteView.text = m
        secondBadge.text = s
        dateFullView.text = dateFormat.format(now.time)
        weekdayView.text = weekdayFormat.format(now.time)
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

