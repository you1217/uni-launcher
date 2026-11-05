package xyz.i4o.uni.launcher.desktop

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Nocturne-styled glass card container for UNI Launcher with Edit Mode support.
 *
 * Features:
 * - 24dp rounded corners clipped directly via hardware RenderNode
 * - Dark-glass base with delicate 1dp edge highlight (#26FFFFFF)
 * - Card-type-specific bottom ambient glow (RadialGradient)
 * - Anti-intercept resize handle with 48dp touch hot-zone
 * - Top-right Delete Button (visible in edit mode)
 * - Long-press to trigger desktop Edit Mode
 */
class EditableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var card: LauncherCard = LauncherCard(id = "default")
        set(value) {
            field = value
            updateGlowColor()
            updateLayoutParamsFromCard()
        }

    var isEditMode: Boolean = false
        set(value) {
            field = value
            deleteBtn.visibility = if (value) View.VISIBLE else View.GONE
            invalidate()
        }

    var onCardResized: ((LauncherCard) -> Unit)? = null
    var onCardDeleted: ((LauncherCard) -> Unit)? = null
    var onCardLongClick: ((LauncherCard) -> Unit)? = null
    var onCardClickInEditMode: ((LauncherCard) -> Unit)? = null

    val contentView: FrameLayout
    private val resizeHandle: ImageView
    private val deleteBtn: ImageView

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f).toFloat()
        color = Color.parseColor("#26FFFFFF")
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#E00C111C") // High-contrast Nocturne base
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var glowColor: Int = Color.parseColor("#383BE2C0")
    private val cornerRadius = dpToPx(24f).toFloat()
    private val handleTouchSize = dpToPx(48f)

    private var isResizing = false
    private var downX = 0f
    private var downY = 0f
    private var startColSpan = 2
    private var startRowSpan = 2

    init {
        // GPU outline clipping
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }

        // Inner content frame
        contentView = FrameLayout(context)
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Delete button at top-right corner
        deleteBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_delete_card)
            val btnSize = dpToPx(24f)
            val lp = LayoutParams(btnSize, btnSize).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(8f)
                rightMargin = dpToPx(8f)
            }
            layoutParams = lp
            visibility = View.GONE
            setOnClickListener {
                onCardDeleted?.invoke(card)
            }
        }
        addView(deleteBtn)

        // Resize handle at bottom-right corner
        resizeHandle = ImageView(context).apply {
            setImageResource(R.drawable.ic_resize_handle)
            val iconSize = dpToPx(20f)
            val lp = LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                rightMargin = dpToPx(8f)
                bottomMargin = dpToPx(8f)
            }
            layoutParams = lp
        }
        addView(resizeHandle)

        setOnClickListener {
            if (isEditMode) {
                onCardClickInEditMode?.invoke(card)
            }
        }

        setOnLongClickListener {
            if (isEditMode) {
                val clipData = ClipData.newPlainText("cardId", card.id)
                val shadow = DragShadowBuilder(this)
                startDragAndDrop(clipData, shadow, card, 0)
                true
            } else {
                onCardLongClick?.invoke(card)
                true
            }
        }

        setWillNotDraw(false)
    }

    private fun updateGlowColor() {
        glowColor = when (card.type) {
            "vehicle", "body" -> Color.parseColor("#38FFB057") // Warm Amber
            "music" -> Color.parseColor("#343BE2C0")   // Nocturne Mint
            "trafficLight" -> Color.parseColor("#363BE2C0") // Mint Green
            "climate" -> Color.parseColor("#346FD3FF") // Cyan
            "clockWeather" -> Color.parseColor("#348E6CFF") // Violet
            "fuel" -> Color.parseColor("#363BE2C0")
            "calendar" -> Color.parseColor("#346FD3FF")
            "shortcuts", "apps", "apps_full" -> Color.parseColor("#308E6CFF") // Violet
            else -> Color.parseColor("#283BE2C0")
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val radius = h.toFloat() * 0.85f
            glowPaint.shader = RadialGradient(
                w / 2f,
                h.toFloat() + radius * 0.15f,
                radius,
                glowColor,
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Dark glass base
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Nocturne Ambient Bottom Glow
        canvas.drawRect(0f, 0f, w, h, glowPaint)

        super.onDraw(canvas)

        // 3. Delicate 1dp border
        val offset = borderPaint.strokeWidth / 2f
        val rect = RectF(offset, offset, w - offset, h - offset)
        if (isResizing || isEditMode) {
            borderPaint.color = Color.parseColor("#993BE2C0")
            borderPaint.strokeWidth = dpToPx(1.5f).toFloat()
        } else {
            borderPaint.color = Color.parseColor("#26FFFFFF")
            borderPaint.strokeWidth = dpToPx(1f).toFloat()
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    private fun updateLayoutParamsFromCard() {
        val lp = layoutParams as? DesktopGridLayout.LayoutParams ?: DesktopGridLayout.LayoutParams()
        lp.col = card.x
        lp.row = card.y
        lp.colSpan = card.w
        lp.rowSpan = card.h
        layoutParams = lp
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handleAreaLeft = width - handleTouchSize
        val handleAreaTop = height - handleTouchSize

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x >= handleAreaLeft && event.y >= handleAreaTop) {
                    isResizing = true
                    downX = event.rawX
                    downY = event.rawY
                    startColSpan = card.w
                    startRowSpan = card.h
                    parent?.requestDisallowInterceptTouchEvent(true)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing) {
                    val parentGrid = parent as? DesktopGridLayout ?: return true
                    val cellW = (parentGrid.cellWidth + parentGrid.cellGap).toFloat()
                    val cellH = (parentGrid.cellHeight + parentGrid.cellGap).toFloat()

                    if (cellW > 0 && cellH > 0) {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY

                        val colDelta = Math.round(dx / cellW)
                        val rowDelta = Math.round(dy / cellH)

                        val maxAllowedW = parentGrid.columns - card.x
                        val maxAllowedH = parentGrid.rows - card.y

                        val targetW = (startColSpan + colDelta).coerceIn(card.minW, card.maxW.coerceAtMost(maxAllowedW))
                        val targetH = (startRowSpan + rowDelta).coerceIn(card.minH, card.maxH.coerceAtMost(maxAllowedH))

                        if (targetW != card.w || targetH != card.h) {
                            card.w = targetW
                            card.h = targetH
                            val lp = layoutParams as? DesktopGridLayout.LayoutParams
                            if (lp != null) {
                                lp.colSpan = targetW
                                lp.rowSpan = targetH
                                parentGrid.requestLayout()
                            }
                        }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isResizing) {
                    isResizing = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                    onCardResized?.invoke(card)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setCardContent(view: View) {
        contentView.removeAllViews()
        contentView.addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
