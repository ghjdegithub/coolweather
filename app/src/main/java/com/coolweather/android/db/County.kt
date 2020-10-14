package com.coolweather.android.db

import org.litepal.crud.DataSupport

class County(val id: Int, val countyName: String, val weatherId: String, val cityId: Int) : DataSupport() {
}
