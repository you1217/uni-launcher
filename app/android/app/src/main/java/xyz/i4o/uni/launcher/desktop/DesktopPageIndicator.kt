package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * 1:1 Smooth Desktop Page Indicator for UNI Launcher.
 * Features a dynamic sliding mint-colored capsule indicating active page.
 */
class DesktopPageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var pageCount: Int = 2
        set(value) {
            field = value.coerceAtLeast(1)
            visibility = if (field > 1) VISIBLE else GONE
            requestLayout()
            invalidate()
        }

    private var position: Int = 0
    private var positionOffset: Float = 0f

    private val inactiveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33FFFFFF")
    }

    private val activeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#3BE2C0") // Mint accent
    }

    private val dotRadius = dpToPx(3f).toFloat()
    private val dotDiameter = dotRadius * 2f
    private val activePillWidth = dpToPx(20f).toFloat()
    private val dotSpacing = dpToPx(8f).toFloat()

    private val activeRect = RectF()

    init {
        visibility = if (pageCount > 1) VISIBLE else GONE
    }

    fun attachToViewPager(viewPager: ViewPager2) {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(pos: Int, posOffset: Float, posOffsetPixels: Int) {
                position = pos
                positionOffset = posOffset
                invalidate()
            }

            override fun onPageSelected(pos: Int) {
                position = pos
                positionOffset = 0f
                invalidate()
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalDotsWidth = if (pageCount > 1) {
            activePillWidth + (pageCount - 1) * dotDiameter + (pageCount - 1) * dotSpacing
        } else {
            activePillWidth
        }
        val desiredW = totalDotsWidth.toInt() + paddingLeft + paddingRight
        val desiredH = dpToPx(16f) + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredW, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pageCount <= 1) return

        val totalWidth = activePillWidth + (pageCount - 1) * dotDiameter + (pageCount - 1) * dotSpacing
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        // Draw Inactive background dots
        var currentX = startX
        for (i in 0 until pageCount) {
            val dotW = if (i == position) {
                dotDiameter + (activePillWidth - dotDiameter) * (1f - positionOffset)
            } else if (i == position + 1) {
                dotDiameter + (activePillWidth - dotDiameter) * positionOffset
            } else {
                dotDiameter
            }

            canvas.drawCircle(currentX + dotRadius, centerY, dotRadius, inactiveDotPaint)
            currentX += dotDiameter + dotSpacing
        }

        // Draw Smooth Sliding Active Pill
        val activeCenterOffset = (position + positionOffset) * (dotDiameter + dotSpacing)
        val pillLeft = startX + activeCenterOffset
        val pillRight = (pillLeft + activePillWidth).coerceAtMost(startX + totalWidth)
        activeRect.set(pillLeft, centerY - dotRadius, pillRight, centerY + dotRadius)
        canvas.drawRoundRect(activeRect, dotRadius, dotRadius, activeDotPaint)
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

