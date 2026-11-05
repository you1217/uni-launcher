package xyz.i4o.uni.launcher.desktop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat

/**
 * Base card container with dark-glass styling, hardware outline clipping,
 * and immediate touch-responsive corner resize handle.
 */
class EditableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var card: LauncherCard = LauncherCard(id = "default")
        set(value) {
            field = value
            updateLayoutParamsFromCard()
        }

    var onCardResized: ((LauncherCard) -> Unit)? = null

    val contentView: FrameLayout
    private val resizeHandle: ImageView
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f).toFloat()
        color = Color.parseColor("#33FFFFFF")
    }

    private val cornerRadius = dpToPx(16f).toFloat()
    private val handleTouchSize = dpToPx(48f)

    private var isResizing = false
    private var downX = 0f
    private var downY = 0f
    private var startColSpan = 2
    private var startRowSpan = 2

    init {
        // Hardware accelerated outline clipping
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        setBackgroundColor(Color.parseColor("#2E121724")) // Dark glass base

        // Card content container
        contentView = FrameLayout(context)
        addView(contentView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Resize handle at bottom-right corner
        resizeHandle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_crop) // System icon fallback or custom drawable
            setColorFilter(Color.parseColor("#80FFFFFF"))
            val iconSize = dpToPx(20f)
            val lp = LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                rightMargin = dpToPx(8f)
                bottomMargin = dpToPx(8f)
            }
            layoutParams = lp
        }
        addView(resizeHandle)

        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Subtle outline border
        val strokeOffset = borderPaint.strokeWidth / 2f
        val rect = RectF(
            strokeOffset,
            strokeOffset,
            width - strokeOffset,
            height - strokeOffset
        )
        if (isResizing) {
            borderPaint.color = Color.parseColor("#8064B5F6") // Soft blue accent while resizing
        } else {
            borderPaint.color = Color.parseColor("#2BFFFFFF")
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

