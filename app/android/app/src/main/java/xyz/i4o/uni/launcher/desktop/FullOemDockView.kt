package xyz.i4o.uni.launcher.desktop

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.MainActivity
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Full-Width OEM Dock Bar for Lynk & Co 05.
 * Features:
 * 1. Left System Controls: Back, Home, All Apps, 360 Camera.
 * 2. Center Dual-Zone Climate: Driver/Passenger Temp (-/+), A/C, Recirc, AUTO, Sheet Trigger.
 * 3. Right Multimedia: Album Disc, Track/Artist marquee, Prev/Play/Next, Animated Equalizer Bars.
 */
class FullOemDockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val runtime = LauncherRuntime.get(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Center Climate UI
    private lateinit var driverTempText: TextView
    private lateinit var passengerTempText: TextView
    private lateinit var acBtn: TextView
    private lateinit var recircBtn: TextView
    private lateinit var autoBtn: TextView

    // Right Media UI
    private lateinit var mediaTitleText: TextView
    private lateinit var mediaArtistText: TextView
    private lateinit var playPauseBtn: ImageView
    private lateinit var eqBar1: View
    private lateinit var eqBar2: View
    private lateinit var eqBar3: View

    private var eqAnimator: ValueAnimator? = null
    private var isCurrentlyPlaying = false

    private val updateRunnable = Runnable {
        updateState()
    }

    init {
        // Full width container with dark-glass background and subtle top border
        setBackgroundColor(Color.parseColor("#F2090C12"))

        // Top border line
        val topBorder = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1f))
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
        addView(topBorder)

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
        }

        buildLeftSection(contentLayout)
        buildCenterSection(contentLayout)
        buildRightSection(contentLayout)

        addView(contentLayout)
        setupEqualizerAnimation()
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
    }

    // ==========================================
    // 1. LEFT SECTION (Navigation & Apps)
    // ==========================================
    private fun buildLeftSection(parent: LinearLayout) {
        val leftLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2.2f)
        }

        // Back
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_back, "返回") {
            (context as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
        })

        // Home
        val homeBtn = createDockActionButton(R.drawable.ic_dock_home, "首页") {
            (context as? MainActivity)?.navigateToPage(0)
        }
        homeBtn.setOnLongClickListener {
            (context as? MainActivity)?.showDesktopSettings()
            true
        }
        leftLayout.addView(homeBtn)

        // All Apps
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_apps, "全部应用") {
            AllAppsCenterDialog(context).show()
        })

        // 360 Camera
        leftLayout.addView(createDockActionButton(R.drawable.ic_dock_camera, "360全景") {
            sendCommand("open-official", "camera")
        })

        parent.addView(leftLayout)
    }

    // ==========================================
    // 2. CENTER SECTION (Dual Climate Control)
    // ==========================================
    private fun buildCenterSection(parent: LinearLayout) {
        val centerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            val padH = dpToPx(8f)
            val padV = dpToPx(4f)
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 5.0f).apply {
                marginStart = dpToPx(6f)
                marginEnd = dpToPx(6f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                ClimateControlBottomSheet(context).show()
            }
        }

        // --- Driver Zone ---
        val showClimateSheet = { ClimateControlBottomSheet(context).show() }

        val driverLabel = TextView(context).apply {
            text = "主驾"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 10f
            setPadding(0, 0, dpToPx(4f), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(driverLabel)

        val driverMinus = createClimateCircleBtn("-") {
            sendCommand("temp-down", "", zone = "left")
        }
        centerLayout.addView(driverMinus)

        driverTempText = TextView(context).apply {
            text = "22.0°"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(6f), 0, dpToPx(6f), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(driverTempText)

        val driverPlus = createClimateCircleBtn("+") {
            sendCommand("temp-up", "", zone = "left")
        }
        centerLayout.addView(driverPlus)

        // --- Center Indicators ---
        val divider1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1f), dpToPx(16f)).apply {
                marginStart = dpToPx(6f)
                marginEnd = dpToPx(6f)
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        centerLayout.addView(divider1)

        autoBtn = createClimateModeBadge("AUTO") {
            sendCommand("climate-mode", "auto")
        }
        centerLayout.addView(autoBtn)

        acBtn = createClimateModeBadge("A/C") {
            sendCommand("climate-mode", "ac")
        }
        centerLayout.addView(acBtn)

        recircBtn = createClimateModeBadge("内循环") {
            sendCommand("climate-mode", "recirc")
        }
        centerLayout.addView(recircBtn)

        val divider2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1f), dpToPx(16f)).apply {
                marginStart = dpToPx(6f)
                marginEnd = dpToPx(6f)
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        centerLayout.addView(divider2)

        // --- Passenger Zone ---
        val passLabel = TextView(context).apply {
            text = "副驾"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 10f
            setPadding(0, 0, dpToPx(4f), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(passLabel)

        val passMinus = createClimateCircleBtn("-") {
            sendCommand("temp-down", "", zone = "right")
        }
        centerLayout.addView(passMinus)

        passengerTempText = TextView(context).apply {
            text = "22.0°"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(6f), 0, dpToPx(6f), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { showClimateSheet() }
        }
        centerLayout.addView(passengerTempText)

        val passPlus = createClimateCircleBtn("+") {
            sendCommand("temp-up", "", zone = "right")
        }
        centerLayout.addView(passPlus)

        parent.addView(centerLayout)
    }

    // ==========================================
    // 3. RIGHT SECTION (Multimedia & Visualizer)
    // ==========================================
    private fun buildRightSection(parent: LinearLayout) {
        val rightLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3.2f)
        }

        // Album cover / Vinyl Disc
        val discIcon = ImageView(context).apply {
            setImageResource(R.drawable.art_vinyl_disc)
            layoutParams = LinearLayout.LayoutParams(dpToPx(38f), dpToPx(38f)).apply {
                marginEnd = dpToPx(8f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                sendCommand("toggle-play", "")
            }
        }
        rightLayout.addView(discIcon)

        // Track & Artist text
        val trackCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(8f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                sendCommand("toggle-play", "")
            }
        }

        mediaTitleText = TextView(context).apply {
            text = "等待媒体播放"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            isSelected = true
        }

        mediaArtistText = TextView(context).apply {
            text = "蓝牙 / 车机多媒体"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        trackCol.addView(mediaTitleText)
        trackCol.addView(mediaArtistText)
        rightLayout.addView(trackCol)

        // Transport Controls (Prev, Play/Pause, Next)
        val prevBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_skip_previous)
            layoutParams = LinearLayout.LayoutParams(dpToPx(32f), dpToPx(32f))
            setPadding(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle)
            setOnClickListener { sendCommand("previous", "") }
        }
        rightLayout.addView(prevBtn)

        playPauseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_play_arrow)
            layoutParams = LinearLayout.LayoutParams(dpToPx(38f), dpToPx(38f)).apply {
                marginStart = dpToPx(6f)
                marginEnd = dpToPx(6f)
            }
            setPadding(dpToPx(6f), dpToPx(6f), dpToPx(6f), dpToPx(6f))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            setOnClickListener { sendCommand("toggle-play", "") }
        }
        rightLayout.addView(playPauseBtn)

        val nextBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_skip_next)
            layoutParams = LinearLayout.LayoutParams(dpToPx(32f), dpToPx(32f)).apply {
                marginEnd = dpToPx(8f)
            }
            setPadding(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle)
            setOnClickListener { sendCommand("next", "") }
        }
        rightLayout.addView(nextBtn)

        // Equalizer Bars
        val eqContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(dpToPx(16f), dpToPx(24f))
        }

        val barW = dpToPx(3f)
        eqBar1 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(8f)).apply {
                marginEnd = dpToPx(2f)
            }
        }
        eqBar2 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(14f)).apply {
                marginEnd = dpToPx(2f)
            }
        }
        eqBar3 = View(context).apply {
            setBackgroundColor(Color.parseColor("#3BE2C0"))
            layoutParams = LinearLayout.LayoutParams(barW, dpToPx(6f))
        }

        eqContainer.addView(eqBar1)
        eqContainer.addView(eqBar2)
        eqContainer.addView(eqBar3)
        rightLayout.addView(eqContainer)

        parent.addView(rightLayout)
    }

    private fun setupEqualizerAnimation() {
        eqAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { anim ->
                if (!isCurrentlyPlaying) return@addUpdateListener
                val fraction = anim.animatedFraction
                val h1 = (4 + fraction * 14).toInt()
                val h2 = (16 - fraction * 10).toInt()
                val h3 = (6 + fraction * 12).toInt()
                eqBar1.layoutParams = (eqBar1.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h1.toFloat()) }
                eqBar2.layoutParams = (eqBar2.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h2.toFloat()) }
                eqBar3.layoutParams = (eqBar3.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(h3.toFloat()) }
            }
        }
    }

    private fun updateState() {
        // 1. Update Climate
        val vehicleSnapshot = runtime.vehicle?.snapshot()
        val climate = vehicleSnapshot?.optJSONObject("climate")

        val driverTemp = climate?.optDouble("left", 22.0) ?: 22.0
        val passTemp = climate?.optDouble("right", 22.0) ?: 22.0
        val isAc = climate?.optBoolean("ac", true) ?: true
        val isRecirc = climate?.optBoolean("recirc", true) ?: true
        val isAuto = climate?.optBoolean("auto", true) ?: true

        driverTempText.text = String.format("%.1f°", driverTemp)
        passengerTempText.text = String.format("%.1f°", passTemp)

        updateBadgeState(acBtn, isAc)
        updateBadgeState(recircBtn, isRecirc)
        updateBadgeState(autoBtn, isAuto)

        // 2. Update Media
        val mediaSnapshot = runtime.media?.snapshot()
        val title = mediaSnapshot?.optString("title", "") ?: ""
        val artist = mediaSnapshot?.optString("artist", "") ?: ""
        val playing = mediaSnapshot?.optBoolean("playing", false) ?: false

        if (title.isNotEmpty()) {
            mediaTitleText.text = title
            mediaArtistText.text = if (artist.isNotEmpty()) artist else "正在播放"
        } else {
            mediaTitleText.text = "等待媒体播放"
            mediaArtistText.text = "蓝牙 / 车机多媒体"
        }

        isCurrentlyPlaying = playing
        if (playing) {
            playPauseBtn.setImageResource(R.drawable.ic_pause)
            if (eqAnimator?.isRunning != true) {
                eqAnimator?.start()
            }
        } else {
            playPauseBtn.setImageResource(R.drawable.ic_play_arrow)
            eqAnimator?.cancel()
            // Reset bars
            eqBar1.layoutParams = (eqBar1.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
            eqBar2.layoutParams = (eqBar2.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
            eqBar3.layoutParams = (eqBar3.layoutParams as LinearLayout.LayoutParams).apply { height = dpToPx(4f) }
        }
    }

    private fun updateBadgeState(badge: TextView, active: Boolean) {
        if (active) {
            badge.setTextColor(Color.parseColor("#3BE2C0"))
            badge.setBackgroundResource(R.drawable.bg_control_circle_accent)
        } else {
            badge.setTextColor(Color.parseColor("#99FFFFFF"))
            badge.setBackgroundResource(R.drawable.bg_pill_badge)
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

    private fun createDockActionButton(iconRes: Int, labelText: String, onClick: () -> Unit): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_control_circle)

            val icon = ImageView(context).apply {
                setImageResource(iconRes)
                layoutParams = LinearLayout.LayoutParams(dpToPx(22f), dpToPx(22f))
            }

            val label = TextView(context).apply {
                text = labelText
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(0, dpToPx(2f), 0, 0)
            }

            addView(icon)
            addView(label)
            setOnClickListener { onClick() }
        }
    }

    private fun createClimateCircleBtn(label: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(26f), dpToPx(26f))
            setBackgroundResource(R.drawable.bg_control_circle)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun createClimateModeBadge(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(26f)
            ).apply {
                marginStart = dpToPx(3f)
                marginEnd = dpToPx(3f)
            }
            setPadding(dpToPx(6f), 0, dpToPx(6f), 0)
            setBackgroundResource(R.drawable.bg_pill_badge)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

