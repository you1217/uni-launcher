package xyz.i4o.uni.launcher.desktop

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.MainActivity
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Complete UNI Bottom Dock for Lynk & Co 05.
 *
 * Provides:
 * 1. Left System Controls (352dp width):
 *    - Back (ic_dock_back)
 *    - Home (ic_dock_home, active indicator dot on page 0, long-press to settings)
 *    - All Apps (ic_dock_apps)
 *    - Recents (ic_dock_recents, invokes recent tasks switcher)
 *    - 360 Camera (ic_dock_camera)
 *
 * 2. Center Dual-Zone Climate Capsule:
 *    - A/C Toggle (climate-mode ac)
 *    - Driver Temp (22.0°)
 *    - DockClimateButton: AUTO text or Fan Icon with 7 tilted speed segments
 *    - Circulation Icon: Internal / Fresh air toggle (climate-mode recirc)
 *    - Passenger Temp (22.0°)
 *    - Climate Power Toggle (climate-power)
 *
 * 3. Right Multimedia Capsule:
 *    - Prev, Play/Pause, Next
 *    - Album Vinyl Disc (spins when playing)
 *    - Marquee Track Title & Artist
 *    - 3-Bar Live Equalizer Visualizer
 *    - Real-time Progress Line
 *
 * 4. Far-Right UNI Menu Button:
 *    - Circular UNI Emblem button
 *    - Opens UniDockMenuPopup (VehicleReadout, LanTransfer, DesktopSettings)
 *
 * 5. Supports both Floating Capsule and Full-Width OEM styles.
 */
class FullOemDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val runtime = LauncherRuntime.get(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var dockStyle: String = "floating" // "floating" or "full_oem"
    private lateinit var contentLayout: LinearLayout

    // Left UI
    private lateinit var homeIndicatorDot: View

    // Center Climate UI
    private lateinit var acBtn: TextView
    private lateinit var driverTempText: TextView
    private lateinit var fanAutoText: TextView
    private lateinit var fanManualContainer: LinearLayout
    private lateinit var fanIcon: ImageView
    private val fanSegmentViews = mutableListOf<View>()
    private lateinit var recircIcon: ImageView
    private lateinit var passengerTempText: TextView
    private lateinit var climatePowerIcon: ImageView

    // Right Media UI
    private lateinit var discImageView: ImageView
    private var discRotationAnimator: ObjectAnimator? = null
    private lateinit var mediaTitleText: TextView
    private lateinit var mediaArtistText: TextView
    private lateinit var playPauseBtn: ImageView
    private lateinit var eqBar1: View
    private lateinit var eqBar2: View
    private lateinit var eqBar3: View
    private lateinit var mediaProgressBar: View

    // Far Right UNI Menu
    private lateinit var uniMenuBtn: View

    private var eqAnimator: ValueAnimator? = null
    private var isCurrentlyPlaying = false

    private val updateRunnable = Runnable {
        updateState()
    }

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(64f))
        buildDockView()
        setupEqualizerAnimation()
        setupDiscAnimation()
    }

    fun setActivePage(position: Int) {
        if (::homeIndicatorDot.isInitialized) {
            homeIndicatorDot.visibility = if (position == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    fun setDockStyle(style: String) {
        dockStyle = style
        applyStyleAppearance()
    }

    private fun applyStyleAppearance() {
        if (dockStyle == "floating") {
            setBackgroundResource(R.drawable.bg_dock_capsule)
            val lp = layoutParams as? MarginLayoutParams ?: MarginLayoutParams(LayoutParams.MATCH_PARENT, dpToPx(64f))
            lp.setMargins(dpToPx(16f), dpToPx(4f), dpToPx(16f), dpToPx(8f))
            layoutParams = lp
        } else {
            setBackgroundColor(Color.parseColor("#F2090C12"))
            val lp = layoutParams as? MarginLayoutParams ?: MarginLayoutParams(LayoutParams.MATCH_PARENT, dpToPx(64f))
            lp.setMargins(0, 0, 0, 0)
            layoutParams = lp
        }
    }

    private fun buildDockView() {
        removeAllViews()
        applyStyleAppearance()

        // Top hairline border in full_oem mode
        val topBorder = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1f))
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
        addView(topBorder)

        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(12f), 0, dpToPx(12f), 0)
        }

        buildLeftSection(contentLayout)
        buildGap(contentLayout, 8f)
        buildCenterSection(contentLayout)
        buildGap(contentLayout, 8f)
        buildRightSection(contentLayout)
        buildGap(contentLayout, 8f)
        buildUniMenuSection(contentLayout)

        addView(contentLayout)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        runtime.addListener(this, updateRunnable)
        updateState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runtime.removeListener(this)
        eqAnimator?.cancel()
        discRotationAnimator?.cancel()
    }

    private fun buildGap(parent: LinearLayout, dp: Float) {
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(dp), 1)
        }
        parent.addView(spacer)
    }

    // ==========================================
    // 1. LEFT SECTION (Navigation & System Controls - 320dp)
    // ==========================================
    private fun buildLeftSection(parent: LinearLayout) {
        val leftLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(dpToPx(320f), LinearLayout.LayoutParams.MATCH_PARENT)
        }

        // Back
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_back, "返回") {
            findActivity(context)?.onBackPressed()
        })

        // Home (with active indicator dot and long-press to settings)
        val homeContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            isFocusable = true
            contentDescription = "首页"

            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_dock_home)
                setColorFilter(Color.parseColor("#EFF4F6"))
                layoutParams = FrameLayout.LayoutParams(dpToPx(26f), dpToPx(26f)).apply {
                    gravity = Gravity.CENTER
                }
            }
            addView(icon)

            homeIndicatorDot = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(dpToPx(16f), dpToPx(3f)).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = dpToPx(4f)
                }
                setBackgroundResource(R.drawable.bg_dock_pill_accent)
                visibility = View.VISIBLE
            }
            addView(homeIndicatorDot)

            setOnClickListener {
                findActivity(context)?.navigateToPage(0)
            }
            setOnLongClickListener {
                findActivity(context)?.showDesktopSettings()
                true
            }
        }
        leftLayout.addView(homeContainer)

        // All Apps
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_apps, "全部应用") {
            AllAppsCenterDialog(context).show()
        })

        // Recents (Recent Tasks Switcher)
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_recents, "最近任务") {
            triggerRecents()
        })

        // 360 Camera
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_camera, "360 影像") {
            sendCommand("open-official", "camera")
        })

        parent.addView(leftLayout)
    }

    private fun triggerRecents() {
        try {
            val service = context.getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val toggleRecentApps = statusbarManager.getMethod("toggleRecentApps")
            toggleRecentApps.invoke(service)
            return
        } catch (_: Exception) {}

        try {
            Runtime.getRuntime().exec("input keyevent 187") // KEYCODE_APP_SWITCH
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // 2. CENTER SECTION (Dual Climate Group)
    // ==========================================
    private fun buildCenterSection(parent: LinearLayout) {
        val centerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_pill_badge)
            val padH = dpToPx(10f)
            val padV = dpToPx(4f)
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(50f), 5.0f)
        }

        val showClimateSheet = { ClimateControlBottomSheet(context).show() }

        // 1. A/C Mode Toggle
        acBtn = TextView(context).apply {
            text = "A/C"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#3BE2C0"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(42f), dpToPx(36f))
            setBackgroundResource(R.drawable.bg_dock_pill_accent)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                sendCommand("climate-mode", "ac")
            }
        }
        centerLayout.addView(acBtn)

        buildGap(centerLayout, 4f)

        // 2. Driver Temperature (Click opens sheet)
        driverTempText = TextView(context).apply {
            text = "22.0°"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isSingleLine = true
            maxLines = 1
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.15f)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(driverTempText)

        // 3. DockClimateButton (Fan / AUTO)
        val fanContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(52f), dpToPx(38f))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setOnClickListener { showClimateSheet() }
        }

        fanAutoText = TextView(context).apply {
            text = "AUTO"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }
        fanContainer.addView(fanAutoText)

        fanManualContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

            fanIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_climate_fan)
                setColorFilter(Color.parseColor("#3BE2C0"))
                layoutParams = LinearLayout.LayoutParams(dpToPx(16f), dpToPx(16f))
            }
            addView(fanIcon)

            val segmentsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(6f)).apply {
                    topMargin = dpToPx(2f)
                }
            }

            fanSegmentViews.clear()
            for (i in 1..7) {
                val seg = View(context).apply {
                    rotation = -25f
                    setBackgroundColor(if (i <= 3) Color.parseColor("#3BE2C0") else Color.parseColor("#26FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(dpToPx(2.5f), dpToPx(5f)).apply {
                        if (i < 7) marginEnd = dpToPx(2f)
                    }
                }
                fanSegmentViews.add(seg)
                segmentsRow.addView(seg)
            }
            addView(segmentsRow)
        }
        fanContainer.addView(fanManualContainer)
        centerLayout.addView(fanContainer)

        // 4. Circulation Indicator & Toggle
        recircIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_circulation)
            setColorFilter(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f))
            val p = dpToPx(7f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setOnClickListener {
                sendCommand("climate-mode", "recirc")
            }
        }
        centerLayout.addView(recircIcon)

        // 5. Passenger Temperature (Click opens sheet)
        passengerTempText = TextView(context).apply {
            text = "22.0°"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isSingleLine = true
            maxLines = 1
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.15f)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(passengerTempText)

        buildGap(centerLayout, 4f)

        // 6. Climate Power Button
        climatePowerIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_climate_power)
            setColorFilter(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f))
            val p = dpToPx(7f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setOnClickListener {
                sendCommand("climate-power", "")
            }
        }
        centerLayout.addView(climatePowerIcon)

        parent.addView(centerLayout)
    }

    // ==========================================
    // 3. RIGHT SECTION (Multimedia & Visualizer)
    // ==========================================
    private fun buildRightSection(parent: LinearLayout) {
        val rightFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(50f), 5.2f)
            setBackgroundResource(R.drawable.bg_pill_badge)
        }

        val rightLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            val padH = dpToPx(10f)
            setPadding(padH, 0, padH, 0)
        }

        // Prev
        val prevBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_skip_previous)
            setColorFilter(Color.parseColor("#EFF4F6"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(30f), dpToPx(30f))
            val p = dpToPx(5f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setOnClickListener { sendCommand("previous", "") }
        }
        rightLayout.addView(prevBtn)

        // Play / Pause
        playPauseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_play_arrow)
            setColorFilter(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f)).apply {
                marginStart = dpToPx(4f)
                marginEnd = dpToPx(4f)
            }
            val p = dpToPx(5f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            setOnClickListener { sendCommand("toggle-play", "") }
        }
        rightLayout.addView(playPauseBtn)

        // Next
        val nextBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_skip_next)
            setColorFilter(Color.parseColor("#EFF4F6"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(30f), dpToPx(30f))
            val p = dpToPx(5f)
            setPadding(p, p, p, p)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            setOnClickListener { sendCommand("next", "") }
        }
        rightLayout.addView(nextBtn)

        // Vinyl Disc Artwork
        discImageView = ImageView(context).apply {
            setImageResource(R.drawable.art_vinyl_disc)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34f), dpToPx(34f)).apply {
                marginStart = dpToPx(8f)
                marginEnd = dpToPx(8f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                sendCommand("launch-player", "")
            }
        }
        rightLayout.addView(discImageView)

        // Track & Artist text
        val trackCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(6f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                sendCommand("launch-player", "")
            }
        }

        mediaTitleText = TextView(context).apply {
            text = "向海而行"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            isSelected = true
        }

        mediaArtistText = TextView(context).apply {
            text = "Coastline · 晨间驾驶歌单"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 10f
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        trackCol.addView(mediaTitleText)
        trackCol.addView(mediaArtistText)
        rightLayout.addView(trackCol)

        // Equalizer Bars
        val eqContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(dpToPx(16f), dpToPx(20f)).apply {
                marginEnd = dpToPx(4f)
            }
        }

        val barW = dpToPx(3f)
        eqBar1 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(6f)).apply {
                marginEnd = dpToPx(2f)
            }
        }
        eqBar2 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(12f)).apply {
                marginEnd = dpToPx(2f)
            }
        }
        eqBar3 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(5f))
        }

        eqContainer.addView(eqBar1)
        eqContainer.addView(eqBar2)
        eqContainer.addView(eqBar3)
        rightLayout.addView(eqContainer)

        rightFrame.addView(rightLayout)

        // Underline Progress Bar
        mediaProgressBar = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(2f)).apply {
                gravity = Gravity.BOTTOM
            }
            setBackgroundColor(Color.parseColor("#4D3BE2C0"))
        }
        rightFrame.addView(mediaProgressBar)

        parent.addView(rightFrame)
    }

    // ==========================================
    // 4. FAR-RIGHT SECTION (UNI Menu Button)
    // ==========================================
    private fun buildUniMenuSection(parent: LinearLayout) {
        val uniContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(50f), dpToPx(50f))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_pill_badge)
        }

        val uniIcon = ImageView(context).apply {
            setImageResource(R.drawable.uni_icon)
            layoutParams = FrameLayout.LayoutParams(dpToPx(28f), dpToPx(28f)).apply {
                gravity = Gravity.CENTER
            }
        }
        uniContainer.addView(uniIcon)

        uniContainer.setOnClickListener {
            UniDockMenuPopup(context).showAbove(it)
        }
        uniMenuBtn = uniContainer

        parent.addView(uniContainer)
    }

    private fun setupEqualizerAnimation() {
        eqAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                if (!isCurrentlyPlaying) return@addUpdateListener
                val fraction = anim.animatedFraction
                val h1 = (4 + fraction * 12).toInt()
                val h2 = (16 - fraction * 11).toInt()
                val h3 = (5 + fraction * 13).toInt()
                eqBar1.layoutParams = (eqBar1.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h1.toFloat()) }
                eqBar2.layoutParams = (eqBar2.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h2.toFloat()) }
                eqBar3.layoutParams = (eqBar3.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h3.toFloat()) }
            }
        }
    }

    private fun setupDiscAnimation() {
        discRotationAnimator = ObjectAnimator.ofFloat(discImageView, "rotation", 0f, 360f).apply {
            duration = 10000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun updateState() {
        val snapshot = runtime.snapshot(LauncherRuntime.StaticCursor())
        val isDemo = snapshot.optBoolean("demo", false)

        // 1. Update Climate
        val climate = snapshot.optJSONObject("climate")
        val driverTemp = climate?.optDouble("left", 22.0) ?: 22.0
        val passTemp = climate?.optDouble("right", 22.0) ?: 22.0
        val isAc = climate?.optBoolean("ac", true) ?: true
        val isRecirc = climate?.optBoolean("recirc", true) ?: true
        val isAuto = climate?.optBoolean("auto", true) ?: true
        val isPower = climate?.optBoolean("power", true) ?: true
        val fanSpeed = climate?.optInt("fan", 3) ?: 3

        driverTempText.text = String.format("%.1f°", driverTemp)
        passengerTempText.text = String.format("%.1f°", passTemp)

        // AC Button
        if (isAc) {
            acBtn.setTextColor(Color.parseColor("#3BE2C0"))
            acBtn.setBackgroundResource(R.drawable.bg_dock_pill_accent)
        } else {
            acBtn.setTextColor(Color.parseColor("#7A8B9E"))
            acBtn.setBackgroundResource(R.drawable.bg_dock_item_glass)
        }

        // Fan / Auto
        if (isAuto) {
            fanAutoText.visibility = View.VISIBLE
            fanManualContainer.visibility = View.GONE
        } else {
            fanAutoText.visibility = View.GONE
            fanManualContainer.visibility = View.VISIBLE
            for (i in 0 until fanSegmentViews.size) {
                fanSegmentViews[i].setBackgroundColor(
                    if (i + 1 <= fanSpeed) Color.parseColor("#3BE2C0") else Color.parseColor("#33FFFFFF")
                )
            }
        }

        // Recirc
        recircIcon.setColorFilter(
            if (isRecirc) Color.parseColor("#3BE2C0") else Color.parseColor("#7A8B9E")
        )

        // Power
        climatePowerIcon.setColorFilter(
            if (isPower) Color.parseColor("#3BE2C0") else Color.parseColor("#7A8B9E")
        )

        // 2. Update Media
        val media = snapshot.optJSONObject("media")
        val title = media?.optString("title", "") ?: ""
        val artist = media?.optString("artist", "") ?: ""
        val collection = media?.optString("collection", "") ?: ""
        val playing = media?.optBoolean("playing", false) ?: false

        if (title.isNotEmpty()) {
            mediaTitleText.text = title
            mediaArtistText.text = if (artist.isNotEmpty()) "$artist · $collection".trimEnd(' ', '·') else "正在播放"
        } else {
            mediaTitleText.text = if (isDemo) "向海而行" else "等待媒体播放"
            mediaArtistText.text = if (isDemo) "Coastline · 晨间驾驶歌单" else "蓝牙 / 车机多媒体"
        }

        isCurrentlyPlaying = playing
        if (playing) {
            playPauseBtn.setImageResource(R.drawable.ic_pause)
            if (eqAnimator?.isRunning != true) eqAnimator?.start()
            if (discRotationAnimator?.isStarted != true) discRotationAnimator?.start() else discRotationAnimator?.resume()
        } else {
            playPauseBtn.setImageResource(R.drawable.ic_play_arrow)
            eqAnimator?.cancel()
            discRotationAnimator?.pause()
            eqBar1.layoutParams = (eqBar1.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
            eqBar2.layoutParams = (eqBar2.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
            eqBar3.layoutParams = (eqBar3.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
        }
    }

    private fun sendCommand(action: String, value: String, zone: String = "") {
        try {
            val req = JSONObject().apply {
                put("action", action)
                if (value.isNotEmpty()) put("value", value)
                if (zone.isNotEmpty()) put("zone", zone)
            }
            runtime.handle(req)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDockActionButton(iconRes: Int, contentDesc: String, onClick: () -> Unit): View {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            isFocusable = true
            contentDescription = contentDesc

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                setColorFilter(Color.parseColor("#EFF4F6"))
                layoutParams = FrameLayout.LayoutParams(dpToPx(26f), dpToPx(26f)).apply {
                    gravity = Gravity.CENTER
                }
            }
            addView(icon)
            setOnClickListener { onClick() }
        }
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
