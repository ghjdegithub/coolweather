package com.coolweather.android.db

import org.litepal.crud.DataSupport

class City : DataSupport() {
    var id: Int? = null
    var cityName: String? = null
    var cityCode: Int? = null
    var provinceId: Int? = null
}
