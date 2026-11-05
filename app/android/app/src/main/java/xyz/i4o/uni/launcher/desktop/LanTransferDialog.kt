package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 In-Car Wireless LAN Transfer Dialog.
 * Powers high-speed cable-free file upload (music, APKs, wallpapers) between phone and head-unit via HTTP on port 8787.
 */
class LanTransferDialog(context: Context) : Dialog(context) {

    private val runtime = LauncherRuntime.get(context)
    private val transfer = runtime.transfer
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var statusSwitch: Switch
    private lateinit var qrContainer: LinearLayout
    private lateinit var qrImageView: ImageView
    private lateinit var urlTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var filesContainer: LinearLayout
    private lateinit var emptyNoticeView: TextView

    private val refreshRunnable = Runnable { refreshState() }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(780f), dpToPx(620f))
        }

        // 1. Header (Title, Subtitle, Close)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            text = "📲 手机无线快传"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }

        val subTitleView = TextView(context).apply {
            text = "免数据线高速传输 · 支持无损音乐、壁纸与 APK 安装包"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 13f
            setPadding(0, dpToPx(3f), 0, 0)
        }

        titleCol.addView(titleView)
        titleCol.addView(subTitleView)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 18f
            setPadding(dpToPx(12f), dpToPx(6f), dpToPx(12f), dpToPx(6f))
            setBackgroundResource(R.drawable.bg_pill_badge)
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Control Switch Row
        val switchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(14f)
            setPadding(p, dpToPx(10f), p, dpToPx(10f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
                bottomMargin = dpToPx(12f)
            }
        }

        val switchLabelCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val switchTitle = TextView(context).apply {
            text = "无线快传传输服务"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }

        hintTextView = TextView(context).apply {
            text = "手机与车机连接同一 Wi-Fi 或打开手机热点"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 12f
            setPadding(0, dpToPx(2f), 0, 0)
        }

        switchLabelCol.addView(switchTitle)
        switchLabelCol.addView(hintTextView)
        switchRow.addView(switchLabelCol)

        statusSwitch = Switch(context).apply {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (isChecked) {
                    val res = transfer.start()
                    runtime.publish()
                    refreshState()
                } else {
                    transfer.stop()
                    runtime.publish()
                    refreshState()
                }
            }
        }
        switchRow.addView(statusSwitch)
        root.addView(switchRow)

        // 3. Middle Body: Two-column layout (Left: QR Code + URL, Right: File List)
        val bodyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // Left: QR container
        qrContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(dpToPx(260f), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dpToPx(16f)
            }
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(16f)
            setPadding(p, p, p, p)
        }

        val qrWhiteCard = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(dpToPx(180f), dpToPx(180f))
        }

        qrImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        qrWhiteCard.addView(qrImageView)
        qrContainer.addView(qrWhiteCard)

        urlTextView = TextView(context).apply {
            text = "http://192.168.x.x:8787"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(10f), 0, dpToPx(4f))
            setTextIsSelectable(true)
        }
        qrContainer.addView(urlTextView)

        val qrTip = TextView(context).apply {
            text = "微信或浏览器扫码直接上传"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 11f
            gravity = Gravity.CENTER
        }
        qrContainer.addView(qrTip)
        bodyLayout.addView(qrContainer)

        // Right: File List Area
        val rightCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
        }

        val listHeader = TextView(context).apply {
            text = "📁 已接收文件与传输队列"
            setTextColor(Color.parseColor("#64B5F6"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(8f))
        }
        rightCol.addView(listHeader)

        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        filesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        emptyNoticeView = TextView(context).apply {
            text = "暂无文件\n手机扫码后在手机网页选择文件即可高速上传"
            setTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(60f), 0, dpToPx(60f))
        }
        filesContainer.addView(emptyNoticeView)
        scrollView.addView(filesContainer)
        rightCol.addView(scrollView)

        bodyLayout.addView(rightCol)
        root.addView(bodyLayout)

        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        // Automatically start transfer server if not running
        val snapshot = transfer.snapshot()
        val isRunning = snapshot.optBoolean("running", false)
        statusSwitch.isChecked = isRunning
        refreshState()
    }

    override fun onStop() {
        super.onStop()
        mainHandler.removeCallbacks(refreshRunnable)
    }

    private fun refreshState() {
        val snapshot = transfer.snapshot()
        val isRunning = snapshot.optBoolean("running", false)
        val url = snapshot.optString("url", "")
        val mode = snapshot.optString("mode", "")

        statusSwitch.isChecked = isRunning

        if (isRunning && url.isNotEmpty()) {
            qrContainer.visibility = View.VISIBLE
            urlTextView.text = url
            hintTextView.text = "局域网服务运行中 · 网络状态: $mode"
            val qrBitmap = generateQrBitmap(url, dpToPx(200f))
            if (qrBitmap != null) {
                qrImageView.setImageBitmap(qrBitmap)
            }
        } else {
            qrContainer.visibility = View.GONE
            hintTextView.text = if (isRunning) "未获取到有效局域网 IP，请检查车机 Wi-Fi/热点" else "传输服务已关闭"
        }

        // Render files list
        val files = snapshot.optJSONArray("files") ?: JSONArray()
        val links = snapshot.optJSONArray("links") ?: JSONArray()

        filesContainer.removeAllViews()

        val totalItems = files.length() + links.length()
        if (totalItems == 0) {
            filesContainer.addView(emptyNoticeView)
        } else {
            // Render links
            for (i in 0 until links.length()) {
                val link = links.optJSONObject(i) ?: continue
                val linkId = link.optString("id", "")
                val name = link.optString("name", link.optString("host", "网络链接"))
                val status = link.optString("status", "queued")
                val isDownloading = (status == "downloading")

                val row = createFileRow(
                    name = name,
                    tag = "链接任务",
                    tagColor = "#FFB25C",
                    info = if (isDownloading) "正在下载到车机..." else "待下载",
                    actionPrimaryLabel = if (isDownloading) "下载中" else "下载",
                    onPrimaryAction = if (isDownloading) null else {
                        {
                            transfer.downloadLink(linkId)
                            runtime.publish()
                            mainHandler.postDelayed({ refreshState() }, 500)
                        }
                    },
                    actionSecondaryLabel = "删除",
                    onSecondaryAction = {
                        transfer.removeLink(linkId)
                        runtime.publish()
                        refreshState()
                    }
                )
                filesContainer.addView(row)
            }

            // Render uploaded files
            for (i in 0 until files.length()) {
                val file = files.optJSONObject(i) ?: continue
                val fileName = file.optString("name", "")
                val isApk = fileName.lowercase().endsWith(".apk")

                val row = createFileRow(
                    name = fileName,
                    tag = if (isApk) "安装包" else "媒体文件",
                    tagColor = if (isApk) "#3BE2C0" else "#64B5F6",
                    info = "已上传",
                    actionPrimaryLabel = if (isApk) "安装" else null,
                    onPrimaryAction = if (isApk) {
                        {
                            val ok = transfer.install(fileName)
                            if (!ok) Toast.makeText(context, "未找到可安装文件或系统受限", Toast.LENGTH_SHORT).show()
                        }
                    } else null,
                    actionSecondaryLabel = "删除",
                    onSecondaryAction = {
                        transfer.delete(fileName)
                        runtime.publish()
                        refreshState()
                    }
                )
                filesContainer.addView(row)
            }
        }
    }

    private fun createFileRow(
        name: String,
        tag: String,
        tagColor: String,
        info: String,
        actionPrimaryLabel: String?,
        onPrimaryAction: (() -> Unit)?,
        actionSecondaryLabel: String,
        onSecondaryAction: () -> Unit
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_control_circle)
            val p = dpToPx(8f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(6f)
            }
        }

        val tagView = TextView(context).apply {
            text = tag
            setTextColor(Color.parseColor(tagColor))
            textSize = 10f
            setBackgroundResource(R.drawable.bg_pill_badge)
            setPadding(dpToPx(6f), dpToPx(2f), dpToPx(6f), dpToPx(2f))
        }
        row.addView(tagView)

        val nameCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(8f)
                marginEnd = dpToPx(8f)
            }
        }

        val nameText = TextView(context).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val infoText = TextView(context).apply {
            text = info
            setTextColor(Color.parseColor("#80FFFFFF"))
            textSize = 11f
        }

        nameCol.addView(nameText)
        nameCol.addView(infoText)
        row.addView(nameCol)

        if (actionPrimaryLabel != null) {
            val primaryBtn = TextView(context).apply {
                text = actionPrimaryLabel
                setTextColor(Color.parseColor("#3BE2C0"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_pill_badge)
                setPadding(dpToPx(12f), dpToPx(6f), dpToPx(12f), dpToPx(6f))
                isClickable = true
                isFocusable = true
                setOnClickListener { onPrimaryAction?.invoke() }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(6f)
                }
            }
            row.addView(primaryBtn)
        }

        val secondaryBtn = TextView(context).apply {
            text = actionSecondaryLabel
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 12f
            setBackgroundResource(R.drawable.bg_pill_badge)
            setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            isClickable = true
            isFocusable = true
            setOnClickListener { onSecondaryAction() }
        }
        row.addView(secondaryBtn)

        return row
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    pixels[y * sizePx + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

