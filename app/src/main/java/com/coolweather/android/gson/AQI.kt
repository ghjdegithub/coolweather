package com.coolweather.android.gson

class AQI {
    var city: AQICity? = null

    inner class AQICity {
        var aqi: String? = null
        var pm25: String? = null
    }
}
