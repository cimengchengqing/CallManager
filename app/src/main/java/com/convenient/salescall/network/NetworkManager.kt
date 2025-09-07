package com.convenient.salescall.network

import android.content.Context
import com.convenient.salescall.network.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager private constructor(private val context: Context) {

    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .pingInterval(30, TimeUnit.SECONDS)

        // 认证拦截器在前
        builder.addInterceptor(AuthInterceptor(context))

        // 使用其一：自定义日志拦截器（记录异常）
        builder.addNetworkInterceptor(LoggingInterceptor(BuildConfig.DEBUG))

        // 或使用其二：OkHttp 官方日志拦截器（二选一）
        // builder.addNetworkInterceptor(provideLoggingInterceptor(BuildConfig.DEBUG))

        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(requireNotNull(NetworkConfig.BASE_URL.takeIf { it.endsWith("/") }) {
                "NetworkConfig.BASE_URL 必须以 / 结尾"
            })
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkManager? = null

        fun getInstance(context: Context): NetworkManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}