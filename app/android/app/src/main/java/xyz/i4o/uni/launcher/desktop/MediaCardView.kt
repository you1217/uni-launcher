package xyz.i4o.uni.launcher.desktop

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Base64
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Visual reproduction of UNI Launcher's Vinyl Media Player Card.
 *
 * Features:
 * - Left-side rotating vinyl disc with album artwork cover
 * - Smooth continuous hardware rotation while playing
 * - Source tag pill badge
 * - Marquee song title, artist label, custom progress bar
 * - Modern circular playback action buttons (Prev, Play/Pause, Next)
 */
class MediaCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val discView: ImageView
    private val discRotateAnimator: ObjectAnimator
    private val sourceBadge: TextView
    private val titleView: TextView
    private val artistView: TextView
    private val progressBar: ProgressBar
    private val timeView: TextView
    private val playPauseBtn: ImageButton
    private val prevBtn: ImageButton
    private val nextBtn: ImageButton

    private var isPlaying = false
    private var lastArtworkBase64 = ""

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // 1. Left Vinyl Disc Container
        val discContainer = FrameLayout(context).apply {
            val discSize = dpToPx(110f)
            layoutParams = LayoutParams(discSize, discSize).apply {
                marginEnd = dpToPx(16f)
            }
        }

        discView = ImageView(context).apply {
            setImageResource(R.drawable.art_vinyl_disc)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        discContainer.addView(discView)

        // Vinyl rotation animator (20s per full rotation)
        discRotateAnimator = ObjectAnimator.ofFloat(discView, "rotation", 0f, 360f).apply {
            duration = 20000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        addView(discContainer)

        // 2. Right Track Info & Controls
        val infoLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Top row: Source badge
        sourceBadge = TextView(context).apply {
            text = "多媒体"
            setTextColor(Color.parseColor("#3BE2C0")) // Nocturne Mint
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            setPadding(dpToPx(8f), dpToPx(3f), dpToPx(8f), dpToPx(3f))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        infoLayout.addView(sourceBadge)

        // Song Title (Marquee)
        titleView = TextView(context).apply {
            text = "暂无播放"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSelected = true
            setPadding(0, dpToPx(4f), 0, dpToPx(2f))
        }
        infoLayout.addView(titleView)

        // Artist Name
        artistView = TextView(context).apply {
            text = "点击启动音乐播放器"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            setSingleLine(true)
            setPadding(0, 0, 0, dpToPx(4f))
        }
        infoLayout.addView(artistView)

        // Progress bar + Time
        val progressRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(0, dpToPx(4f), 1f).apply {
                marginEnd = dpToPx(8f)
            }
            progressDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.progress_bar_mint)
            max = 1000
            progress = 0
        }
        timeView = TextView(context).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 10f
        }
        progressRow.addView(progressBar)
        progressRow.addView(timeView)
        infoLayout.addView(progressRow)

        // 3. Playback Controls Row
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(6f), 0, 0)
        }

        prevBtn = ImageButton(context).apply {
            setImageResource(R.drawable.ic_skip_previous)
            setBackgroundResource(R.drawable.bg_control_circle)
            val btnSize = dpToPx(36f)
            layoutParams = LayoutParams(btnSize, btnSize).apply {
                marginEnd = dpToPx(12f)
            }
            setOnClickListener { sendAction("previous") }
        }

        playPauseBtn = ImageButton(context).apply {
            setImageResource(R.drawable.ic_play_arrow)
            setBackgroundResource(R.drawable.bg_control_circle_accent)
            val btnSize = dpToPx(44f)
            layoutParams = LayoutParams(btnSize, btnSize).apply {
                marginEnd = dpToPx(12f)
            }
            setOnClickListener { sendAction("toggle-play") }
        }

        nextBtn = ImageButton(context).apply {
            setImageResource(R.drawable.ic_skip_next)
            setBackgroundResource(R.drawable.bg_control_circle)
            val btnSize = dpToPx(36f)
            layoutParams = LayoutParams(btnSize, btnSize)
            setOnClickListener { sendAction("next") }
        }

        controls.addView(prevBtn)
        controls.addView(playPauseBtn)
        controls.addView(nextBtn)
        infoLayout.addView(controls)

        addView(infoLayout)

        setOnClickListener {
            sendAction("launch-player")
        }
    }

    private fun sendAction(action: String) {
        try {
            LauncherRuntime.get(context).handle(JSONObject().put("action", action))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateMedia(mediaState: JSONObject) {
        val title = mediaState.optString("title", "").trim()
        val artist = mediaState.optString("artist", "").trim()
        val source = mediaState.optString("sourceLabel", "系统多媒体")
        val playing = mediaState.optBoolean("playing", false)

        sourceBadge.text = source
        titleView.text = if (title.isNotEmpty()) title else "暂无播放"
        artistView.text = if (artist.isNotEmpty()) artist else "未检测到歌曲信息"

        if (playing != isPlaying) {
            isPlaying = playing
            if (playing) {
                playPauseBtn.setImageResource(R.drawable.ic_pause)
                if (discRotateAnimator.isPaused) discRotateAnimator.resume()
                else if (!discRotateAnimator.isStarted) discRotateAnimator.start()
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_play_arrow)
                if (discRotateAnimator.isRunning) discRotateAnimator.pause()
            }
        }

        // Handle artwork bitmap if present
        val artworkBase64 = mediaState.optString("artwork", "")
        if (artworkBase64.isNotEmpty() && artworkBase64 != lastArtworkBase64) {
            lastArtworkBase64 = artworkBase64
            try {
                val bytes = Base64.decode(artworkBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    discView.setImageBitmap(bmp)
                }
            } catch (ignored: Exception) {}
        } else if (artworkBase64.isEmpty() && lastArtworkBase64.isNotEmpty()) {
            lastArtworkBase64 = ""
            discView.setImageResource(R.drawable.art_vinyl_disc)
        }

        val duration = mediaState.optLong("durationMs", 0)
        val position = mediaState.optLong("positionMs", 0)
        if (duration > 0) {
            progressBar.progress = ((position.toDouble() / duration) * 1000).toInt()
            val curMin = (position / 60000).toInt()
            val curSec = ((position % 60000) / 1000).toInt()
            val durMin = (duration / 60000).toInt()
            val durSec = ((duration % 60000) / 1000).toInt()
            timeView.text = "%02d:%02d / %02d:%02d".format(curMin, curSec, durMin, durSec)
        } else {
            progressBar.progress = 0
            timeView.text = "00:00"
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
