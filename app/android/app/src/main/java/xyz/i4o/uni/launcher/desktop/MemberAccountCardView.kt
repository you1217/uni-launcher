package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * Native Member Account Card for UNI Launcher (LynkCo Co-Club).
 * Features user avatar, Co-Club level badge, points balance, and account services.
 */
class MemberAccountCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

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
            text = "领克账号"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val vipPill = TextView(context).apply {
            text = "Co-Club 尊享会员"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(3f)
            setPadding(hp, vp, hp, vp)
        }

        header.addView(titleView)
        header.addView(vipPill)
        addView(header)

        // 2. Avatar & Info Row
        val contentRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dpToPx(8f)
            }
        }

        val avatar = TextView(context).apply {
            text = "05"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val s = dpToPx(48f)
            layoutParams = LayoutParams(s, s)
        }
        contentRow.addView(avatar)

        val infoCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(14f)
            }
        }

        val nameView = TextView(context).apply {
            text = "领克车主"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

        val levelView = TextView(context).apply {
            text = "Lv.5 星钻俱乐部 · 车辆主账号已绑定"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
            setPadding(0, dpToPx(2f), 0, 0)
        }

        infoCol.addView(nameView)
        infoCol.addView(levelView)
        contentRow.addView(infoCol)

        addView(contentRow)

        // 3. Footer: Co-Points & Privilege
        val footerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val pointsView = TextView(context).apply {
            text = "领克积分 12,850 · 免费保养权益 × 2"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 11f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val actionView = TextView(context).apply {
            text = "查看权益 ›"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 11f
        }

        footerRow.addView(pointsView)
        footerRow.addView(actionView)
        addView(footerRow)
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

