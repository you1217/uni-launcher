package xyz.i4o.uni.launcher.desktop

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * High-performance vector map canvas for in-car desktop navigation.
 * Renders dark-palette road networks, green parks, water bodies, and glowing routes.
 */
class NavigationVectorMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var zoom: Float = 1.0f
        set(value) {
            field = value.coerceIn(1.0f, 1.8f)
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A1118")
    }

    private val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1476ad84")
        style = Paint.Style.FILL
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#292f7d97")
        style = Paint.Style.FILL
    }

    private val secondaryRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F92BBBF")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val primaryRoadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3392BBBE")
        strokeWidth = 18f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val routeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D3BE2C0")
        strokeWidth = 26f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val routeCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3BE2C0")
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#998CA5AA")
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val carDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3BE2C0")
        style = Paint.Style.FILL
    }

    private val carHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263BE2C0")
        style = Paint.Style.FILL
    }

    private var pulseRadius = 36f

    init {
        // Pulse animation for the car locator
        val animator = ValueAnimator.ofFloat(30f, 48f).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                pulseRadius = it.animatedValue as Float
                invalidate()
            }
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        canvas.save()

        // 1. Fill base dark canvas
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Center scale according to zoom and 1144x732 reference frame
        val refW = 1144f
        val refH = 732f
        val baseScale = maxOf(w / refW, h / refH)
        val finalScale = baseScale * zoom

        val cx = w / 2f
        val cy = h / 2f
        canvas.translate(cx, cy)
        canvas.scale(finalScale, finalScale)
        canvas.translate(-refW / 2f, -refH / 2f)

        // 3. Draw Water Bodies
        val waterPath = Path().apply {
            moveTo(1080f, -10f)
            cubicTo(960f, 180f, 1120f, 260f, 975f, 425f)
            cubicTo(830f, 590f, 960f, 620f, 1144f, 750f)
            lineTo(1200f, 750f)
            lineTo(1200f, -10f)
            close()
        }
        canvas.drawPath(waterPath, waterPaint)

        // 4. Draw Green Parks
        val park1 = Path().apply {
            moveTo(55f, 240f)
            lineTo(305f, 230f)
            lineTo(369f, 378f)
            lineTo(275f, 507f)
            lineTo(44f, 452f)
            close()
        }
        val park2 = Path().apply {
            moveTo(720f, 410f)
            lineTo(890f, 422f)
            lineTo(907f, 574f)
            lineTo(701f, 604f)
            close()
        }
        canvas.drawPath(park1, parkPaint)
        canvas.drawPath(park2, parkPaint)

        // 5. Draw Road Grid (Secondary)
        canvas.drawLine(-10f, 235f, 1144f, 235f, secondaryRoadPaint)
        canvas.drawLine(-10f, 390f, 1144f, 390f, secondaryRoadPaint)
        canvas.drawLine(-10f, 575f, 1144f, 575f, secondaryRoadPaint)
        canvas.drawLine(180f, -10f, 180f, 760f, secondaryRoadPaint)
        canvas.drawLine(330f, -10f, 330f, 760f, secondaryRoadPaint)
        canvas.drawLine(780f, -10f, 780f, 760f, secondaryRoadPaint)
        canvas.drawLine(950f, -10f, 950f, 760f, secondaryRoadPaint)

        // Diagonal Highway
        canvas.drawLine(-10f, 20f, 1144f, 620f, secondaryRoadPaint)

        // 6. Draw Primary Arterials
        canvas.drawLine(-10f, 145f, 1154f, 145f, primaryRoadPaint)
        canvas.drawLine(-10f, 470f, 1154f, 470f, primaryRoadPaint)
        canvas.drawLine(487f, -10f, 487f, 760f, primaryRoadPaint)
        canvas.drawLine(650f, -10f, 650f, 760f, primaryRoadPaint)

        // 7. Draw Landmarks
        canvas.drawText("城市公园", 110f, 313f, textPaint)
        canvas.drawText("滨河路", 225f, 416f, textPaint)
        canvas.drawText("城市大道", 710f, 181f, textPaint)
        canvas.drawText("文化广场", 724f, 506f, textPaint)

        // 8. Draw Active Navigation Route
        val routePath = Path().apply {
            moveTo(650f, 520f)
            lineTo(650f, 470f)
            lineTo(780f, 470f)
            lineTo(780f, 145f)
            lineTo(960f, 145f)
        }
        canvas.drawPath(routePath, routeGlowPaint)
        canvas.drawPath(routePath, routeCorePaint)

        // 9. Draw Destination Pin (960, 145)
        canvas.drawCircle(960f, 145f, 16f, carDotPaint)
        val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#16232B")
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(960f, 145f, 16f, targetPaint)

        // 10. Draw Car Locator Halo & Arrow at (650, 520)
        canvas.drawCircle(650f, 520f, pulseRadius, carHaloPaint)
        canvas.drawCircle(650f, 520f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16232B") })

        val arrowPath = Path().apply {
            moveTo(650f, 502f)
            lineTo(664f, 532f)
            lineTo(650f, 524f)
            lineTo(636f, 532f)
            close()
        }
        canvas.drawPath(arrowPath, carDotPaint)

        canvas.restore()
    }
}

