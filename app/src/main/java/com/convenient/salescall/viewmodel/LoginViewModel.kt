package com.convenient.salescall.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenient.salescall.activity.LoginActivity.Companion.JSESSIONID
import com.convenient.salescall.activity.LoginActivity.Companion.TAG
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.tools.LocalDataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class LoginViewModel(private val apiService: ApiService) : ViewModel() {

    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }
    private val _loginResult = MutableLiveData<Result<ApiService.AppResponse<Unit>>>()
    val loginResult: LiveData<Result<ApiService.AppResponse<Unit>>> = _loginResult

    private val _captchaImageResult = MutableLiveData<Result<ResponseBody>>()
    val captchaImageResult: LiveData<Result<ResponseBody>> = _captchaImageResult

    /**
     * 获取验证码图片
     */
    fun getCaptchaImage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getCaptchaImage()
                val headers = response.headers()
                headers.let {
                    JSESSIONID = headers.values("Set-Cookie").first()
                    localDataUtils.saveAuthCookie(JSESSIONID)
                    Log.d(TAG, "登录前JSESSIONID:$JSESSIONID ")
                }
                if (response.isSuccessful) {
                    val body = response.body() ?: throw Exception("Empty response body")
                    _captchaImageResult.postValue(Result.success(body))
                } else {
                    // 处理请求失败的情况
                    _captchaImageResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                // 处理异常情况
                _captchaImageResult.postValue(Result.failure(Exception("请求错误: ${e.message}")))
            }
        }
    }

    /**
     * 请求登录
     * 测试账号：
     *  gtd
     *  N@DU3Mjgs-
     */
    fun performLogin(name: String, pw: String, code: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.loginByPassword(
                    localDataUtils.getAuthCookie(),
                    localDataUtils.getRegistrationId(),
                    name, pw, code, true
                )

                val headers = response.headers()
                headers.let {
                    val id = headers.values("Set-Cookie").first()
                    Log.d(TAG, "登录后JSESSIONID:$id ")
                }

                if (response.isSuccessful) {
                    Log.e(TAG, "请求成功")
                    response.body()?.let {
                        if (it.code == 0) {
//                            JSESSIONID = localDataUtils.getAuthCookie()
//                                .substringAfter("JSESSIONID=")
//                                .substringBefore(";")
//                            localDataUtils.saveAuthCookie(JSESSIONID)
//                            Log.d(TAG, "登录后JSESSIONID:$JSESSIONID ")
                            _loginResult.postValue(Result.success(it))
                        } else {
                            _loginResult.postValue(Result.failure(Exception("请求错误: ${it.msg}")))
                        }
                    }
                } else {
                    // 处理请求失败的情况
                    _loginResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                }
            } catch (e: Exception) {
                // 处理异常情况
                _loginResult.postValue(Result.failure(Exception("请求错误: ${e.message}")))
            }
        }
    }
}