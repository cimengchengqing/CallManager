package com.convenient.salescall.tools

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限辅助类
 * 处理存储权限的检查和请求
 */
object PermissionHelper {

    // 权限请求码
    const val REQUEST_STORAGE_PERMISSION = 1001
    const val REQUEST_MANAGE_EXTERNAL_STORAGE = 1002

    /**
     * 检查是否有存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 需要MANAGE_EXTERNAL_STORAGE权限
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ 需要READ_EXTERNAL_STORAGE权限
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 6以下默认有权限
                true
            }
        }
    }

    /**
     * 检查是否有写入存储权限
     */
    fun hasWriteStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 需要MANAGE_EXTERNAL_STORAGE权限
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ 需要WRITE_EXTERNAL_STORAGE权限
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 6以下默认有权限
                true
            }
        }
    }

    /**
     * 获取权限状态文本描述
     */
    fun getPermissionStatus(context: Context): String {
        val androidVersion = Build.VERSION.SDK_INT
        val hasReadPermission = hasStoragePermission(context)
        val hasWritePermission = hasWriteStoragePermission(context)

        return buildString {
            appendLine("Android版本: $androidVersion")
            appendLine("系统版本: ${Build.VERSION.RELEASE}")
            appendLine()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    appendLine("权限类型: MANAGE_EXTERNAL_STORAGE (Android 11+)")
                    appendLine("管理所有文件权限: ${if (Environment.isExternalStorageManager()) "✅ 已授予" else "❌ 未授予"}")
                    appendLine("外部存储状态: ${Environment.getExternalStorageState()}")
                    appendLine()
                    if (!Environment.isExternalStorageManager()) {
                        appendLine("⚠️ 需要在系统设置中手动授予'管理所有文件'权限")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    appendLine("权限类型: READ/WRITE_EXTERNAL_STORAGE (Android 6+)")
                    appendLine("读取存储权限: ${if (hasReadPermission) "✅ 已授予" else "❌ 未授予"}")
                    appendLine("写入存储权限: ${if (hasWritePermission) "✅ 已授予" else "❌ 未授予"}")
                    appendLine("外部存储状态: ${Environment.getExternalStorageState()}")
                }
                else -> {
                    appendLine("权限类型: 默认权限 (Android 6以下)")
                    appendLine("存储权限: ✅ 默认已授予")
                    appendLine("外部存储状态: ${Environment.getExternalStorageState()}")
                }
            }

            appendLine()
            appendLine("存储目录: ${Environment.getExternalStorageDirectory().absolutePath}")

            // 额外的Scoped Storage信息
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appendLine("Scoped Storage: ✅ 已启用")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    appendLine("建议使用: MediaStore API")
                }
            } else {
                appendLine("Scoped Storage: ❌ 未启用")
                appendLine("建议使用: File API")
            }
        }
    }

    /**
     * 打开系统设置页面授予权限
     */
    fun openPermissionSettings(context: Context) {
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Android 11+ 打开管理所有文件权限设置
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                else -> {
                    // Android 10及以下打开应用详情页
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            // 如果上述方式失败，尝试打开应用设置
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                // 最后尝试打开系统设置
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * 获取需要请求的权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 主要使用MANAGE_EXTERNAL_STORAGE，但仍需要READ权限作为基础
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            else -> {
                // Android 6以下不需要运行时权限
                emptyArray()
            }
        }
    }

    /**
     * 检查是否需要显示权限说明
     */
    fun shouldShowPermissionRationale(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ 的MANAGE_EXTERNAL_STORAGE权限不支持rationale
                false
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (context is android.app.Activity) {
                    context.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            context.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * 获取权限说明文本
     */
    fun getPermissionRationaleText(): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                """
                为了访问录音文件，需要授予"管理所有文件"权限。
                
                请在接下来的设置页面中：
                1. 找到"管理所有文件"选项
                2. 开启该权限
                3. 返回应用继续使用
                
                这个权限用于：
                • 读取MIUI通话录音文件
                • 访问存储在外部存储的音频文件
                • 提供文件上传功能
                """.trimIndent()
            }
            else -> {
                """
                为了读取录音文件，需要授予存储权限。
                
                这个权限用于：
                • 读取MIUI通话录音文件
                • 访问存储在外部存储的音频文件
                • 提供文件管理功能
                
                请点击"允许"以继续使用。
                """.trimIndent()
            }
        }
    }

    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 请求管理外部存储权限
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE)
            } catch (e: Exception) {
                // 如果上面的方式不行，尝试通用设置
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE)
            }
        } else {
            // Android 10及以下请求读取外部存储权限
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }

    /**
     * 处理权限请求结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
}