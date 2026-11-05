package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime

/**
 * Native music player card displaying song metadata, playback progress,
 * and direct native playback controls (Play/Pause, Prev, Next).
 */
class MediaCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val sourceView: TextView
    private val titleView: TextView
    private val artistView: TextView
    private val progressBar: ProgressBar
    private val prevBtn: Button
    private val playPauseBtn: Button
    private val nextBtn: Button

    private var isPlaying = false

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        // Top source indicator
        sourceView = TextView(context).apply {
            text = "多媒体播放"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
        }
        addView(sourceView)

        // Song Title
        titleView = TextView(context).apply {
            text = "暂无播放"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setSingleLine(true)
            setPadding(0, dpToPx(8f), 0, dpToPx(2f))
        }
        addView(titleView)

        // Artist Name
        artistView = TextView(context).apply {
            text = "点击启动音乐播放器"
            setTextColor(Color.parseColor("#B3FFFFFF"))
            textSize = 14f
            setSingleLine(true)
            setPadding(0, 0, 0, dpToPx(8f))
        }
        addView(artistView)

        // Progress bar
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(4f)).apply {
                bottomMargin = dpToPx(12f)
            }
            max = 1000
            progress = 0
        }
        addView(progressBar)

        // Controls Row
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        prevBtn = createControlButton("⏮") {
            sendCommand("previous")
        }
        playPauseBtn = createControlButton("▶") {
            sendCommand("toggle-play")
        }
        nextBtn = createControlButton("⏭") {
            sendCommand("next")
        }

        controls.addView(prevBtn)
        controls.addView(playPauseBtn)
        controls.addView(nextBtn)
        addView(controls)

        // Clicking card launches media player
        setOnClickListener {
            sendCommand("launch-player")
        }
    }

    private fun createControlButton(text: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
            layoutParams = LayoutParams(0, dpToPx(40f), 1f).apply {
                leftMargin = dpToPx(4f)
                rightMargin = dpToPx(4f)
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun sendCommand(action: String) {
        try {
            val cmd = JSONObject().put("action", action)
            LauncherRuntime.get(context).handle(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateMedia(mediaState: JSONObject) {
        val title = mediaState.optString("title", "").trim()
        val artist = mediaState.optString("artist", "").trim()
        val source = mediaState.optString("sourceLabel", "系统多媒体")
        isPlaying = mediaState.optBoolean("playing", false)

        sourceView.text = source
        titleView.text = if (title.isNotEmpty()) title else "暂无播放"
        artistView.text = if (artist.isNotEmpty()) artist else "未检测到歌曲信息"
        playPauseBtn.text = if (isPlaying) "⏸" else "▶"

        val duration = mediaState.optLong("durationMs", 0)
        val position = mediaState.optLong("positionMs", 0)
        if (duration > 0) {
            progressBar.progress = ((position.toDouble() / duration) * 1000).toInt()
        } else {
            progressBar.progress = 0
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

