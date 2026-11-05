package xyz.i4o.uni.launcher.platform

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.InputEvent
import android.view.MotionEvent
import android.view.Surface
import xyz.i4o.uni.launcher.LauncherRuntime
import xyz.i4o.uni.launcher.integration.NavigationApps
import java.lang.reflect.Method

/**
 * Android 9 Native VirtualDisplay Navigation Host.
 * Powers zero-latency in-car hardware rendering for Amap Auto (com.autonavi.amapauto)
 * directly into a native Surface/TextureView, with touch event forwarding via InputForwarder.
 */
class NativeNavigationHost(private val activity: Activity) {

    companion object {
        private const val TAG = "UniNativeNavHost"
    }

    private val runtime = LauncherRuntime.get(activity)
    private val navigationApps: NavigationApps = NavigationApps(activity, activity.getSharedPreferences("launcher", 0))
    private val mainHandler = Handler(Looper.getMainLooper())

    private var display: VirtualDisplay? = null
    private var inputForwarder: Any? = null
    private var forwardEventMethod: Method? = null

    var isReady = false
        private set
    var virtualWidth = 1144
        private set
    var virtualHeight = 732
        private set
    var displayId = -1
        private set

    fun isAvailable(): Boolean {
        val snapshot = navigationApps.snapshot()
        return snapshot.optBoolean("canEmbed", false)
    }

    fun selectedPackage(): String {
        return navigationApps.selected()
    }

    /**
     * Creates the VirtualDisplay targeting the provided Surface and launches Amap into it.
     */
    fun createDisplay(surface: Surface, width: Int, height: Int, scale: Float = 1.0f): Boolean {
        release()
        this.virtualWidth = (width * scale).toInt().coerceAtLeast(100)
        this.virtualHeight = (height * scale).toInt().coerceAtLeast(100)

        return try {
            val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                ?: return false

            val densityDpi = (activity.resources.displayMetrics.densityDpi * scale).toInt().coerceAtLeast(72)

            display = displayManager.createVirtualDisplay(
                "UNI_Live_Nav_Display",
                virtualWidth,
                virtualHeight,
                densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            )

            if (display == null) {
                Log.w(TAG, "VirtualDisplay creation returned null")
                return false
            }

            displayId = display!!.display.displayId

            // Android 9 System Display setup: dontOverrideDisplayInfo
            try {
                val wm = Class.forName("android.view.WindowManagerGlobal").getMethod("getWindowManagerService").invoke(null)
                val override = wm.javaClass.getMethod("dontOverrideDisplayInfo", Int::class.javaPrimitiveType)
                override.isAccessible = true
                override.invoke(wm, displayId)
            } catch (e: Exception) {
                Log.i(TAG, "dontOverrideDisplayInfo reflection skipped: ${e.message}")
            }

            // Android 9 Touch InputForwarder setup
            try {
                val inputManagerClass = Class.forName("android.hardware.input.InputManager")
                val inputInstance = inputManagerClass.getMethod("getInstance").invoke(null)
                val createForwarder = inputInstance.javaClass.getMethod("createInputForwarder", Int::class.javaPrimitiveType)
                createForwarder.isAccessible = true
                inputForwarder = createForwarder.invoke(inputInstance, displayId)
                forwardEventMethod = inputForwarder?.javaClass?.getMethod("forwardEvent", InputEvent::class.java)
                forwardEventMethod?.isAccessible = true
            } catch (e: Exception) {
                Log.i(TAG, "InputForwarder reflection skipped: ${e.message}")
            }

            // Launch Navigation Activity on this VirtualDisplay
            launchNavigation()

            isReady = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create VirtualDisplay: ${e.message}", e)
            release()
            false
        }
    }

    fun launchNavigation() {
        val dispId = displayId
        if (dispId < 0) return
        try {
            val intent = navigationApps.displayIntent() ?: return
            val options = ActivityOptions.makeBasic()
            options.setLaunchDisplayId(dispId)
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options.toBundle())
            Log.i(TAG, "Started navigation on VirtualDisplay: $dispId")
        } catch (e: Exception) {
            Log.w(TAG, "Could not launch navigation to display $dispId: ${e.message}")
        }
    }

    fun launchFullscreen(): Boolean {
        return navigationApps.launchFullscreen()
    }

    fun forwardTouchEvent(event: MotionEvent, cardWidth: Int, cardHeight: Int): Boolean {
        val forwarder = inputForwarder ?: return false
        val forwardMethod = forwardEventMethod ?: return false
        if (cardWidth <= 0 || cardHeight <= 0) return false

        return try {
            val scaleX = virtualWidth.toFloat() / cardWidth
            val scaleY = virtualHeight.toFloat() / cardHeight
            val cloned = MotionEvent.obtain(event)
            cloned.setLocation(event.x * scaleX, event.y * scaleY)
            forwardMethod.invoke(forwarder, cloned)
            cloned.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resize(width: Int, height: Int, scale: Float = 1.0f) {
        val nextW = (width * scale).toInt().coerceAtLeast(100)
        val nextH = (height * scale).toInt().coerceAtLeast(100)
        if (nextW == virtualWidth && nextH == virtualHeight) return
        virtualWidth = nextW
        virtualHeight = nextH
        val densityDpi = (activity.resources.displayMetrics.densityDpi * scale).toInt().coerceAtLeast(72)
        display?.resize(virtualWidth, virtualHeight, densityDpi)
    }

    fun release() {
        isReady = false
        display?.release()
        display = null
        inputForwarder = null
        forwardEventMethod = null
        displayId = -1
    }
}

