package xyz.i4o.uni.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject
import xyz.i4o.uni.launcher.desktop.CardLibraryDialog
import xyz.i4o.uni.launcher.desktop.DesktopLayoutManager
import xyz.i4o.uni.launcher.desktop.DesktopSettingsDialog
import xyz.i4o.uni.launcher.desktop.WorkspacePagerAdapter

/**
 * Native Android View + RenderThread Launcher Activity for Lynk & Co 05.
 *
 * Direct hardware RenderNode rendering, 16ms touch responsiveness,
 * and full modular freedom (Card Library, Add/Delete, Dynamic savedDesign).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var runtime: LauncherRuntime
    private lateinit var layoutManager: DesktopLayoutManager
    private lateinit var viewPager: ViewPager2
    private lateinit var workspaceAdapter: WorkspacePagerAdapter
    private lateinit var topWeatherText: TextView
    private lateinit var topSystemStatus: TextView
    private lateinit var wallpaperImageView: ImageView
    private var currentWallpaper: String = "graphite_night"

    private val cursor = LauncherRuntime.StaticCursor()

    private val updateRunnable = Runnable {
        updateFromRuntime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()

        setContentView(R.layout.activity_main)

        wallpaperImageView = findViewById(R.id.wallpaperImageView)
        val savedWallpaper = getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
            .getString("wallpaper", "graphite_night") ?: "graphite_night"
        applyWallpaper(savedWallpaper)

        runtime = LauncherRuntime.get(this)
        layoutManager = DesktopLayoutManager.getInstance(this)
        layoutManager.onLayoutChanged = {
            workspaceAdapter.notifyDataSetChanged()
        }

        topWeatherText = findViewById(R.id.topWeatherText)
        topSystemStatus = findViewById(R.id.topSystemStatus)

        initWorkspace()
        initDock()
        initEditBar()

        runtime.scheduleBootAutoPlay()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        runtime.setHomeVisible(true)
        runtime.addListener(this, updateRunnable)
        updateFromRuntime()
    }

    override fun onPause() {
        super.onPause()
        runtime.setHomeVisible(false)
        runtime.removeListener(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    override fun onBackPressed() {
        if (workspaceAdapter.isEditMode) {
            setEditMode(false)
            return
        }
        super.onBackPressed()
    }

    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun initWorkspace() {
        viewPager = findViewById(R.id.workspaceViewPager)
        workspaceAdapter = WorkspacePagerAdapter(this, layoutManager) {
            setEditMode(true)
        }
        viewPager.adapter = workspaceAdapter
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                runtime.setDesktopContent(position == 0)
            }
        })
    }

    private fun initDock() {
        findViewById<View>(R.id.dockNavBtn).setOnClickListener {
            sendAction("launch-navigation")
        }
        findViewById<View>(R.id.dockMusicBtn).setOnClickListener {
            sendAction("launch-player")
        }
        findViewById<View>(R.id.dockCameraBtn).setOnClickListener {
            runtime.apps().launchOfficialCamera(runtime.vehicle().cameraCapability())
        }
        findViewById<View>(R.id.dockCarSettingsBtn).setOnClickListener {
            runtime.apps().launchPackage("com.ecarx.carsettings")
        }
        findViewById<View>(R.id.dockAppsBtn).setOnClickListener {
            val next = if (viewPager.currentItem == 0) 1 else 0
            viewPager.setCurrentItem(next, true)
        }
        findViewById<View>(R.id.dockSettingsBtn).apply {
            setOnClickListener {
                DesktopSettingsDialog(
                    context = this@MainActivity,
                    currentWallpaper = currentWallpaper,
                    onWallpaperSelected = { wp ->
                        applyWallpaper(wp)
                    },
                    onEnterEditMode = {
                        setEditMode(true)
                    },
                    onResetLayout = {
                        layoutManager.resetToDefault()
                    }
                ).show()
            }
            setOnLongClickListener {
                setEditMode(true)
                true
            }
        }
    }

    private fun applyWallpaper(name: String) {
        currentWallpaper = name
        val resId = when (name) {
            "precision_grid" -> R.drawable.wallpaper_precision_grid
            "coastal_mist" -> R.drawable.wallpaper_coastal_mist
            else -> R.drawable.wallpaper_graphite_night
        }
        wallpaperImageView.setImageResource(resId)
        getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("wallpaper", name)
            .apply()
    }

    private fun initEditBar() {
        findViewById<View>(R.id.editAddCardBtn).setOnClickListener {
            CardLibraryDialog(this, viewPager.currentItem) { type ->
                layoutManager.addCard(type, viewPager.currentItem)
            }.show()
        }
        findViewById<View>(R.id.editResetBtn).setOnClickListener {
            layoutManager.resetToDefault()
        }
        findViewById<View>(R.id.editDoneBtn).setOnClickListener {
            setEditMode(false)
        }
    }

    private fun setEditMode(editing: Boolean) {
        workspaceAdapter.isEditMode = editing
        findViewById<View>(R.id.dockContainer).visibility = if (editing) View.GONE else View.VISIBLE
        findViewById<View>(R.id.editBarContainer).visibility = if (editing) View.VISIBLE else View.GONE
    }

    private fun sendAction(action: String, value: String = "") {
        try {
            val cmd = JSONObject().put("action", action)
            if (value.isNotEmpty()) cmd.put("value", value)
            runtime.handle(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFromRuntime() {
        val snapshot = runtime.snapshot(cursor)
        workspaceAdapter.dispatchSnapshot(snapshot)

        // Update top bar weather
        val weather = snapshot.optJSONObject("weather")
        if (weather != null && weather.optBoolean("available", false)) {
            val temp = weather.optString("temp", "")
            val text = weather.optString("weather", "")
            if (temp.isNotEmpty()) {
                topWeatherText.text = "$text $temp°C".trim()
            }
        }

        // Update system status
        val status = snapshot.optJSONObject("systemStatus")
        if (status != null) {
            val wifi = status.optBoolean("wifiConnected", false)
            val bt = status.optBoolean("bluetoothConnected", false)
            topSystemStatus.text = buildString {
                append(if (wifi) "WiFi " else "4G ")
                append("| ")
                append(if (bt) "蓝牙已连接" else "蓝牙未连接")
            }
        }
    }

    companion object {
        @JvmStatic
        fun requestBootUpdateCheck() {
            // Background OTA update check
        }
    }
}
