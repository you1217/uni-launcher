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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Nocturne-styled Desktop Settings Dialog.
 * Provides Wallpaper switching, Dock Mode switching (Capsule / Full OEM),
 * Edit Mode trigger, Layout Reset, LAN Wireless Transfer, and System diagnostics.
 */
class DesktopSettingsDialog(
    context: Context,
    private var currentWallpaper: String,
    private var currentDockStyle: String,
    private val onWallpaperSelected: (wallpaperKey: String) -> Unit,
    private val onDockStyleChanged: (dockStyle: String) -> Unit,
    private val onEnterEditMode: () -> Unit,
    private val onResetLayout: () -> Unit
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val scroll = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(dpToPx(780f), dpToPx(560f))
            setBackgroundResource(R.drawable.bg_card_glass)
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scroll.addView(root)

        // 1. Header with Close Button
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
            text = "桌面设置"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }

        val subTitleView = TextView(context).apply {
            text = "管理桌面壁纸、底栏形态、无线快传与卡片排版"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
            setPadding(0, dpToPx(3f), 0, 0)
        }

        titleCol.addView(titleView)
        titleCol.addView(subTitleView)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 18f
            setPadding(dpToPx(12f), dpToPx(6f), dpToPx(12f), dpToPx(6f))
            setBackgroundResource(R.drawable.bg_pill_badge)
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Wallpaper Section
        val wpSectionTitle = TextView(context).apply {
            text = "🎨 原厂暗夜壁纸"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(16f), 0, dpToPx(10f))
        }
        root.addView(wpSectionTitle)

        val wpGrid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val wallpapers = listOf(
            Triple("极夜石墨", "graphite_night", R.drawable.wallpaper_graphite_night),
            Triple("矩阵网格", "precision_grid", R.drawable.wallpaper_precision_grid),
            Triple("海岸薄雾", "coastal_mist", R.drawable.wallpaper_coastal_mist)
        )

        for ((wpTitle, wpKey, wpThumb) in wallpapers) {
            val isSelected = (wpKey == currentWallpaper)
            val wpCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dpToPx(6f), 0, dpToPx(6f), 0)
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_control_circle_accent else R.drawable.bg_pill_badge)
                val p = dpToPx(12f)
                setPadding(p, p, p, p)
                isClickable = true
                isFocusable = true

                val thumb = ImageView(context).apply {
                    setImageResource(wpThumb)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(dpToPx(180f), dpToPx(64f))
                    clipToOutline = true
                }

                val label = TextView(context).apply {
                    text = if (isSelected) "$wpTitle (当前)" else wpTitle
                    setTextColor(if (isSelected) Color.parseColor("#3BE2C0") else Color.WHITE)
                    textSize = 13f
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setPadding(0, dpToPx(8f), 0, 0)
                }

                addView(thumb)
                addView(label)

                setOnClickListener {
                    currentWallpaper = wpKey
                    onWallpaperSelected(wpKey)
                    dismiss()
                }
            }
            wpGrid.addView(wpCard)
        }
        root.addView(wpGrid)

        // 3. Dock Mode Selection Section
        val dockSectionTitle = TextView(context).apply {
            text = "🎛️ 底栏形态风格 (双模架构)"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(10f))
        }
        root.addView(dockSectionTitle)

        val dockGrid = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val dockStyles = listOf(
            Pair("极简悬浮胶囊 (Floating)", "floating"),
            Pair("原厂全功能底栏 (Full OEM)", "full_oem")
        )

        for ((dockTitle, dockKey) in dockStyles) {
            val isSelected = (dockKey == currentDockStyle)
            val dockCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dpToPx(6f), 0, dpToPx(6f), 0)
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_control_circle_accent else R.drawable.bg_pill_badge)
                val p = dpToPx(12f)
                setPadding(p, p, p, p)
                isClickable = true
                isFocusable = true

                val label = TextView(context).apply {
                    text = if (isSelected) "🔘 $dockTitle (生效中)" else "⚪ $dockTitle"
                    setTextColor(if (isSelected) Color.parseColor("#3BE2C0") else Color.WHITE)
                    textSize = 13f
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }

                val desc = TextView(context).apply {
                    text = if (dockKey == "floating") "6 键独立半透明悬浮胶囊，视觉轻盈" else "全宽原厂风格：常驻主副驾温控 + 音乐工具条 + 返回"
                    setTextColor(Color.parseColor("#80FFFFFF"))
                    textSize = 11f
                    setPadding(0, dpToPx(4f), 0, 0)
                }

                addView(label)
                addView(desc)

                setOnClickListener {
                    currentDockStyle = dockKey
                    onDockStyleChanged(dockKey)
                    dismiss()
                }
            }
            dockGrid.addView(dockCard)
        }
        root.addView(dockGrid)

        // 4. Layout & Wireless Transfer Section
        val toolsSectionTitle = TextView(context).apply {
            text = "🛠️ 桌面排版与车机互联"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(10f))
        }
        root.addView(toolsSectionTitle)

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val btnHeight = dpToPx(44f)

        val editModeBtn = TextView(context).apply {
            text = "📐 卡片自由编辑"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
                onEnterEditMode()
            }
        }

        val lanTransferBtn = TextView(context).apply {
            text = "📲 手机无线快传"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
                LanTransferDialog(context).show()
            }
        }

        val resetLayoutBtn = TextView(context).apply {
            text = "🔄 恢复原厂布局"
            setTextColor(Color.parseColor("#FFFF5722"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dismiss()
                onResetLayout()
            }
        }

        val lp1 = LinearLayout.LayoutParams(0, btnHeight, 1f).apply { marginEnd = dpToPx(6f) }
        val lp2 = LinearLayout.LayoutParams(0, btnHeight, 1.2f).apply { marginStart = dpToPx(6f); marginEnd = dpToPx(6f) }
        val lp3 = LinearLayout.LayoutParams(0, btnHeight, 1f).apply { marginStart = dpToPx(6f) }

        actionRow.addView(editModeBtn, lp1)
        actionRow.addView(lanTransferBtn, lp2)
        actionRow.addView(resetLayoutBtn, lp3)
        root.addView(actionRow)

        // 5. System Diagnostics Section
        val infoSectionTitle = TextView(context).apply {
            text = "⚡ 关于 UNI 原生极速引擎"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(8f))
        }
        root.addView(infoSectionTitle)

        val infoBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val v1 = TextView(context).apply {
                text = "• 架构: 纯原生 Android View + RenderThread (零 Flutter / JNI 帧间消耗)"
                setTextColor(Color.parseColor("#B3FFFFFF"))
                textSize = 12f
            }
            val v2 = TextView(context).apply {
                text = "• 适配车型: 领克 05 / 高通骁龙 820A (MSM8996AU) 车载中控"
                setTextColor(Color.parseColor("#B3FFFFFF"))
                textSize = 12f
                setPadding(0, dpToPx(4f), 0, 0)
            }
            val v3 = TextView(context).apply {
                text = "• 性能指标: 启动 ~12ms | 触控延迟 ~16ms | 帧率 60FPS 垂直同步"
                setTextColor(Color.parseColor("#3BE2C0"))
                textSize = 12f
                setPadding(0, dpToPx(4f), 0, 0)
            }
            addView(v1)
            addView(v2)
            addView(v3)
        }
        root.addView(infoBox)

        setContentView(scroll)
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
