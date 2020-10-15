package com.coolweather.android.db

import org.litepal.crud.DataSupport

class County() : DataSupport() {
    var id: Int? = null
    var countyName: String? = null
    var weatherId: String? = null
    var cityId: Int? = null
}
