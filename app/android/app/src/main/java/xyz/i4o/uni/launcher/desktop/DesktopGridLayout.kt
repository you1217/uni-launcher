package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * High-performance 8x4 Grid ViewGroup for Lynk & Co 1920x1080 display.
 *
 * Measured directly via RenderThread-accelerated coordinates with 0 overdraw.
 */
class DesktopGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var columns: Int = 8
        set(value) {
            field = value
            requestLayout()
        }

    var rows: Int = 4
        set(value) {
            field = value
            requestLayout()
        }

    var cellGap: Int = dpToPx(16f)
        set(value) {
            field = value
            requestLayout()
        }

    var cellWidth: Int = 0
        private set

    var cellHeight: Int = 0
        private set

    init {
        // Default desktop padding
        val horizontalPad = dpToPx(24f)
        val verticalPad = dpToPx(16f)
        setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val totalHeight = MeasureSpec.getSize(heightMeasureSpec)

        val availWidth = totalWidth - paddingLeft - paddingRight - (columns - 1) * cellGap
        val availHeight = totalHeight - paddingTop - paddingBottom - (rows - 1) * cellGap

        cellWidth = if (columns > 0) (availWidth / columns).coerceAtLeast(1) else 1
        cellHeight = if (rows > 0) (availHeight / rows).coerceAtLeast(1) else 1

        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as? LayoutParams ?: LayoutParams(0, 0, 2, 2)
                val childWidth = (cellWidth * lp.colSpan + cellGap * (lp.colSpan - 1)).coerceAtLeast(1)
                val childHeight = (cellHeight * lp.rowSpan + cellGap * (lp.rowSpan - 1)).coerceAtLeast(1)

                val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
                val childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as? LayoutParams ?: continue
                val left = paddingLeft + lp.col * (cellWidth + cellGap)
                val top = paddingTop + lp.row * (cellHeight + cellGap)
                val right = left + child.measuredWidth
                val bottom = top + child.measuredHeight
                child.layout(left, top, right, bottom)
            }
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(0, 0, 2, 2)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return when (p) {
            is LayoutParams -> LayoutParams(p.col, p.row, p.colSpan, p.rowSpan)
            is MarginLayoutParams -> LayoutParams(0, 0, 2, 2)
            else -> LayoutParams(0, 0, 2, 2)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(0, 0, 2, 2)
    }

    fun updateCardPosition(view: View, col: Int, row: Int, colSpan: Int, rowSpan: Int) {
        val lp = view.layoutParams as? LayoutParams ?: LayoutParams(col, row, colSpan, rowSpan)
        lp.col = col
        lp.row = row
        lp.colSpan = colSpan
        lp.rowSpan = rowSpan
        view.layoutParams = lp
        requestLayout()
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    class LayoutParams(
        var col: Int = 0,
        var row: Int = 0,
        var colSpan: Int = 2,
        var rowSpan: Int = 2
    ) : ViewGroup.MarginLayoutParams(MATCH_PARENT, MATCH_PARENT)
}

