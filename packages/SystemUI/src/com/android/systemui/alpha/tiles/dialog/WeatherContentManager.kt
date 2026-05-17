/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.internal.util.alpha.OmniJawsClient
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.res.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "WeatherContentManager"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

@SysUISingleton
class WeatherContentManager
@Inject
constructor(
    @Background private val bgLooper: Looper,
) {
    private lateinit var contentView: View
    private lateinit var context: Context
    private lateinit var coroutineScope: CoroutineScope

    private lateinit var currentIcon: ImageView
    private lateinit var currentTemp: TextView
    private lateinit var cityName: TextView
    private lateinit var windInfo: TextView
    private lateinit var windDirection: TextView
    private lateinit var humidityInfo: TextView
    private lateinit var forecastContainer: LinearLayout
    private lateinit var noDataView: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bgHandler = Handler(bgLooper)

    private val omniJawsObserver = object : OmniJawsClient.OmniJawsObserver {
        override fun weatherUpdated() {
            refreshWeatherAsync()
        }

        override fun weatherError(errorReason: Int) {
            if (DEBUG) Log.d(TAG, "weatherError: $errorReason")
            mainHandler.post { showNoData() }
        }
    }

    fun bind(view: View, scope: CoroutineScope) {
        if (DEBUG) Log.d(TAG, "bind")
        contentView = view
        context = view.context
        coroutineScope = scope

        currentIcon = view.findViewById(R.id.weather_current_icon)
        currentTemp = view.findViewById(R.id.weather_current_temp)
        cityName = view.findViewById(R.id.weather_city_name)
        windInfo = view.findViewById(R.id.weather_wind_info)
        windDirection = view.findViewById(R.id.weather_wind_direction)
        humidityInfo = view.findViewById(R.id.weather_humidity_info)
        forecastContainer = view.findViewById(R.id.weather_forecast_container)
        noDataView = view.findViewById(R.id.weather_no_data)
    }

    fun start() {
        if (DEBUG) Log.d(TAG, "start")
        OmniJawsClient.get().addObserver(context, omniJawsObserver)
        refreshWeatherAsync()
    }

    fun stop() {
        if (DEBUG) Log.d(TAG, "stop")
        OmniJawsClient.get().removeObserver(context, omniJawsObserver)
    }

    private fun refreshWeatherAsync() {
        bgHandler.post {
            OmniJawsClient.get().queryWeather(context)
            val info = OmniJawsClient.get().weatherInfo
            mainHandler.post {
                if (info != null) {
                    populateWeather(info)
                } else {
                    showNoData()
                }
            }
        }
    }

    private fun populateWeather(info: OmniJawsClient.WeatherInfo) {
        noDataView.visibility = View.GONE
        currentIcon.visibility = View.VISIBLE
        currentTemp.visibility = View.VISIBLE
        cityName.visibility = View.VISIBLE
        windInfo.visibility = View.VISIBLE
        windDirection.visibility = View.VISIBLE
        humidityInfo.visibility = View.VISIBLE
        forecastContainer.visibility = View.VISIBLE

        val condIcon: Drawable? = OmniJawsClient.get().getWeatherConditionImage(context, info.conditionCode)
        if (condIcon != null) {
            currentIcon.setImageDrawable(condIcon)
        }

        currentTemp.text = "${info.temp}${info.tempUnits}"
        cityName.text = info.city ?: ""

        windInfo.text = "${info.windSpeed} ${info.windUnits}"
        windDirection.text = info.windDirection ?: ""

        humidityInfo.text = "${info.humidity}%"

        populateForecasts(info.forecasts ?: emptyList())
    }

    private fun populateForecasts(forecasts: List<OmniJawsClient.DayForecast>) {
        forecastContainer.removeAllViews()

        val inflater = android.view.LayoutInflater.from(context)
        val displayForecasts = forecasts.take(5)

        for (day in displayForecasts) {
            val dayView = inflater.inflate(R.layout.weather_forecast_day_item, forecastContainer, false)

            val dayLabel = dayView.findViewById<TextView>(R.id.forecast_day_label)
            val dayIcon = dayView.findViewById<ImageView>(R.id.forecast_day_icon)
            val dayHigh = dayView.findViewById<TextView>(R.id.forecast_day_high)
            val dayLow = dayView.findViewById<TextView>(R.id.forecast_day_low)

            dayLabel.text = formatForecastDate(day.date)

            val icon: Drawable? = OmniJawsClient.get().getWeatherConditionImage(context, day.conditionCode)
            if (icon != null) {
                dayIcon.setImageDrawable(icon)
            }

            dayHigh.text = day.high
            dayLow.text = day.low

            forecastContainer.addView(dayView)
        }
    }

    private fun formatForecastDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date: Date = inputFormat.parse(dateStr) ?: return dateStr
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun showNoData() {
        noDataView.visibility = View.VISIBLE
        currentIcon.visibility = View.GONE
        currentTemp.visibility = View.GONE
        cityName.visibility = View.GONE
        windInfo.visibility = View.GONE
        windDirection.visibility = View.GONE
        humidityInfo.visibility = View.GONE
        forecastContainer.visibility = View.GONE
    }
}
