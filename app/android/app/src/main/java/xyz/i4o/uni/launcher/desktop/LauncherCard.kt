package xyz.i4o.uni.launcher.desktop

import org.json.JSONObject

/**
 * Data model for a single desktop card.
 *
 * Coordinates are on an 8-column x 4-row grid per desktop page.
 */
data class LauncherCard(
    val id: String,
    var page: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var w: Int = 2,
    var h: Int = 2,
    val minW: Int = 2,
    val minH: Int = 1,
    val maxW: Int = 8,
    val maxH: Int = 4,
    val type: String = "empty",
    val title: String = ""
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("page", page)
            put("x", x)
            put("y", y)
            put("w", w)
            put("h", h)
            put("type", type)
            put("title", title)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): LauncherCard {
            return LauncherCard(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                page = json.optInt("page", 0),
                x = json.optInt("x", 0),
                y = json.optInt("y", 0),
                w = json.optInt("w", 2),
                h = json.optInt("h", 2),
                type = json.optString("type", "empty"),
                title = json.optString("title", "")
            )
        }
    }
}

