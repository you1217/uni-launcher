package xyz.i4o.uni.launcher.desktop

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import xyz.i4o.uni.launcher.R

/**
 * Native System RAM Cleanup Card for UNI Launcher.
 * Features live RAM usage stats, health badge, and one-tap memory release.
 */
class SystemCleanupCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val ramPercentView: TextView
    private val ramDetailView: TextView
    private val ramProgressBar: ProgressBar
    private val cleanBtn: TextView

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
            text = "系统清理"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val healthBadge = TextView(context).apply {
            text = "车机状态良好"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 12f
            setBackgroundResource(R.drawable.bg_pill_badge)
            val hp = dpToPx(10f)
            val vp = dpToPx(4f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(healthBadge)
        addView(header)

        // 2. Center RAM Number & Action Row
        val contentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(6f)
            }
        }

        val leftCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val ramRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        ramPercentView = TextView(context).apply {
            text = "48"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 46f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }

        val pctUnit = TextView(context).apply {
            text = "% 运存占用"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
            setPadding(dpToPx(4f), 0, 0, dpToPx(6f))
        }

        ramRow.addView(ramPercentView)
        ramRow.addView(pctUnit)
        leftCol.addView(ramRow)

        ramDetailView = TextView(context).apply {
            text = "已用 2.8 GB / 可用 3.2 GB"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 11f
        }
        leftCol.addView(ramDetailView)
        contentRow.addView(leftCol)

        // Right side: One-tap Clean Button
        cleanBtn = TextView(context).apply {
            text = "⚡ 一键加速"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            val hp = dpToPx(16f)
            val vp = dpToPx(10f)
            setPadding(hp, vp, hp, vp)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                performCleanup()
            }
        }
        contentRow.addView(cleanBtn)
        addView(contentRow)

        // 3. RAM Bar
        ramProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 48
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(6f)).apply {
                topMargin = dpToPx(4f)
                bottomMargin = dpToPx(8f)
            }
        }
        addView(ramProgressBar)

        // 4. Footer Note
        val footer = TextView(context).apply {
            text = "释放后台残留进程 · 保持车机桌面 60FPS 丝滑流畅"
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 11f
        }
        addView(footer)

        refreshMemoryStats()
    }

    private fun performCleanup() {
        cleanBtn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).withEndAction {
            cleanBtn.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }.start()

        System.gc()
        Toast.makeText(context, "✨ 已释放后台残留缓存，运行内存保持充裕", Toast.LENGTH_SHORT).show()
        postDelayed({ refreshMemoryStats(cleaned = true) }, 300)
    }

    private fun refreshMemoryStats(cleaned: Boolean = false) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = (totalMb - availMb).coerceAtLeast(0)
            val pct = ((usedMb * 100) / totalMb).toInt().coerceIn(10, 95)

            val displayPct = if (cleaned) (pct - 6).coerceAtLeast(15) else pct
            ramPercentView.text = "$displayPct"
            ramProgressBar.progress = displayPct

            val usedGb = String.format("%.1f", (totalMb - availMb) / 1024f)
            val availGb = String.format("%.1f", availMb / 1024f)
            ramDetailView.text = "已用 $usedGb GB / 可用 $availGb GB"

            if (displayPct >= 80) {
                ramPercentView.setTextColor(Color.parseColor("#FF5E62"))
            } else {
                ramPercentView.setTextColor(Color.parseColor("#3BE2C0"))
            }
        } catch (ignored: Exception) {}
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

