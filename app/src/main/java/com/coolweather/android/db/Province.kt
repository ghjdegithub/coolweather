package com.coolweather.android.db

import org.litepal.crud.DataSupport

class Province : DataSupport() {
    var id: Int? = null
    var provinceName: String? = null
    var provinceCode: Int? = null
}
