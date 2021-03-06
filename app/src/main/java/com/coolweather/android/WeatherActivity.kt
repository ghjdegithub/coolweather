package com.coolweather.android

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.coolweather.android.gson.Weather
import com.coolweather.android.service.AutoUpdateService
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.aqi.*
import kotlinx.android.synthetic.main.forecast.*
import kotlinx.android.synthetic.main.forecast_item.view.*
import kotlinx.android.synthetic.main.now.*
import kotlinx.android.synthetic.main.suggestion.*
import kotlinx.android.synthetic.main.title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class WeatherActivity : AppCompatActivity() {
    var mWeatherId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        val decorView = window.decorView
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            decorView.windowInsetsController?.hide(WindowInsets.Type.statusBars())
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
//            window.setStatusBarColor(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//            window.statusBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_weather)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        if (weatherString != null) {
            val weather = Utility.handleWeatherResponse(weatherString)
            mWeatherId = weather?.basic?.weatherId
            if (weather != null) {
                showWeatherInfo(weather)
            }
        } else {
            mWeatherId = intent.getStringExtra("weather_id")
            weatherLayout.visibility = View.INVISIBLE
            mWeatherId?.let { requestWeather(it) }
        }
        swipeRefresh.setOnRefreshListener {
            mWeatherId?.let { requestWeather(it) }
        }
        navButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        val bingPic = prefs.getString("bing_pic", null)
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg)
        } else {
            loadBingPic()
        }
    }

    fun requestWeather(weatherId: String) {
        val weatherUrl = "http://guolin.tech/api/weather?cityid=$weatherId&key=bbcf336ec3ce4d6e8fdd92650fc9b2fd"
        HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val responseText = it.string()
                    val weather = Utility.handleWeatherResponse(responseText)
                    runOnUiThread {
                        if (weather != null && "ok" == (weather.status)) {
                            val editor: SharedPreferences.Editor =
                                    PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                            editor.putString("weather", responseText)
                            editor.apply()
                            mWeatherId = weather.basic!!.weatherId
                            showWeatherInfo(weather)
                        } else {
                            Toast.makeText(this@WeatherActivity, "获取天气信息失败", Toast.LENGTH_SHORT).show()
                        }
                        swipeRefresh.isRefreshing = false
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@WeatherActivity, "获取天气信息失败", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            }
        })
        loadBingPic()
    }

    private fun loadBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body?.string()
                val editor = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
                runOnUiThread {
                    Glide.with(this@WeatherActivity).load(bingPic).into(bingPicImg)
                }
            }

        })
    }

    fun showWeatherInfo(weather: Weather) {
        val cityName = weather.basic?.cityName
        val updateTime = weather.basic?.update?.updateTime?.split(" ")?.get(1)
        val degree = "${weather.now?.temperature}°C"
        val weatherInfo = weather.now?.more?.info
        titleCity.text = cityName
        titleUpdateTime.text = updateTime
        degreeText.text = degree
        weatherInfoText.text = weatherInfo
        forecastLayout.removeAllViews()
        weather.forecastList?.forEach { forecast ->
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)
            view.dateText.text = forecast.date
            view.infoText.text = forecast.more?.info
            view.maxText.text = forecast.temperatur?.max
            view.minText.text = forecast.temperatur?.min
            forecastLayout.addView(view)
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi?.city?.aqi)
            pm25Text.setText(weather.aqi?.city?.pm25)
        }
        val comfort = "舒适度：${weather.suggestion?.comfort?.info}"
        val carWash = "洗车指数：${weather.suggestion?.carWash?.info}"
        val sport = "运动建议：${weather.suggestion?.sport?.info}"
        comfortText.text = comfort
        carWashText.text = carWash
        sportText.text = sport
        weatherLayout.visibility = View.VISIBLE
        val intent = Intent(this, AutoUpdateService::class.java)
        startService(intent)
    }
}
