package com.coolweather.android.db

import org.litepal.crud.DataSupport

class City(val id: Int, val cityName: String, val cityCode: Int, val provinceCode: Int) : DataSupport() {
}
