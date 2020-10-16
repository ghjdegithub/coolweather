package com.coolweather.android.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.preference.PreferenceManager
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class AutoUpdateService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateWeather()
        updateBingPic()
        val manager = getSystemService(ALARM_SERVICE) as AlarmManager
        val anHour = 8 * 60 * 60 * 1000
        val triggerAtTime = SystemClock.elapsedRealtime() + anHour
        val i = Intent(this, this::class.java)
        val pi = PendingIntent.getService(this, 0, i, 0)
        manager.cancel(pi)
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWeather() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val weatherString = prefs.getString("weather", null)
        if (weatherString != null) {
            val weather = Utility.handleWeatherResponse(weatherString)
            val weatherId = weather?.basic?.weatherId!!
            val weatherUrl = "http://guolin.tech/api/weather?cityid=$weatherId&key=bbcf336ec3ce4d6e8fdd92650fc9b2fd"
            HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.body?.let {
                        val responseText = it.string()
                        val weather = Utility.handleWeatherResponse(responseText)
                        if (weather != null && "ok" == (weather.status)) {
                            val editor =
                                    PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateService).edit()
                            editor.putString("weather", responseText)
                            editor.apply()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
            })
        }
    }

    private fun updateBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic"
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body?.string()
                val editor = PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateService).edit()
                editor.putString("bing_pic", bingPic)
                editor.apply()
            }

        })
    }


}
