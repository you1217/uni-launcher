package xyz.i4o.uni.launcher.desktop

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

/**
 * Dynamic adapter for ViewPager2 desktop workspace pages, powered by DesktopLayoutManager.
 */
class WorkspacePagerAdapter(
    private val context: Context,
    private val layoutManager: DesktopLayoutManager,
    private val onEnterEditMode: () -> Unit
) : RecyclerView.Adapter<WorkspacePagerAdapter.PageViewHolder>() {

    var isEditMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val activeCards = mutableListOf<View>()
    private var lastSnapshot: JSONObject? = null

    override fun getItemCount(): Int = layoutManager.getPageCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val grid = DesktopGridLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnLongClickListener {
                onEnterEditMode()
                true
            }
            onCardDropped = { draggedCard, col, row ->
                layoutManager.updateCardPosition(draggedCard.id, col, row)
            }
        }
        return PageViewHolder(grid)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val grid = holder.itemView as DesktopGridLayout
        grid.removeAllViews()

        val cards = layoutManager.getCardsForPage(position)
        for (card in cards) {
            val contentView = createCardContent(card.type)
            activeCards.add(contentView)

            val editable = EditableCardView(context).apply {
                this.card = card
                this.isEditMode = this@WorkspacePagerAdapter.isEditMode
                setCardContent(contentView)

                onCardResized = { updated ->
                    layoutManager.updateCardBounds(updated.id, updated.x, updated.y, updated.w, updated.h)
                }
                onCardDeleted = { deleted ->
                    layoutManager.removeCard(deleted.id)
                }
                onCardLongClick = {
                    onEnterEditMode()
                }
            }
            grid.addView(editable)
        }

        // Apply cached snapshot immediately to newly bound cards
        lastSnapshot?.let { dispatchSnapshot(it) }
    }

    private fun createCardContent(type: String): View {
        return when (type) {
            "vehicle" -> VehicleCardView(context)
            "body" -> BodyDiagnosticsCardView(context)
            "music" -> MediaCardView(context)
            "trafficLight" -> TrafficLightCardView(context)
            "climate" -> ClimateCardView(context)
            "clockWeather", "clock" -> ClockWeatherCardView(context)
            "fuel", "battery" -> FuelCardView(context)
            "calendar" -> CalendarCardView(context)
            "shortcuts", "apps", "apps_full" -> AppGridCardView(context)
            else -> AppGridCardView(context)
        }
    }

    fun dispatchSnapshot(snapshot: JSONObject) {
        lastSnapshot = snapshot
        val vehicle = snapshot.optJSONObject("vehicle") ?: JSONObject()
        val climate = snapshot.optJSONObject("climate") ?: JSONObject()
        val media = snapshot.optJSONObject("media") ?: JSONObject()
        val trafficLights = snapshot.optJSONObject("trafficLights") ?: JSONObject()
        val weather = snapshot.optJSONObject("weather") ?: JSONObject()

        for (v in activeCards) {
            when (v) {
                is VehicleCardView -> v.updateTelemetry(vehicle, climate)
                is BodyDiagnosticsCardView -> v.updateTelemetry(vehicle)
                is MediaCardView -> v.updateMedia(media)
                is TrafficLightCardView -> v.updateTrafficLights(trafficLights)
                is ClimateCardView -> v.updateFromTelemetry(climate)
                is ClockWeatherCardView -> v.updateWeather(weather)
                is FuelCardView -> v.updateTelemetry(vehicle)
            }
        }
    }

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
