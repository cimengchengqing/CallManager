package com.convenient.salescall.tools

import android.util.Log
import com.convenient.salescall.BuildConfig

/**
 * 简洁的日志工具类
 * 功能：
 * - 根据构建类型自动控制日志输出（debug=true, release=false）
 * - 提供四种基本日志级别：d, i, w, e
 */
object LogUtils {

    /**
     * 是否为调试模式
     * debug构建为true，release构建为false
     */
    private val IS_DEBUG = BuildConfig.DEBUG

    /**
     * Debug日志
     */
    @JvmStatic
    fun d(tag: String, msg: String) {
        if (IS_DEBUG) {
            Log.d(tag, msg)
        }
    }

    /**
     * Info日志
     */
    @JvmStatic
    fun i(tag: String, msg: String) {
        if (IS_DEBUG) {
            Log.i(tag, msg)
        }
    }

    /**
     * Warning日志
     */
    @JvmStatic
    fun w(tag: String, msg: String) {
        if (IS_DEBUG) {
            Log.w(tag, msg)
        }
    }

    /**
     * Error日志
     */
    @JvmStatic
    fun e(tag: String, msg: String) {
        if (IS_DEBUG) {
            Log.e(tag, msg)
        }
    }

    /**
     * 检查当前是否为debug环境
     */
    @JvmStatic
    fun isDebug(): Boolean = IS_DEBUG
}