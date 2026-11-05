package xyz.i4o.uni.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject
import xyz.i4o.uni.launcher.desktop.AllAppsCenterDialog
import xyz.i4o.uni.launcher.desktop.CardLibraryDialog
import xyz.i4o.uni.launcher.desktop.ClimateControlBottomSheet
import xyz.i4o.uni.launcher.desktop.DesktopLayoutManager
import xyz.i4o.uni.launcher.desktop.DesktopPageIndicator
import xyz.i4o.uni.launcher.desktop.DesktopSettingsDialog
import xyz.i4o.uni.launcher.desktop.FullOemDockView
import xyz.i4o.uni.launcher.desktop.WeatherForecastDialog
import xyz.i4o.uni.launcher.desktop.WorkspacePagerAdapter

/**
 * Native Android View + RenderThread Launcher Activity for Lynk & Co 05.
 *
 * Direct hardware RenderNode rendering, 16ms touch responsiveness,
 * Dual-dock architecture (Floating Capsule / Full OEM Dock),
 * Steering wheel key intercept, and full modular freedom.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var runtime: LauncherRuntime
    private lateinit var layoutManager: DesktopLayoutManager
    private lateinit var viewPager: ViewPager2
    private lateinit var workspaceAdapter: WorkspacePagerAdapter
    private lateinit var topWeatherText: TextView
    private lateinit var topSystemStatus: TextView
    private lateinit var wallpaperImageView: ImageView
    private lateinit var desktopPageIndicator: DesktopPageIndicator
    private lateinit var dockContainer: View
    private lateinit var fullOemDockView: FullOemDockView
    private var currentWallpaper: String = "graphite_night"
    private var currentDockStyle: String = "floating"

    private val cursor = LauncherRuntime.StaticCursor()

    private val updateRunnable = Runnable {
        updateFromRuntime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()

        setContentView(R.layout.activity_main)

        wallpaperImageView = findViewById(R.id.wallpaperImageView)
        val prefs = getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
        val savedWallpaper = prefs.getString("wallpaper", "graphite_night") ?: "graphite_night"
        applyWallpaper(savedWallpaper)

        dockContainer = findViewById(R.id.dockContainer)
        fullOemDockView = findViewById(R.id.fullOemDockView)
        currentDockStyle = prefs.getString("dock_style", "floating") ?: "floating"
        applyDockStyle(currentDockStyle)

        runtime = LauncherRuntime.get(this)
        layoutManager = DesktopLayoutManager.getInstance(this)
        layoutManager.onLayoutChanged = {
            workspaceAdapter.notifyDataSetChanged()
            if (::desktopPageIndicator.isInitialized) {
                desktopPageIndicator.pageCount = layoutManager.getPageCount()
            }
        }

        topWeatherText = findViewById(R.id.topWeatherText)
        topWeatherText.setOnClickListener {
            WeatherForecastDialog(this).show()
        }
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
        if (viewPager.currentItem > 0) {
            navigateToPage(0)
            return
        }
        // If already at page 0, retain launcher in foreground without finishing
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    sendAction("next")
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    sendAction("previous")
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    sendAction("toggle-play")
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun navigateToPage(index: Int) {
        if (index in 0 until layoutManager.getPageCount()) {
            viewPager.setCurrentItem(index, true)
        }
    }

    fun showDesktopSettings() {
        DesktopSettingsDialog(
            context = this,
            currentWallpaper = currentWallpaper,
            currentDockStyle = currentDockStyle,
            onWallpaperSelected = { wp ->
                applyWallpaper(wp)
            },
            onDockStyleChanged = { style ->
                applyDockStyle(style)
            },
            onEnterEditMode = {
                setEditMode(true)
            },
            onResetLayout = {
                layoutManager.resetToDefault()
            }
        ).show()
    }

    fun applyDockStyle(style: String) {
        currentDockStyle = style
        getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("dock_style", style)
            .apply()
        if (::workspaceAdapter.isInitialized && workspaceAdapter.isEditMode) {
            dockContainer.visibility = View.GONE
            fullOemDockView.visibility = View.GONE
        } else {
            dockContainer.visibility = if (style == "floating") View.VISIBLE else View.GONE
            fullOemDockView.visibility = if (style == "full_oem") View.VISIBLE else View.GONE
        }
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

        desktopPageIndicator = findViewById(R.id.desktopPageIndicator)
        desktopPageIndicator.pageCount = layoutManager.getPageCount()
        desktopPageIndicator.attachToViewPager(viewPager)

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
            AllAppsCenterDialog(this) { selectedApp ->
                layoutManager.addCard("shortcuts", viewPager.currentItem)
            }.show()
        }
        findViewById<View>(R.id.dockSettingsBtn).apply {
            setOnClickListener {
                showDesktopSettings()
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
        if (editing) {
            dockContainer.visibility = View.GONE
            fullOemDockView.visibility = View.GONE
        } else {
            applyDockStyle(currentDockStyle)
        }
        findViewById<View>(R.id.editBarContainer).visibility = if (editing) View.VISIBLE else View.GONE
        if (::desktopPageIndicator.isInitialized) {
            desktopPageIndicator.visibility = if (editing || layoutManager.getPageCount() <= 1) View.GONE else View.VISIBLE
        }
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
