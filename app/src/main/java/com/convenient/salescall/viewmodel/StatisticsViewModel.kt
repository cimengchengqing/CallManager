package com.convenient.salescall.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenient.salescall.datas.UserVoiceInfo
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatisticsViewModel(private val apiService: ApiService) : ViewModel() {

    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }
    private val _logoutResult = MutableLiveData<Result<String>>()
    val logoutResult: LiveData<Result<String>> = _logoutResult

    private val _statisticsResult = MutableLiveData<Result<List<UserVoiceInfo>>>()
    val statisticsResult: LiveData<Result<List<UserVoiceInfo>>> = _statisticsResult


    /**
     * 退出登录
     */
    fun onLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.doLogout(localDataUtils.getAuthCookie())
                if (response.isSuccessful) {
                    val body = response.body() ?: throw Exception("Empty response body")
                    _logoutResult.postValue(Result.success("成功"))
                } else {
                    // 处理请求失败的情况
                    _logoutResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                // 处理异常情况
                e.message?.let {
                    LogUtils.e("viewModelScope", it)
                }
                _logoutResult.postValue(Result.failure(Exception("请求错误: ${e.message}")))
            }
        }
    }

    /**
     * 获取统计数据
     */
    fun getStatisticsData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getStatisticsData(localDataUtils.getAuthCookie())
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        when (it.code) {
                            0 -> {
                                _statisticsResult.postValue(Result.success(body.data))
                            }

                            401 -> {
                                _statisticsResult.postValue(Result.failure(Exception("登录过期")))
                                localDataUtils.clearAuthCookie()
                            }

                            else -> {
                                _statisticsResult.postValue(Result.failure(Exception("请求出错: ${it.msg}")))
                            }
                        }
                    }
                } else {
                    // 处理请求失败的情况
                    _statisticsResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                // 处理异常情况
                e.message?.let {
                    LogUtils.e("viewModelScope", it)
                }
                _statisticsResult.postValue(Result.failure(Exception("请求错误: ${e.message}")))
            }
        }
    }
}