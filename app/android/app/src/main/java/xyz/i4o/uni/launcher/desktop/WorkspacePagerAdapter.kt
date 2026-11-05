package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

/**
 * High-performance adapter for ViewPager2 desktop workspace pages.
 */
class WorkspacePagerAdapter(
    private val context: Context,
    private val onCardLayoutChanged: (LauncherCard) -> Unit
) : RecyclerView.Adapter<WorkspacePagerAdapter.PageViewHolder>() {

    var vehicleCardView: VehicleCardView? = null
        private set

    var mediaCardView: MediaCardView? = null
        private set

    var trafficLightCardView: TrafficLightCardView? = null
        private set

    var appGridCardView: AppGridCardView? = null
        private set

    override fun getItemCount(): Int = 2 // 2 desktop pages

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val grid = DesktopGridLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PageViewHolder(grid)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val grid = holder.itemView as DesktopGridLayout
        grid.removeAllViews()

        if (position == 0) {
            // Page 0: Primary dashboard

            // 1. Vehicle Telemetry Card (4x4)
            val vehicleCard = LauncherCard(
                id = "card-vehicle",
                page = 0,
                x = 0,
                y = 0,
                w = 4,
                h = 4,
                type = "vehicle",
                title = "车辆状态"
            )
            val vCardView = VehicleCardView(context)
            vehicleCardView = vCardView
            val vEditable = EditableCardView(context).apply {
                card = vehicleCard
                setCardContent(vCardView)
                onCardResized = onCardLayoutChanged
            }
            grid.addView(vEditable)

            // 2. Media Player Card (4x2)
            val mediaCard = LauncherCard(
                id = "card-media",
                page = 0,
                x = 4,
                y = 0,
                w = 4,
                h = 2,
                type = "media",
                title = "媒体播放"
            )
            val mCardView = MediaCardView(context)
            mediaCardView = mCardView
            val mEditable = EditableCardView(context).apply {
                card = mediaCard
                setCardContent(mCardView)
                onCardResized = onCardLayoutChanged
            }
            grid.addView(mEditable)

            // 3. Traffic Light Card (2x2)
            val trafficCard = LauncherCard(
                id = "card-traffic",
                page = 0,
                x = 4,
                y = 2,
                w = 2,
                h = 2,
                type = "traffic",
                title = "红绿灯"
            )
            val tCardView = TrafficLightCardView(context)
            trafficLightCardView = tCardView
            val tEditable = EditableCardView(context).apply {
                card = trafficCard
                setCardContent(tCardView)
                onCardResized = onCardLayoutChanged
            }
            grid.addView(tEditable)

            // 4. Quick Apps Grid (2x2)
            val appCard = LauncherCard(
                id = "card-apps",
                page = 0,
                x = 6,
                y = 2,
                w = 2,
                h = 2,
                type = "apps",
                title = "常用应用"
            )
            val aCardView = AppGridCardView(context)
            appGridCardView = aCardView
            val aEditable = EditableCardView(context).apply {
                card = appCard
                setCardContent(aCardView)
                onCardResized = onCardLayoutChanged
            }
            grid.addView(aEditable)
        } else {
            // Page 1: Extended apps page
            val fullAppCard = LauncherCard(
                id = "card-apps-full",
                page = 1,
                x = 0,
                y = 0,
                w = 8,
                h = 4,
                type = "apps_full",
                title = "全部应用"
            )
            val fullAppView = AppGridCardView(context)
            val editable = EditableCardView(context).apply {
                card = fullAppCard
                setCardContent(fullAppView)
                onCardResized = onCardLayoutChanged
            }
            grid.addView(editable)
        }
    }

    fun dispatchSnapshot(snapshot: JSONObject) {
        val vehicle = snapshot.optJSONObject("vehicle") ?: JSONObject()
        val climate = snapshot.optJSONObject("climate") ?: JSONObject()
        vehicleCardView?.updateTelemetry(vehicle, climate)

        val media = snapshot.optJSONObject("media") ?: JSONObject()
        mediaCardView?.updateMedia(media)

        val trafficLights = snapshot.optJSONObject("trafficLights") ?: JSONObject()
        trafficLightCardView?.updateTrafficLights(trafficLights)
    }

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

