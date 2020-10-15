package com.coolweather.android

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.coolweather.android.db.City
import com.coolweather.android.db.County
import com.coolweather.android.db.Province
import com.coolweather.android.util.HttpUtil
import com.coolweather.android.util.Utility
import kotlinx.android.synthetic.main.choose_area.*
import kotlinx.android.synthetic.main.choose_area.view.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.litepal.crud.DataSupport
import java.io.IOException

class ChooseAreaFragment : Fragment() {
    companion object {
        const val LEVEL_PROVINCE = 0
        const val LEVEL_CITY = 1
        const val LEVEL_COUNTY = 2
    }

    var progressDialog: ProgressDialog? = null
    lateinit var adapter: ArrayAdapter<String>
    var dataList = arrayListOf<String>()
    lateinit var provinceList: List<Province>
    lateinit var cityList: List<City>
    lateinit var countyList: List<County>
    lateinit var selectedProvince: Province
    lateinit var selectedCity: City
    var currentLevel: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.choose_area, container, false)
        if (context != null) {
            adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_1, dataList)
        }
        view.listView.adapter = adapter
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listView.setOnItemClickListener { parent, view, position, id ->
            when (currentLevel) {
                LEVEL_PROVINCE -> {
                    selectedProvince = provinceList?.get(position)
                    queryCities()
                }
                LEVEL_CITY -> {
                    selectedCity = cityList?.get(position)
                    queryCounties()
                }
                LEVEL_COUNTY -> {
                    val weatherId = countyList[position].weatherId
                    val intent = Intent(activity, WeatherActivity::class.java)
                    intent.putExtra("weather_id", weatherId)
                    startActivity(intent)
                    activity?.finish()
                }
            }
        }
        backButton.setOnClickListener {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities()
            } else if (currentLevel == LEVEL_CITY) {
                queryProvinces()
            }
        }
        queryProvinces()
    }

    /**
     * 查询全国所有的省,优先从数据库查询,如果没有查询到再去服务器上查询
     */
    private fun queryProvinces() {
        titleText.text = "中国"
        backButton.visibility = View.GONE
        provinceList = DataSupport.findAll(Province::class.java)
        if (provinceList.isNotEmpty()) {
            dataList.clear()
            for (province in provinceList) {
                province.provinceName?.let { dataList.add(it) }
            }
            adapter.notifyDataSetChanged()
            listView.setSelection(0)
            currentLevel = LEVEL_PROVINCE
        } else {
            val address = "http://guolin.tech/api/china"
            queryFromServer(address, "province")
        }
    }

    private fun queryCities() {
        titleText.text = selectedProvince.provinceName
        backButton.visibility = View.VISIBLE
        cityList = DataSupport.where("provinceid = ?", selectedProvince.id.toString()).find(City::class.java)
        if (cityList.isNotEmpty()) {
            dataList.clear()
            for (city in cityList) {
                city.cityName?.let { dataList.add(it) }
            }
            adapter.notifyDataSetChanged()
            listView.setSelection(0)
            currentLevel = LEVEL_CITY
        } else {
            val provinceCode = selectedProvince.provinceCode
            val address = "http://guolin.tech/api/china/$provinceCode"
            queryFromServer(address, "city")
        }
    }

    private fun queryCounties() {
        titleText.text = selectedCity.cityName
        backButton.visibility = View.VISIBLE
        countyList = DataSupport.where("cityid = ?", selectedCity.id.toString()).find(County::class.java)
        if (countyList.isNotEmpty()) {
            dataList.clear()
            for (county in countyList) {
                county.countyName?.let { dataList.add(it) }
            }
            adapter.notifyDataSetChanged()
            listView.setSelection(0)
            currentLevel = LEVEL_COUNTY
        } else {
            val provinceCode = selectedProvince.provinceCode
            val cityCode = selectedCity.cityCode
            val address = "http://guolin.tech/api/china/$provinceCode/$cityCode"
            queryFromServer(address, "county")
        }
    }


    private fun queryFromServer(address: String, type: String) {
        showProgressDialog()
        HttpUtil.sendOkHttpRequest(address, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body?.string()
                responseText?.let {
                    val result = when (type) {
                        "province" -> {
                            Utility.handleProvinceResponse(responseText)
                        }
                        "city" -> {
                            Utility.handleCityResponse(responseText, selectedProvince.id!!)
                        }
                        "county" -> {
                            Utility.handleCountyResponse(responseText, selectedCity.id!!)
                        }
                        else -> false
                    }
                    if (result) {
                        activity?.runOnUiThread {
                            closeProgressDialog()
                            when (type) {
                                "province" -> queryProvinces()
                                "city" -> queryCities()
                                "county" -> queryCounties()
                            }
                        }
                    }
                }


            }

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    closeProgressDialog()
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show()
                }
            }

        })
    }

    private fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(activity)
            progressDialog?.let {
                it.setMessage("正在加载...")
                it.setCanceledOnTouchOutside(false)
            }
        }
    }

    private fun closeProgressDialog() {
        progressDialog?.dismiss()
    }


}
