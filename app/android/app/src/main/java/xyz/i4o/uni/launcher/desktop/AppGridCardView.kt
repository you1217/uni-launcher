package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime

/**
 * App grid card displaying installed applications with instant launch capability.
 */
class AppGridCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val gridLayout: GridLayout

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        titleView = TextView(context).apply {
            text = "常用应用"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dpToPx(10f))
        }
        addView(titleView)

        gridLayout = GridLayout(context).apply {
            columnCount = 4
            rowCount = 2
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(gridLayout)

        populateDefaultApps()
    }

    private fun populateDefaultApps() {
        val defaultApps = listOf(
            Triple("高德地图", "com.autonavi.amapauto", android.R.drawable.ic_dialog_map),
            Triple("音乐播放", "ecarx.xsf.mediacenter", android.R.drawable.ic_media_play),
            Triple("全景影像", "official_camera", android.R.drawable.ic_menu_camera),
            Triple("车辆设置", "com.ecarx.carsettings", android.R.drawable.ic_menu_preferences),
            Triple("应用中心", "official_market", android.R.drawable.ic_menu_agenda),
            Triple("系统设置", "com.android.settings", android.R.drawable.ic_menu_manage),
            Triple("文件管理", "com.android.documentsui", android.R.drawable.ic_menu_save),
            Triple("桌面设置", "xyz.i4o.uni.launcher.DesktopSettings", android.R.drawable.ic_menu_info_details)
        )

        gridLayout.removeAllViews()
        for ((label, pkg, iconRes) in defaultApps) {
            val appItem = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(4f), dpToPx(6f), dpToPx(4f), dpToPx(6f))
                setBackgroundColor(Color.parseColor("#12FFFFFF"))

                val icon = ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(Color.WHITE)
                    val iconSize = dpToPx(28f)
                    layoutParams = LayoutParams(iconSize, iconSize)
                }
                val name = TextView(context).apply {
                    text = label
                    setTextColor(Color.parseColor("#E6FFFFFF"))
                    textSize = 11f
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    setPadding(0, dpToPx(4f), 0, 0)
                }

                addView(icon)
                addView(name)

                setOnClickListener {
                    launchTarget(pkg)
                }
            }

            val param = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            }
            gridLayout.addView(appItem, param)
        }
    }

    private fun launchTarget(target: String) {
        val rt = LauncherRuntime.get(context)
        when (target) {
            "official_camera" -> {
                rt.apps().launchOfficialCamera(rt.vehicle().cameraCapability())
            }
            "official_market" -> {
                rt.apps().launchPackage("com.ecarx.membercenter")
            }
            else -> {
                rt.apps().launchPackage(target)
            }
        }
    }

    fun updateApps(apps: JSONArray) {
        // Can be dynamically customized from user settings
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

