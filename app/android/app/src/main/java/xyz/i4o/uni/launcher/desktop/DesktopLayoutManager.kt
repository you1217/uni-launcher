package xyz.i4o.uni.launcher.desktop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import xyz.i4o.uni.launcher.LauncherRuntime

/**
 * Manages the desktop grid card layout in full 12-column x 6-row space:
 * loading, adding, deleting, resizing, and persisting via savedDesign JSON.
 */
class DesktopLayoutManager private constructor(private val context: Context) {

    private val runtime = LauncherRuntime.get(context)
    private val cards = mutableListOf<LauncherCard>()
    var onLayoutChanged: (() -> Unit)? = null

    init {
        loadLayout()
    }

    fun loadLayout() {
        cards.clear()
        val raw = runtime.prefs().getString("savedDesign", "")
        if (!raw.isNullOrEmpty()) {
            try {
                val data = JSONObject(raw)
                val cardsArray = data.optJSONArray("cards")
                if (cardsArray != null) {
                    for (i in 0 until cardsArray.length()) {
                        val cJson = cardsArray.getJSONObject(i)
                        val card = LauncherCard.fromJson(cJson)
                        // Normalize types
                        val normalizedType = when (card.type) {
                            "tpms" -> "vehicle"
                            "apps", "apps_full" -> "shortcuts"
                            else -> card.type
                        }
                        val normalizedCard = card.copy(
                            type = normalizedType,
                            w = card.w.coerceIn(2, 12),
                            h = card.h.coerceIn(1, 6)
                        )
                        cards.add(normalizedCard)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (cards.isEmpty()) {
            initDefaultCards()
            saveLayout()
        }
    }

    private fun initDefaultCards() {
        cards.clear()
        // Page 0: Main driving dashboard (12 cols x 6 rows)
        cards.add(LauncherCard("card-vehicle", page = 0, x = 0, y = 0, w = 7, h = 6, minW = 4, minH = 3, type = "vehicle", title = "车辆状态"))
        cards.add(LauncherCard("card-media", page = 0, x = 7, y = 0, w = 5, h = 3, minW = 3, minH = 2, type = "music", title = "多媒体"))
        cards.add(LauncherCard("card-traffic", page = 0, x = 7, y = 3, w = 5, h = 3, minW = 3, minH = 2, type = "trafficLight", title = "红绿灯"))

        // Page 1: Extended vehicle controls & info
        cards.add(LauncherCard("card-climate", page = 1, x = 0, y = 0, w = 6, h = 4, minW = 3, minH = 2, type = "climate", title = "智能空调"))
        cards.add(LauncherCard("card-shortcuts", page = 1, x = 0, y = 4, w = 6, h = 2, minW = 3, minH = 2, type = "shortcuts", title = "常用应用"))
        cards.add(LauncherCard("card-clockWeather", page = 1, x = 6, y = 0, w = 6, h = 3, minW = 3, minH = 2, type = "clockWeather", title = "时钟天气"))
        cards.add(LauncherCard("card-fuel", page = 1, x = 6, y = 3, w = 6, h = 3, minW = 3, minH = 2, type = "fuel", title = "燃油续航"))
    }

    fun getCardsForPage(page: Int): List<LauncherCard> {
        return cards.filter { it.page == page }
    }

    fun getPageCount(): Int {
        val maxPage = cards.maxOfOrNull { it.page } ?: 0
        return (maxPage + 1).coerceAtLeast(2)
    }

    fun addCard(type: String, targetPage: Int): LauncherCard? {
        val defaultSize = when (type) {
            "nav" -> Pair(7, 6)
            "vehicle" -> Pair(7, 6)
            "body" -> Pair(5, 4)
            "music", "climate", "clockWeather" -> Pair(5, 3)
            "trafficLight", "fuel", "calendar" -> Pair(5, 3)
            "battery", "air", "cleanup" -> Pair(5, 3)
            "clock", "weather", "member" -> Pair(5, 3)
            "shortcuts" -> Pair(6, 2)
            else -> Pair(4, 2)
        }
        val reqW = defaultSize.first
        val reqH = defaultSize.second

        val pos = findFreeSlot(targetPage, reqW, reqH)
        val (finalPage, x, y) = if (pos != null) {
            Triple(targetPage, pos.first, pos.second)
        } else {
            val newPage = targetPage + 1
            val nextPos = findFreeSlot(newPage, reqW, reqH) ?: Pair(0, 0)
            Triple(newPage, nextPos.first, nextPos.second)
        }

        val title = when (type) {
            "nav" -> "地图导航"
            "vehicle" -> "车辆状态"
            "body" -> "车身诊断"
            "music" -> "音乐播放"
            "climate" -> "智能空调"
            "clockWeather" -> "时钟天气"
            "trafficLight" -> "高德红绿灯"
            "shortcuts" -> "常用应用"
            "fuel" -> "燃油续航"
            "calendar" -> "日历日程"
            "battery" -> "动力电池"
            "air" -> "车内空气"
            "cleanup" -> "系统清理"
            "clock" -> "极简时钟"
            "weather" -> "独立天气"
            "member" -> "领克账号"
            else -> "扩展卡片"
        }

        val newCard = LauncherCard(
            id = "card-${type}-${System.currentTimeMillis()}",
            page = finalPage,
            x = x,
            y = y,
            w = reqW,
            h = reqH,
            type = type,
            title = title
        )
        cards.add(newCard)
        saveLayout()
        onLayoutChanged?.invoke()
        return newCard
    }

    fun removeCard(cardId: String) {
        cards.removeAll { it.id == cardId }
        saveLayout()
        onLayoutChanged?.invoke()
    }

    fun updateCardBounds(cardId: String, x: Int, y: Int, w: Int, h: Int) {
        val card = cards.find { it.id == cardId } ?: return
        card.x = x
        card.y = y
        card.w = w
        card.h = h
        saveLayout()
    }

    fun updateCardProperties(cardId: String, title: String, w: Int, h: Int) {
        val index = cards.indexOfFirst { it.id == cardId }
        if (index == -1) return
        val card = cards[index]
        val safeW = w.coerceIn(2, 12)
        val safeH = h.coerceIn(1, 6)
        val safeX = card.x.coerceIn(0, 12 - safeW)
        val safeY = card.y.coerceIn(0, 6 - safeH)
        cards[index] = card.copy(
            title = title,
            w = safeW,
            h = safeH,
            x = safeX,
            y = safeY
        )
        saveLayout()
        onLayoutChanged?.invoke()
    }

    fun updateCardPosition(cardId: String, x: Int, y: Int) {
        val card = cards.find { it.id == cardId } ?: return
        card.x = x.coerceIn(0, 12 - card.w)
        card.y = y.coerceIn(0, 6 - card.h)
        saveLayout()
        onLayoutChanged?.invoke()
    }

    fun resetToDefault() {
        initDefaultCards()
        saveLayout()
        onLayoutChanged?.invoke()
    }

    private fun findFreeSlot(page: Int, w: Int, h: Int): Pair<Int, Int>? {
        val cols = 12
        val rows = 6
        val grid = Array(rows) { BooleanArray(cols) { false } }
        for (c in cards.filter { it.page == page }) {
            for (r in c.y until (c.y + c.h).coerceAtMost(rows)) {
                for (col in c.x until (c.x + c.w).coerceAtMost(cols)) {
                    grid[r][col] = true
                }
            }
        }

        for (r in 0..(rows - h)) {
            for (col in 0..(cols - w)) {
                var canFit = true
                for (dr in 0 until h) {
                    for (dc in 0 until w) {
                        if (grid[r + dr][col + dc]) {
                            canFit = false
                            break
                        }
                    }
                    if (!canFit) break
                }
                if (canFit) return Pair(col, r)
            }
        }
        return null
    }

    fun saveLayout() {
        try {
            val json = JSONObject().apply {
                put("version", 5)
                put("pageCount", getPageCount())
                val prefs = JSONObject().apply {
                    put("mode", "dark")
                    put("themeStyle", "nocturne")
                    put("custom", "#3BE2C0")
                    put("palette", "mint")
                    put("radius", 25)
                    put("dockSize", "large")
                }
                put("preferences", prefs)
                val cardsArr = JSONArray()
                for (c in cards) {
                    cardsArr.put(c.toJsonObject())
                }
                put("cards", cardsArr)
            }
            runtime.saveDesign(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var instance: DesktopLayoutManager? = null

        fun getInstance(context: Context): DesktopLayoutManager {
            return instance ?: synchronized(this) {
                instance ?: DesktopLayoutManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
