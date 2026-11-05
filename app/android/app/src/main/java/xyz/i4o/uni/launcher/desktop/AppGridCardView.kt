package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Fast App Grid Card with Customization Support.
 * Allows users to tap to launch or long-press to customize any of the 8 app slots.
 */
class AppGridCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subHintView: TextView
    private val gridLayout: GridLayout
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_quick_apps", Context.MODE_PRIVATE)

    private val defaultPkgs = listOf(
        "com.autonavi.amapauto",
        "ecarx.xsf.mediacenter",
        "official_camera",
        "com.ecarx.carsettings",
        "com.tencent.wecarflow",
        "com.android.settings",
        "com.android.documentsui",
        "all_apps"
    )

    init {
        orientation = VERTICAL
        val pad = dpToPx(16f)
        setPadding(pad, pad, pad, pad)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        titleView = TextView(context).apply {
            text = "常用应用"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        subHintView = TextView(context).apply {
            text = "长按格子可自定义"
            setTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 11f
        }

        header.addView(titleView)
        header.addView(subHintView)
        addView(header)

        gridLayout = GridLayout(context).apply {
            columnCount = 4
            rowCount = 2
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                topMargin = dpToPx(6f)
            }
        }
        addView(gridLayout)

        populateApps()
    }

    fun populateApps() {
        gridLayout.removeAllViews()
        val pm = context.packageManager

        for (slot in 0 until 8) {
            val pkg = prefs.getString("slot_$slot", defaultPkgs[slot]) ?: defaultPkgs[slot]
            val (label, iconRes, realIcon) = resolveAppInfo(pm, pkg)

            val appItem = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(4f), dpToPx(6f), dpToPx(4f), dpToPx(6f))
                setBackgroundResource(R.drawable.bg_pill_badge)
                isClickable = true
                isFocusable = true

                val icon = ImageView(context).apply {
                    val iconSize = dpToPx(28f)
                    layoutParams = LayoutParams(iconSize, iconSize)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    if (realIcon != null) {
                        setImageDrawable(realIcon)
                    } else {
                        setImageResource(iconRes)
                    }
                }

                val name = TextView(context).apply {
                    text = label
                    setTextColor(Color.parseColor("#E6FFFFFF"))
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setSingleLine(true)
                    setPadding(0, dpToPx(3f), 0, 0)
                }

                addView(icon)
                addView(name)

                setOnClickListener {
                    launchTarget(pkg)
                }

                setOnLongClickListener {
                    // Open App Picker to customize this slot
                    AllAppsCenterDialog(context) { selected ->
                        prefs.edit().putString("slot_$slot", selected.pkg).apply()
                        populateApps()
                        Toast.makeText(context, "已设置第 ${slot + 1} 格为 ${selected.label}", Toast.LENGTH_SHORT).show()
                    }.show()
                    true
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

    private fun resolveAppInfo(pm: PackageManager, target: String): Triple<String, Int, android.graphics.drawable.Drawable?> {
        return when (target) {
            "official_camera" -> Triple("全景影像", R.drawable.ic_dock_camera, null)
            "all_apps" -> Triple("全部应用", R.drawable.ic_dock_apps, null)
            "xyz.i4o.uni.launcher.DesktopSettings" -> Triple("桌面设置", R.drawable.ic_dock_settings, null)
            else -> {
                try {
                    val info = pm.getApplicationInfo(target, 0)
                    val label = pm.getApplicationLabel(info).toString()
                    val icon = pm.getApplicationIcon(info)
                    Triple(label, 0, icon)
                } catch (e: Exception) {
                    val fallbackName = when (target) {
                        "com.autonavi.amapauto" -> "高德地图"
                        "ecarx.xsf.mediacenter" -> "音乐播放"
                        "com.ecarx.carsettings" -> "车辆设置"
                        "com.android.settings" -> "系统设置"
                        "com.android.documentsui" -> "文件管理"
                        "com.tencent.wecarflow" -> "微信车载"
                        else -> "常用应用"
                    }
                    Triple(fallbackName, R.drawable.ic_dock_apps, null)
                }
            }
        }
    }

    private fun launchTarget(target: String) {
        val rt = LauncherRuntime.get(context)
        when (target) {
            "official_camera" -> {
                rt.apps().launchOfficialCamera(rt.vehicle().cameraCapability())
            }
            "all_apps" -> {
                AllAppsCenterDialog(context).show()
            }
            else -> {
                val ok = rt.apps().launchPackage(target)
                if (!ok) {
                    val intent = context.packageManager.getLaunchIntentForPackage(target)
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "应用未安装或无法启动", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
