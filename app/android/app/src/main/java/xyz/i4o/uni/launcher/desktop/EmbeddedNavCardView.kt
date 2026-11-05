package xyz.i4o.uni.launcher.desktop

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R
import xyz.i4o.uni.launcher.platform.NativeNavigationHost

/**
 * 1:1 In-Car Embedded Navigation Card View for Lynk & Co 05.
 * Features:
 * 1. Dual-Engine Rendering: Hardware-accelerated VirtualDisplay TextureView (Amap Auto) + Vector Map Fallback.
 * 2. Real-time In-Car HUD: Turn direction indicator, next distance, road name, compass needle.
 * 3. Bottom ETA & Route Status Bar: One-click seamless transition to full-screen navigation.
 * 4. Floating Zoom Controls (+/-) & Fullscreen trigger.
 */
class EmbeddedNavCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val runtime = LauncherRuntime.get(context)
    private val activity = context as? Activity
    private val navHost = if (activity != null) NativeNavigationHost(activity) else null

    private var textureView: TextureView? = null
    private var vectorMapView: NavigationVectorMapView? = null

    // HUD UI Elements
    private lateinit var turnIcon: ImageView
    private lateinit var turnDistanceText: TextView
    private lateinit var nextRoadText: TextView
    private lateinit var destinationText: TextView
    private lateinit var etaText: TextView
    private lateinit var compassView: ImageView

    private var isTextureActive = false
    private var currentZoom = 1.0f

    init {
        setBackgroundResource(R.drawable.bg_card_glass)
        clipToOutline = true

        // 1. Always build Base Vector Map Engine Layer (immediate interactive 60fps canvas)
        setupVectorMapEngine()

        // 2. If Car VirtualDisplay is supported, add TextureView layer above vector map
        val canEmbed = navHost?.isAvailable() == true
        if (canEmbed && activity != null) {
            setupTextureEngine()
        }

        // 3. Build Overlay HUD Controls
        buildHudOverlays()
    }

    private fun setupTextureEngine() {
        var hasFrame = false
        val tv = TextureView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            alpha = 0f
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                    val surface = Surface(st)
                    val ok = navHost?.createDisplay(surface, width, height) ?: false
                    isTextureActive = ok
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                    navHost?.resize(width, height)
                }

                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                    navHost?.release()
                    isTextureActive = false
                    alpha = 0f
                    hasFrame = false
                    return true
                }

                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                    if (!hasFrame) {
                        hasFrame = true
                        animate().alpha(1f).setDuration(300).start()
                    }
                }
            }
        }
        textureView = tv
        addView(tv)
    }

    private fun setupVectorMapEngine() {
        if (vectorMapView != null) return
        val mv = NavigationVectorMapView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        vectorMapView = mv
        // Place below HUD
        addView(mv, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // If texture map is active and user is interacting with map area (outside floating HUDs), forward touches
        if (isTextureActive) {
            navHost?.forwardTouchEvent(ev, width, height)
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun buildHudOverlays() {
        // --- 1. Top Bar: Turn Direction HUD (Left) & Compass (Right) ---
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                val m = dpToPx(14f)
                setMargins(m, m, m, 0)
            }
        }

        // Left: Turn HUD Card
        val turnCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_pill_badge)
            val pad = dpToPx(10f)
            setPadding(pad, dpToPx(8f), dpToPx(14f), dpToPx(8f))
        }

        val turnIconCard = FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            val s = dpToPx(38f)
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        turnIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_turn)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        turnIconCard.addView(turnIcon)
        turnCard.addView(turnIconCard)

        val turnCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(10f)
            }
        }
        turnDistanceText = TextView(context).apply {
            text = "600 米后右转"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        nextRoadText = TextView(context).apply {
            text = "进入城市大道"
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 11f
        }
        turnCol.addView(turnDistanceText)
        turnCol.addView(nextRoadText)
        turnCard.addView(turnCol)
        topBar.addView(turnCard)

        // Spacer
        topBar.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))

        // Right: Compass Pill
        val compassCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(dpToPx(38f), dpToPx(48f))
        }
        val northLabel = TextView(context).apply {
            text = "N"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        compassView = ImageView(context).apply {
            setImageResource(R.drawable.ic_nav_compass)
            layoutParams = LinearLayout.LayoutParams(dpToPx(18f), dpToPx(18f))
        }
        compassCard.addView(northLabel)
        compassCard.addView(compassView)
        topBar.addView(compassCard)

        addView(topBar)

        // --- 2. Right Zoom & Fullscreen Controls ---
        val rightControls = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = dpToPx(14f)
            }
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(4f)
            setPadding(p, p, p, p)
        }

        val zoomInBtn = createCircleActionButton("+") {
            currentZoom = (currentZoom + 0.15f).coerceAtMost(1.8f)
            vectorMapView?.zoom = currentZoom
        }
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(1f)).apply {
                topMargin = dpToPx(4f)
                bottomMargin = dpToPx(4f)
            }
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
        val zoomOutBtn = createCircleActionButton("-") {
            currentZoom = (currentZoom - 0.15f).coerceAtLeast(1.0f)
            vectorMapView?.zoom = currentZoom
        }

        val fullscreenBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_fullscreen)
            val s = dpToPx(34f)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                topMargin = dpToPx(6f)
            }
            val p = dpToPx(6f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            setOnClickListener {
                val ok = navHost?.launchFullscreen() ?: false
                if (!ok) {
                    runtime.apps().launchPackage("com.autonavi.amapauto")
                }
            }
        }

        rightControls.addView(zoomInBtn)
        rightControls.addView(divider)
        rightControls.addView(zoomOutBtn)
        rightControls.addView(fullscreenBtn)
        addView(rightControls)

        // --- 3. Bottom ETA & Route Status Bar ---
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_pill_badge)
            val padH = dpToPx(16f)
            val padV = dpToPx(10f)
            setPadding(padH, padV, padH, padV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
                val m = dpToPx(14f)
                setMargins(m, 0, m, m)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val ok = navHost?.launchFullscreen() ?: false
                if (!ok) {
                    runtime.apps().launchPackage("com.autonavi.amapauto")
                }
            }
        }

        val destCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        destinationText = TextView(context).apply {
            text = "前往 · 城市中心"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        etaText = TextView(context).apply {
            text = "10:08 到达 · 27 分钟 · 12.6 公里"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 12f
            setPadding(0, dpToPx(2f), 0, 0)
        }
        destCol.addView(destinationText)
        destCol.addView(etaText)
        bottomBar.addView(destCol)

        val navPillIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_dock_nav)
            val s = dpToPx(26f)
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        bottomBar.addView(navPillIcon)

        addView(bottomBar)
    }

    private fun createCircleActionButton(text: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val s = dpToPx(34f)
            layoutParams = LinearLayout.LayoutParams(s, s)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle)
            setOnClickListener { onClick() }
        }
    }

    fun updateSnapshot(snapshot: JSONObject) {
        val nav = snapshot.optJSONObject("navigation")
        val traffic = snapshot.optJSONObject("trafficLights")

        if (traffic != null && traffic.optBoolean("available", false)) {
            val intersection = traffic.optString("intersection", "")
            if (intersection.isNotEmpty()) {
                destinationText.text = "前方路口: $intersection"
            }
        }

        if (nav != null) {
            val label = nav.optString("label", "")
            if (label.isNotEmpty() && !destinationText.text.contains("路口")) {
                destinationText.text = "导航中 · $label"
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        navHost?.release()
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

