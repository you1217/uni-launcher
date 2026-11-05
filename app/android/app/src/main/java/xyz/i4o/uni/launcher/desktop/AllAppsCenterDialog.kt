package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.R
import java.util.*

/**
 * 1:1 Nocturne-styled All-Apps Center Dialog for UNI Launcher.
 * Features real-time search, system/user filter tabs, squircle app icons, and instant launch.
 */
class AllAppsCenterDialog(
    context: Context,
    private val onAddToDesktop: ((AppEntry) -> Unit)? = null
) : Dialog(context) {

    data class AppEntry(
        val pkg: String,
        val label: String,
        val componentName: ComponentName,
        val icon: Drawable,
        val isSystem: Boolean
    )

    private val allApps = mutableListOf<AppEntry>()
    private val filteredApps = mutableListOf<AppEntry>()
    private lateinit var adapter: AppGridAdapter
    private var activeFilter = FilterType.ALL
    private var searchQuery = ""

    enum class FilterType { ALL, USER, SYSTEM }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(880f), dpToPx(560f))
        }

        // 1. Header Bar: Title + Search Box + Filter Tabs + Close
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(context).apply {
            text = "全部应用"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dpToPx(16f), 0)
        }
        header.addView(titleView)

        // Search Input Box
        val searchBox = EditText(context).apply {
            hint = "🔍 搜索车载应用..."
            setHintTextColor(Color.parseColor("#66FFFFFF"))
            setTextColor(Color.WHITE)
            textSize = 13f
            setBackgroundResource(R.drawable.bg_pill_badge)
            val hp = dpToPx(14f)
            val vp = dpToPx(8f)
            setPadding(hp, vp, hp, vp)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(16f)
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString()?.trim() ?: ""
                    applyFilter()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        header.addView(searchBox)

        // Filter Tabs
        val filterGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(16f)
            }
        }

        val tabAll = createFilterTab("全部", FilterType.ALL)
        val tabUser = createFilterTab("常用", FilterType.USER)
        val tabSystem = createFilterTab("系统", FilterType.SYSTEM)

        filterGroup.addView(tabAll)
        filterGroup.addView(tabUser)
        filterGroup.addView(tabSystem)
        header.addView(filterGroup)

        // Close Button
        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#99FFFFFF"))
            textSize = 18f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_pill_badge)
            val p = dpToPx(8f)
            setPadding(p * 2, p, p * 2, p)
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Apps Recycler Grid
        val recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 6)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dpToPx(16f)
            }
        }
        adapter = AppGridAdapter(filteredApps,
            onAppClick = { app ->
                dismiss()
                launchApp(app)
            },
            onAppLongClick = { app ->
                showAppContextMenu(app)
            }
        )
        recyclerView.adapter = adapter
        root.addView(recyclerView)

        setContentView(root)
        loadApps()
    }

    private fun createFilterTab(label: String, type: FilterType): TextView {
        return TextView(context).apply {
            text = label
            textSize = 12f
            val hp = dpToPx(10f)
            val vp = dpToPx(6f)
            setPadding(hp, vp, hp, vp)
            isClickable = true
            isFocusable = true
            updateTabStyle(this, type == activeFilter)
            setOnClickListener {
                activeFilter = type
                applyFilter()
                val parentGroup = parent as? LinearLayout ?: return@setOnClickListener
                for (i in 0 until parentGroup.childCount) {
                    val child = parentGroup.getChildAt(i) as? TextView ?: continue
                    updateTabStyle(child, child == this)
                }
            }
        }
    }

    private fun updateTabStyle(tv: TextView, isSelected: Boolean) {
        if (isSelected) {
            tv.setTextColor(Color.parseColor("#3BE2C0"))
            tv.setBackgroundResource(R.drawable.bg_control_circle_accent)
            tv.typeface = Typeface.DEFAULT_BOLD
        } else {
            tv.setTextColor(Color.parseColor("#B3FFFFFF"))
            tv.setBackgroundResource(R.drawable.bg_pill_badge)
            tv.typeface = Typeface.DEFAULT
        }
    }

    private fun loadApps() {
        allApps.clear()
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = pm.queryIntentActivities(query, 0)
        val selfPkg = context.packageName

        for (resolve in resolves) {
            val pkg = resolve.activityInfo.packageName
            if (pkg == selfPkg && resolve.activityInfo.name != "xyz.i4o.uni.launcher.DesktopSettings") {
                continue
            }
            val label = try {
                resolve.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }
            val icon = try {
                resolve.loadIcon(pm)
            } catch (e: Exception) {
                context.getDrawable(R.drawable.ic_dock_apps)!!
            }
            val isSys = (resolve.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val entry = AppEntry(
                pkg = pkg,
                label = label,
                componentName = ComponentName(pkg, resolve.activityInfo.name),
                icon = icon,
                isSystem = isSys
            )
            allApps.add(entry)
        }

        allApps.sortBy { it.label.lowercase(Locale.getDefault()) }
        applyFilter()
    }

    private fun applyFilter() {
        filteredApps.clear()
        for (app in allApps) {
            // 1. Filter by category
            val matchesCategory = when (activeFilter) {
                FilterType.ALL -> true
                FilterType.USER -> !app.isSystem
                FilterType.SYSTEM -> app.isSystem
            }
            if (!matchesCategory) continue

            // 2. Filter by search query
            if (searchQuery.isNotEmpty()) {
                val matchesQuery = app.label.contains(searchQuery, ignoreCase = true) ||
                    app.pkg.contains(searchQuery, ignoreCase = true)
                if (!matchesQuery) continue
            }

            filteredApps.add(app)
        }
        adapter.notifyDataSetChanged()
    }

    private fun launchApp(app: AppEntry) {
        val rt = LauncherRuntime.get(context)
        val launched = rt.apps().launchPackage(app.pkg)
        if (!launched) {
            val intent = context.packageManager.getLaunchIntentForPackage(app.pkg)
            if (intent != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "无法启动应用: ${app.label}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAppContextMenu(app: AppEntry) {
        val menu = PopupMenu(context, null)
        menu.menu.add(0, 1, 0, "➕ 添加到桌面卡片")
        menu.menu.add(0, 2, 1, "ℹ️ 应用详情")

        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    onAddToDesktop?.invoke(app)
                    dismiss()
                    Toast.makeText(context, "已将 ${app.label} 添加至桌面", Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", app.pkg, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "无法打开应用信息", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        // Native context dialog
        val d = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val box = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_glass)
                val p = dpToPx(16f)
                setPadding(p, p, p, p)

                val t = TextView(context).apply {
                    text = app.label
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, 0, 0, dpToPx(12f))
                }
                addView(t)

                val b1 = TextView(context).apply {
                    text = "➕ 添加到常用应用"
                    setTextColor(Color.parseColor("#3BE2C0"))
                    textSize = 14f
                    setBackgroundResource(R.drawable.bg_pill_badge)
                    val hp = dpToPx(14f)
                    val vp = dpToPx(10f)
                    setPadding(hp, vp, hp, vp)
                    setOnClickListener {
                        dismiss()
                        this@AllAppsCenterDialog.dismiss()
                        onAddToDesktop?.invoke(app)
                        Toast.makeText(context, "已添加 ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                }
                val b2 = TextView(context).apply {
                    text = "ℹ️ 应用系统详情"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setBackgroundResource(R.drawable.bg_pill_badge)
                    val hp = dpToPx(14f)
                    val vp = dpToPx(10f)
                    setPadding(hp, vp, hp, vp)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(8f)
                    }
                    setOnClickListener {
                        dismiss()
                        this@AllAppsCenterDialog.dismiss()
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开应用信息", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                addView(b1)
                addView(b2)
            }
            setContentView(box)
        }
        d.show()
    }

    private inner class AppGridAdapter(
        private val list: List<AppEntry>,
        private val onAppClick: (AppEntry) -> Unit,
        private val onAppLongClick: (AppEntry) -> Unit
    ) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

        inner class AppViewHolder(val root: LinearLayout, val icon: ImageView, val label: TextView) :
            RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val itemLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val pad = dpToPx(8f)
                setPadding(pad, pad, pad, pad)
                setBackgroundResource(R.drawable.bg_pill_badge)
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    val m = dpToPx(6f)
                    setMargins(m, m, m, m)
                }
            }

            val iconView = ImageView(context).apply {
                val size = dpToPx(52f)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                clipToOutline = true
            }

            val labelView = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(0, dpToPx(6f), 0, 0)
            }

            itemLayout.addView(iconView)
            itemLayout.addView(labelView)
            return AppViewHolder(itemLayout, iconView, labelView)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = list[position]
            holder.icon.setImageDrawable(app.icon)
            holder.label.text = app.label
            holder.root.setOnClickListener { onAppClick(app) }
            holder.root.setOnLongClickListener {
                onAppLongClick(app)
                true
            }
        }

        override fun getItemCount(): Int = list.size
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

