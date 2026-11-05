package xyz.i4o.uni.launcher.desktop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import xyz.i4o.uni.launcher.R

/**
 * 1:1 Nocturne-styled Weather & Air Quality Forecast Sheet for UNI Launcher.
 * Features 24-hour hourly trend, lifestyle air-quality indices, and 3-day overview.
 */
class WeatherForecastDialog(context: Context) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_glass)
            val pad = dpToPx(24f)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(dpToPx(760f), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // 1. Header with Close Button
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            text = "气象与空气质量"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTitleView = TextView(context).apply {
            text = "未来 24 小时车载实时气象走势与生活指数"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            setPadding(0, dpToPx(3f), 0, 0)
        }
        titleCol.addView(titleView)
        titleCol.addView(subTitleView)
        header.addView(titleCol)

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40f), dpToPx(40f))
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // 2. Current Conditions Overview Card
        val overviewCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val p = dpToPx(16f)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16f)
            }
        }

        val tempLarge = TextView(context).apply {
            text = "18°C"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dpToPx(16f), 0)
        }
        overviewCard.addView(tempLarge)

        val conditionCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val condText = TextView(context).apply {
            text = "晴朗 · 适宜驾驶出行"
            setTextColor(Color.parseColor("#EFF4F6"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val highLowText = TextView(context).apply {
            text = "最高 22°C / 最低 12°C · 气压稳定"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 12f
            setPadding(0, dpToPx(3f), 0, 0)
        }
        conditionCol.addView(condText)
        conditionCol.addView(highLowText)
        overviewCard.addView(conditionCol)

        // 4 Weather Indices Grid
        val indicesCol = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val aqiPill = createIndexBadge("AQI 28 优", Color.parseColor("#3BE2C0"))
        val humPill = createIndexBadge("湿度 56%", Color.parseColor("#64C8FF"))
        val windPill = createIndexBadge("北风 2级", Color.WHITE)
        indicesCol.addView(aqiPill)
        indicesCol.addView(humPill)
        indicesCol.addView(windPill)
        overviewCard.addView(indicesCol)

        root.addView(overviewCard)

        // 3. 24-Hour Trend Strip
        val trendTitle = TextView(context).apply {
            text = "24 小时气温与天气走势"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(8f))
        }
        root.addView(trendTitle)

        val hourlyScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val hourlyContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val hourlyData = listOf(
            Triple("现在", "18°", "晴"),
            Triple("12:00", "21°", "晴"),
            Triple("14:00", "22°", "多云"),
            Triple("16:00", "20°", "多云"),
            Triple("18:00", "17°", "阴"),
            Triple("20:00", "15°", "阴"),
            Triple("22:00", "14°", "晴夜")
        )

        for ((hour, temp, cond) in hourlyData) {
            val hBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_dock_item_glass)
                val p = dpToPx(12f)
                setPadding(p, p, p, p)
                layoutParams = LinearLayout.LayoutParams(dpToPx(94f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dpToPx(8f)
                }

                val tHour = TextView(context).apply {
                    text = hour
                    setTextColor(Color.parseColor("#8A99AD"))
                    textSize = 11f
                }
                val tCond = TextView(context).apply {
                    text = cond
                    setTextColor(Color.parseColor("#EFF4F6"))
                    textSize = 12f
                    setPadding(0, dpToPx(4f), 0, dpToPx(4f))
                }
                val tTemp = TextView(context).apply {
                    text = temp
                    setTextColor(Color.parseColor("#3BE2C0"))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(tHour)
                addView(tCond)
                addView(tTemp)
            }
            hourlyContainer.addView(hBox)
        }
        hourlyScroll.addView(hourlyContainer)
        root.addView(hourlyScroll)

        // 4. 3-Day Forecast Strip
        val futureTitle = TextView(context).apply {
            text = "未来 3 天气象趋势"
            setTextColor(Color.parseColor("#3BE2C0"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dpToPx(18f), 0, dpToPx(8f))
        }
        root.addView(futureTitle)

        val daysContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dock_item_glass)
            val p = dpToPx(12f)
            setPadding(p, p, p, p)
        }

        val days = listOf(
            Triple("今天 · 11月5日", "晴朗", "12°C ~ 22°C · 空气优"),
            Triple("明天 · 11月6日", "多云转阴", "13°C ~ 21°C · 空气良"),
            Triple("后天 · 11月7日", "局部小雨", "11°C ~ 18°C · 空气优")
        )

        for ((day, weather, info) in days) {
            val dRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(32f)
                )

                val dayView = TextView(context).apply {
                    text = day
                    setTextColor(Color.parseColor("#EFF4F6"))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(dpToPx(140f), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val weatherView = TextView(context).apply {
                    text = weather
                    setTextColor(Color.parseColor("#3BE2C0"))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(dpToPx(120f), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val infoView = TextView(context).apply {
                    text = info
                    setTextColor(Color.parseColor("#8A99AD"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                addView(dayView)
                addView(weatherView)
                addView(infoView)
            }
            daysContainer.addView(dRow)
        }
        root.addView(daysContainer)

        setContentView(root)
    }

    private fun createIndexBadge(text: String, color: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tire_chip)
            val hp = dpToPx(8f)
            val vp = dpToPx(4f)
            setPadding(hp, vp, hp, vp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(6f)
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}

