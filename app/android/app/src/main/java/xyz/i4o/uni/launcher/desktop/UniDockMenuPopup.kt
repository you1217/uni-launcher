package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import xyz.i4o.uni.launcher.MainActivity
import xyz.i4o.uni.launcher.R

/**
 * Anchored Dark-Glass Popup Menu extending above the UNI dock button.
 * 1:1 Parity with UniDockPopupRoute from UNI Launcher.
 * Contains:
 * 1. 车辆信息 -> VehicleReadoutDialog
 * 2. 文件互传 -> LanTransferDialog
 * 3. UNI桌面设置 -> MainActivity.showDesktopSettings()
 */
class UniDockMenuPopup(private val context: Context) : PopupWindow(context) {

    init {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
            layoutParams = ViewGroup.LayoutParams(dpToPx(340f), ViewGroup.LayoutParams.WRAP_CONTENT)
            elevation = dpToPx(16f).toFloat()
        }

        // Item 1: 车辆信息
        root.addView(createMenuItem(
            iconRes = R.drawable.ic_dock_car,
            title = "车辆信息",
            subtitle = "实时胎压 · 三电数据 · 车身诊断"
        ) {
            dismiss()
            VehicleReadoutDialog(context).show()
        })

        val div1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1f)).apply {
                topMargin = dpToPx(6f)
                bottomMargin = dpToPx(6f)
            }
            setBackgroundColor(Color.parseColor("#1FFFFFFF"))
        }
        root.addView(div1)

        // Item 2: 文件互传
        root.addView(createMenuItem(
            iconRes = R.drawable.ic_dock_apps,
            title = "无线文件互传",
            subtitle = "手机扫码高速传输文件 (端口 8787)"
        ) {
            dismiss()
            LanTransferDialog(context).show()
        })

        val div2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1f)).apply {
                topMargin = dpToPx(6f)
                bottomMargin = dpToPx(6f)
            }
            setBackgroundColor(Color.parseColor("#1FFFFFFF"))
        }
        root.addView(div2)

        // Item 3: UNI桌面设置
        root.addView(createMenuItem(
            iconRes = R.drawable.ic_dock_settings,
            title = "UNI桌面设置",
            subtitle = "外观壁纸 · 卡片网格 · 氛围灯联动"
        ) {
            dismiss()
            findActivity(context)?.showDesktopSettings()
        })

        contentView = root
        width = dpToPx(340f)
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        animationStyle = android.R.style.Animation_Dialog
    }

    private fun createMenuItem(
        iconRes: Int,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val p = dpToPx(10f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(Color.parseColor("#3BE2C0"))
                layoutParams = LinearLayout.LayoutParams(dpToPx(28f), dpToPx(28f))
            }
            addView(icon)

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dpToPx(12f)
                }
            }

            val titleView = TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
            }
            val subtitleView = TextView(context).apply {
                text = subtitle
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 11f
                setPadding(0, dpToPx(2f), 0, 0)
            }
            textCol.addView(titleView)
            textCol.addView(subtitleView)
            addView(textCol)

            val arrow = TextView(context).apply {
                text = "›"
                setTextColor(Color.parseColor("#66FFFFFF"))
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dpToPx(6f), 0, dpToPx(4f), 0)
            }
            addView(arrow)

            setOnClickListener { onClick() }
        }
    }

    fun showAbove(anchor: View) {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]

        // Measure content
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = contentView.measuredHeight

        // Show above the anchor aligned to its right edge
        val x = anchorX + anchor.width - width
        val y = anchorY - popupHeight - dpToPx(12f)
        showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    private fun findActivity(context: Context): MainActivity? {
        var ctx: Context? = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is MainActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}

