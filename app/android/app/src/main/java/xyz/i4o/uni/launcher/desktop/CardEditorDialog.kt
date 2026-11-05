package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * Native Card Editor Dialog for UNI Launcher.
 * Allows customizing card title, switching preset grid sizes (4x2, 4x3, 4x4, 7x6, etc.), and removing cards.
 */
class CardEditorDialog(
    context: Context,
    private val card: LauncherCard,
    private val onSave: (title: String, w: Int, h: Int) -> Unit,
    private val onDelete: () -> Unit
) : Dialog(context) {

    private var currentW = card.w
    private var currentH = card.h

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6121721"))
                cornerRadius = dpToPx(24f).toFloat()
                setStroke(dpToPx(1.5f), Color.parseColor("#4D3BE2C0"))
            }
            background = bg
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(560f), ViewGroup.LayoutParams.WRAP_CONTENT)
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

        val titleView = TextView(context).apply {
            text = "编辑卡片 · ${card.title}"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40f), dpToPx(40f))
            isClickable = true
            isFocusable = true
            setOnClickListener { dismiss() }
        }

        header.addView(titleView)
        header.addView(closeBtn)
        root.addView(header)

        // 2. Custom Title Input
        val titleLabel = TextView(context).apply {
            text = "自定义卡片名称"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            setPadding(0, dpToPx(16f), 0, dpToPx(6f))
        }
        root.addView(titleLabel)

        val titleInput = EditText(context).apply {
            setText(card.title)
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 15f
            setHintTextColor(Color.parseColor("#4D8A99AD"))
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val hp = dpToPx(14f)
            val vp = dpToPx(10f)
            setPadding(hp, vp, hp, vp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(titleInput)

        // 3. Size Preset Selector
        val sizeLabel = TextView(context).apply {
            text = "网格尺寸预设 (当前: ${card.w} × ${card.h})"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            setPadding(0, dpToPx(16f), 0, dpToPx(8f))
        }
        root.addView(sizeLabel)

        val presetRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val presets = listOf(
            Triple("4×2 紧凑", 4, 2),
            Triple("4×3 标准", 4, 3),
            Triple("4×4 中卡", 4, 4),
            Triple("7×6 半屏", 7, 6)
        )

        val presetButtons = mutableListOf<TextView>()

        for ((label, w, h) in presets) {
            val btn = TextView(context).apply {
                text = label
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                val isSelected = (w == currentW && h == currentH)
                if (isSelected) {
                    setTextColor(Color.parseColor("#3BE2C0"))
                    setBackgroundResource(R.drawable.bg_dock_pill_accent)
                } else {
                    setTextColor(Color.parseColor("#8A99AD"))
                    setBackgroundResource(R.drawable.bg_dock_item_glass)
                }
                val hp = dpToPx(10f)
                val vp = dpToPx(8f)
                setPadding(hp, vp, hp, vp)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dpToPx(4f), 0, dpToPx(4f), 0)
                }
                isClickable = true
                setOnClickListener {
                    currentW = w
                    currentH = h
                    sizeLabel.text = "网格尺寸预设 (已选: ${w}×${h})"
                    for (b in presetButtons) {
                        b.setTextColor(Color.parseColor("#8A99AD"))
                        b.setBackgroundResource(R.drawable.bg_dock_item_glass)
                    }
                    setTextColor(Color.parseColor("#3BE2C0"))
                    setBackgroundResource(R.drawable.bg_dock_pill_accent)
                }
            }
            presetButtons.add(btn)
            presetRow.addView(btn)
        }
        root.addView(presetRow)

        // 4. Action Buttons (Save & Delete)
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(24f)
            }
        }

        val deleteBtn = TextView(context).apply {
            text = "移除卡片"
            setTextColor(Color.parseColor("#FF5E62"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val hp = dpToPx(16f)
            val vp = dpToPx(8f)
            setPadding(hp, vp, hp, vp)
            isClickable = true
            setOnClickListener {
                dismiss()
                onDelete()
            }
        }

        val spacer = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val saveBtn = TextView(context).apply {
            text = "保存并应用"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_dock_pill_accent)
            val hp = dpToPx(24f)
            val vp = dpToPx(10f)
            setPadding(hp, vp, hp, vp)
            isClickable = true
            setOnClickListener {
                val newTitle = titleInput.text.toString().trim().ifEmpty { card.title }
                dismiss()
                onSave(newTitle, currentW, currentH)
            }
        }

        actionRow.addView(deleteBtn)
        actionRow.addView(spacer)
        actionRow.addView(saveBtn)
        root.addView(actionRow)

        setContentView(root)
    }

    private fun dpToPx(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()
}

