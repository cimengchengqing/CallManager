package com.convenient.salescall.tools

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cn.jpush.android.api.JPushInterface
import com.convenient.salescall.app.CallApp
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class LocalDataUtils() {

    companion object {
        const val FIRST_INSTALL_TIME = "first_install_time"
        const val IS_AUTO_LOGIN = "is_auto_login"
        const val IS_LOGIN = "is_login"
        const val AUTH_COOKIE = "auth_cookie"
        const val LAST_TIME_LOG = "last_time_log"
        const val LAST_TIME_RECORDING = "last_time_recording"
        const val REGISTRATION_ID = "registrationId"
        const val TIME_MARK = 1751299201000
    }

    // 自定义命名空间UUID（示例使用DNS命名空间）
    private val NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

    fun generateFileUUID(file: File): UUID {
        val fileBytes = file.readBytes()
        val sha1 = MessageDigest.getInstance("SHA-1").digest(fileBytes)
        val input = NAMESPACE.toString().toByteArray() + sha1
        return UUID.nameUUIDFromBytes(input)
    }

    private val sharedPreferences: SharedPreferences by lazy {
        CallApp.appContext.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    }

    //设置极光推送的设备注册ID
    fun setFistInstallTime(value: Long) {
        sharedPreferences.edit { putLong(FIRST_INSTALL_TIME, value) }
    }

    // 获取极光推送的设备注册ID
    fun getFistInstallTime(defaultValue: Long = 0): Long {
        return sharedPreferences.getLong(FIRST_INSTALL_TIME, defaultValue)
    }

    //设置极光推送的设备注册ID
    fun setRegistrationId(value: String) {
        sharedPreferences.edit { putString(REGISTRATION_ID, value) }
    }

    // 获取极光推送的设备注册ID
    fun getRegistrationId(defaultValue: String = ""): String {
        var id = sharedPreferences.getString(REGISTRATION_ID, defaultValue) ?: defaultValue
        if (id.isEmpty()) {
            id = JPushInterface.getRegistrationID(CallApp.appContext)
        }
        return id
    }

    // 设置自动登录
    fun setAutoLogin(value: Boolean) {
        sharedPreferences.edit { putBoolean(IS_AUTO_LOGIN, value) }
    }

    // 获取自动登录配置
    fun getAutoLogin(defaultValue: Boolean = true): Boolean {
        return sharedPreferences.getBoolean(IS_AUTO_LOGIN, defaultValue) ?: defaultValue
    }

    // 设置自动登录
    fun setLogin(value: Boolean) {
        sharedPreferences.edit { putBoolean(IS_LOGIN, value) }
    }

    // 获取自动登录配置
    fun isLogin(defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(IS_LOGIN, defaultValue)
    }

    // 保存auth_cookie
    fun saveAuthCookie(value: String) {
        sharedPreferences.edit { putString(AUTH_COOKIE, value) }
    }

    // 获取auth_cookie
    fun getAuthCookie(defaultValue: String = ""): String {
        return sharedPreferences.getString(AUTH_COOKIE, defaultValue) ?: defaultValue
    }

    // 清除auth_cookie
    fun clearAuthCookie() {
        sharedPreferences.edit { remove(AUTH_COOKIE) }
    }

    // 保存last_time_log
    fun saveLastTimeLog(value: Long) {
        sharedPreferences.edit { putLong(LAST_TIME_LOG, value) }
    }

    // 获取last_time_log:
    // 1749031600000(北京时间 2025-07-01 00:00:01)
    fun getLastTimeLog(): Long {
        return sharedPreferences.getLong(LAST_TIME_LOG, getFistInstallTime())
    }

    // 清除last_time_log
    fun clearLastTimeLog() {
        sharedPreferences.edit { remove(LAST_TIME_LOG) }
    }

    // 保存last_time_recording
    fun saveLastTimeRecording(value: Long) {
        sharedPreferences.edit { putLong(LAST_TIME_RECORDING, value) }
    }

    // 获取last_time_recording
    fun getLastTimeRecording(): Long {
        return sharedPreferences.getLong(LAST_TIME_RECORDING, getFistInstallTime())
    }

    // 清除last_time_recording
    fun clearLastTimeRecording() {
        sharedPreferences.edit { remove(LAST_TIME_RECORDING) }
    }
}