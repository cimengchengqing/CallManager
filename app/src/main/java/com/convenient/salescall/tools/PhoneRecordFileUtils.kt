package com.convenient.salescall.tools

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.datas.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PhoneRecordFileUtils {
    private const val TAG = "PhoneRecordFileUtils"

    // 支持的音频文件格式（扩展格式）
    private val SUPPORTED_AUDIO_FORMATS = arrayOf(
        ".mp3", ".wav", ".m4a", ".aac", ".3gp", ".amr", ".ogg", ".flac"
    )

    // 华为录音文件可能的存储路径（优先级从高到低）
    private val HUAWEI_RECORD_PATHS = arrayOf(
        "/storage/emulated/0/Sounds/CallRecord",    //电话录音专用文件夹
        "/storage/emulated/0/sounds/CallRecord",    //电话录音专用文件夹
        "/storage/emulated/0/CallRecord",           //电话录音专用文件夹
        "/storage/emulated/0/Recordings/CallRecord",    //电话录音专用文件夹
        "/storage/emulated/0/DCIM/CallRecord",      //可能的目录
        "/storage/emulated/0/HWRecorder",           //可能的目录
        "/storage/emulated/0/Android/data/com.huawei.systemmanager/files/CallRecord",   //华为自带的录音应用通常会将通话录音文件存储在该路径
        "/sdcard/sounds/CallRecord",
        "/sdcard/CallRecord"
    )

    // OPPO录音文件可能的存储路径（优先级从高到低）
    private val OPPO_RECORD_PATHS = arrayOf(
        // ColorOS 11+ 主要路径
        "/storage/emulated/0/Music/Recordings/Call Recordings",
        "/storage/emulated/0/Music/Recordings",
        "/sdcard/Music/Recordings/Call Recording",
        "/sdcard/Music/Recordings",
        // 其他可能的路径
        "/storage/emulated/0/OPPO/SoundRecorder/Call",
        "/storage/emulated/0/SoundRecorder/Call",
        "/storage/emulated/0/Recordings/Call",
        "/storage/emulated/0/CallRecord",
        "/storage/emulated/0/OPPO/CallRecord",
        "/storage/emulated/0/Android/data/com.oppo.soundrecorder/files",
        "/storage/emulated/0/Android/data/com.coloros.soundrecorder/files",
        // SD卡路径
        "/storage/sdcard1/Music/Recordings/Call Recordings",
        "/storage/sdcard1/OPPO/SoundRecorder/Call"
    )

    // MIUI录音文件可能的存储路径（扩展更多路径）
    private val MIUI_RECORD_PATHS = arrayOf(
        "/storage/emulated/0/MIUI/sound_recorder/call_rec",
        "/sdcard/MIUI/sound_recorder/call_rec",
        "/storage/emulated/0/MIUI/sound_recorder",
        "/sdcard/MIUI/sound_recorder",
        "/storage/emulated/0/sound_recorder",
        "/sdcard/sound_recorder",
        "/storage/emulated/0/Recorder",
        "/sdcard/Recorder",
        "/storage/emulated/0/Android/data/com.miui.soundrecorder/files",
        "/storage/emulated/0/Android/data/com.android.soundrecorder/files",
        "/storage/emulated/0/MIUI/callrecord",
        "/sdcard/MIUI/callrecord",
        "/storage/emulated/0/CallRecord",
        "/sdcard/CallRecord",
        "/storage/emulated/0/Music/call_rec",
        "/storage/emulated/0/Sounds/call_rec"
    )

    /**
     * 手机可能的录音文件目录
     */
    fun getRecordFiles(): List<String> {
        val parentPath = Environment.getExternalStorageDirectory().absolutePath
        LogUtils.d(TAG, "外部储存相对路径：" + parentPath)
        val list = mutableListOf<String>()
        val potentialPaths = listOf(
            "Music/Recordings/Call Recordings",     //Oppo
            "Recordings/Call Recordings",
            "PhoneRecord",
            "Sounds/CallRecord",    //华为、荣耀、三星
            "HarmonyOS/Sounds/CallRecord",
            "Sounds",
            "CallRecord",
            "CallRecords",
            "MIUI/sound_recorder/call_rec", // 小米
            "Android/media/com.miui.voiceassistantsdk/SoundRecorder/call_rec",  //部分机型新增路径（MIUI 12+/MIUI HyperOS
            "Recordings",   //魅族
            "Record/Call",  //vivo
            "Recordings/Call Recordings",   //一加（OnePlus）/ realme / iQOO(这些品牌通常基于 OPPO 和 vivo 定制系统，路径基本一致)
            "record",
            "Recorder"
        )

        for (path in potentialPaths) {
            val file = File(parentPath, path)
            if (file.exists()) {
                list.add(file.absolutePath)
                LogUtils.d(TAG, "找到可用的路径：" + file.absolutePath)
            }
        }
        return list
    }


    /**
     * 获取手机录音文件列表
     */
    fun getRecordFileList(): List<RecordFileInfo> {
        LogUtils.d(TAG, "开始获取手机录音文件列表")
        val recordDir =
            if (isMiuiSystem()) {
                getMiuiRecordDirectory()
            } else if (isOppoSystem()) {
                getOppoRecordDirectory()
            } else if (isHuaweiDevice()) {
                getHuaWeiRecordDirectory()
            } else {
                null
            }

        if (recordDir == null) {
            LogUtils.w(TAG, "未找到手机录音目录，尝试深度搜索")
            return deepSearchRecordFiles()
        }

        return try {
            val allFiles = recordDir.listFiles()
            LogUtils.d(TAG, "目录下总文件数: ${allFiles?.size ?: 0}")

            val audioFiles = allFiles?.filter { file ->
                file.isFile && file.canRead() && SUPPORTED_AUDIO_FORMATS.any {
                    file.name.lowercase().endsWith(it)
                }
            }

            audioFiles?.map { file ->
                RecordFileInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSize = file.length(),
                    createTime = java.util.Date(file.lastModified()),
                    duration = getAudioDuration(file),
                    fileType = getFileExtension(file.name),
                    remark = "手机通话录音",
                    uploadStatus = UploadStatus.PENDING
                )
            }?.sortedByDescending { it.createTime } ?: emptyList()

        } catch (e: Exception) {
            LogUtils.e(TAG, "获取手机录音文件列表失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 深度搜索OPPO录音文件
     */
    private fun deepSearchRecordFiles(): List<RecordFileInfo> {
        LogUtils.d(TAG, "开始深度搜索手机录音文件")
        val foundFiles = mutableListOf<RecordFileInfo>()

        val searchRoots = arrayOf(
            "/storage/emulated/0",
            "/sdcard",
            Environment.getExternalStorageDirectory().absolutePath
        )

        for (root in searchRoots) {
            try {
                val rootDir = File(root)
                if (rootDir.exists() && rootDir.canRead()) {
                    searchOppoRecursively(rootDir, foundFiles, 0, 3)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "搜索手机录音目录失败: $root")
            }
        }

        return foundFiles.sortedByDescending { it.createTime }
    }

    /**
     * 递归搜索录音文件
     */
    private fun searchOppoRecursively(
        dir: File,
        result: MutableList<RecordFileInfo>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth) return

        try {
            val files = dir.listFiles() ?: return

            for (file in files) {
                if (file.isFile) {
                    val fileName = file.name.lowercase()
                    val isAudioFile = SUPPORTED_AUDIO_FORMATS.any { fileName.endsWith(it) }

                    if (isAudioFile) {
                        result.add(
                            RecordFileInfo(
                                filePath = file.absolutePath,
                                fileName = file.name,
                                fileSize = file.length(),
                                createTime = java.util.Date(file.lastModified()),
                                duration = getAudioDuration(file),
                                fileType = getFileExtension(file.name),
                                remark = "通话录音",
                                uploadStatus = UploadStatus.PENDING
                            )
                        )
                    }
                } else if (file.isDirectory && file.canRead()) {
                    searchOppoRecursively(file, result, currentDepth + 1, maxDepth)
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "递归搜索目录失败: ${dir.absolutePath}")
        }
    }

    /**
     * 指定路径搜索
     */
    fun searchInPath(customPath: String): List<RecordFileInfo> {
        val dir = File(customPath)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return emptyList()
        }

        return try {
            val files = dir.listFiles() ?: return emptyList()
            files.filter { file ->
                file.isFile && file.canRead() &&
                        SUPPORTED_AUDIO_FORMATS.any { file.name.lowercase().endsWith(it) }
            }.map { file ->
                RecordFileInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSize = file.length(),
                    createTime = java.util.Date(file.lastModified()),
                    duration = getAudioDuration(file),
                    fileType = getFileExtension(file.name),
                    remark = "手机通话录音",
                    uploadStatus = UploadStatus.PENDING
                )
            }.sortedByDescending { it.createTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 指定路径和呼出号码搜索
     */
    suspend fun searchInPath(customPath: String, callNum: String): List<RecordFileInfo> {
        LogUtils.d("主页", "searchInPath customPath：${customPath}")
        val dir = File(customPath)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return emptyList()
        }
        return try {
            val startTime = System.currentTimeMillis()
            withContext(Dispatchers.IO) {

                val names = dir.list { _, name ->
                    name.contains(callNum, ignoreCase = true) &&
                            SUPPORTED_AUDIO_FORMATS.any { name.endsWith(it, ignoreCase = true) }
                } ?: emptyArray()
                val filterResult = names
                    .asSequence()
                    .map { File(dir, it) }
                    .filter { it.isFile && it.canRead() }
                    .toList().sortedByDescending { it.lastModified() }

                LogUtils.d(
                    "主页",
                    "test 耗时:${(System.currentTimeMillis() - startTime) / (1000)}秒"
                )
                if (filterResult.size > 0) {
                    mutableListOf(
                        RecordFileInfo(
                            filePath = filterResult[0].absolutePath,
                            fileName = filterResult[0].name,
                            fileSize = filterResult[0].length(),
                            createTime = java.util.Date(filterResult[0].lastModified()),
                            duration = getAudioDuration(filterResult[0]),
                            fileType = getFileExtension(filterResult[0].name),
                            remark = "手机通话录音",
                            uploadStatus = UploadStatus.PENDING
                        )
                    )
                } else {
                    emptyList<RecordFileInfo>()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取音频文件时长
     */
    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取音频时长失败: ${file.name}")
            0L
        }
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    /**
     * 获取OPPO录音目录
     */
    private fun getOppoRecordDirectory(): File? {
        for (path in OPPO_RECORD_PATHS) {
            LogUtils.d(TAG, "检查OPPO录音路径: $path")
            val dir = File(path)
            LogUtils.d(TAG, "检查路径: $path")
            LogUtils.d(TAG, "目录存在: ${dir.exists()}")
            LogUtils.d(TAG, "是否为目录: ${dir.isDirectory}")
            LogUtils.d(TAG, "是否可读: ${dir.canRead()}")

            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                LogUtils.d(TAG, "找到可用的OPPO录音目录: $path")
                return dir
            }
        }
        return null
    }

    /**
     * 获取华为录音目录
     */
    private fun getHuaWeiRecordDirectory(): File? {
        for (path in HUAWEI_RECORD_PATHS) {
            LogUtils.d(TAG, "检查华为录音路径: $path")
            val dir = File(path)
            LogUtils.d(TAG, "检查路径: $path")
            LogUtils.d(TAG, "目录存在: ${dir.exists()}")
            LogUtils.d(TAG, "是否为目录: ${dir.isDirectory}")
            LogUtils.d(TAG, "是否可读: ${dir.canRead()}")

            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                LogUtils.d(TAG, "找到可用的华为录音目录: $path")
                return dir
            }
        }
        return null
    }

    /**
     * 获取小米（MIUI）录音目录
     */
    private fun getMiuiRecordDirectory(): File? {
        for (path in MIUI_RECORD_PATHS) {
            LogUtils.d(TAG, "检查小米录音路径: $path")
            val dir = File(path)
            LogUtils.d(TAG, "检查路径: $path")
            LogUtils.d(TAG, "目录存在: ${dir.exists()}")
            LogUtils.d(TAG, "是否为目录: ${dir.isDirectory}")
            LogUtils.d(TAG, "是否可读: ${dir.canRead()}")

            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                LogUtils.d(TAG, "找到小米可用目录: $path")
                // 尝试列出文件来验证权限
                try {
                    val files = dir.listFiles()
                    LogUtils.d(TAG, "目录下文件数量: ${files?.size ?: 0}")
                    if (files != null) {
                        for (file in files.take(5)) { // 只打印前5个文件
                            LogUtils.d(
                                TAG,
                                "文件: ${file.name}, 大小: ${file.length()}, 可读: ${file.canRead()}"
                            )
                        }
                    }
                    return dir
                } catch (e: Exception) {
                    LogUtils.e(TAG, "访问小米目录失败: $path, 错误: ${e.message}")
                }
            }
        }
        return null
    }

    /**
     * 检查存储权限
     */
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /*******************  厂商校验  **********************/
    /**
     * 检查是否为OPPO系统（直接检测）
     */
    fun isOppoSystem(): Boolean {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        return brand.contains("oppo") || manufacturer.contains("oppo")
    }

    /**
     * 检查是否为MIUI系统
     */
    fun isMiuiSystem(): Boolean {
        return try {
            // 检查厂商和品牌
            val isXiaomiDevice = isXiaomiDevice()

            // 检查系统标识中是否包含MIUI特征
            val hasMiuiIdentifier = Build.DISPLAY.contains("MIUI", ignoreCase = true) ||
                    Build.FINGERPRINT.contains("MIUI", ignoreCase = true) ||
                    Build.ID.contains("MIUI", ignoreCase = true) ||
                    Build.TAGS.contains("MIUI", ignoreCase = true)

            val result = isXiaomiDevice || hasMiuiIdentifier
            LogUtils.d(
                TAG,
                "MIUI系统检测结果: $result (设备: $isXiaomiDevice, 系统标识: $hasMiuiIdentifier)"
            )

            result
        } catch (e: Exception) {
            LogUtils.e(TAG, "检测MIUI系统失败: ${e.message}")
            false
        }
    }

    /**
     * 检查是否为小米手机（直接检测）
     */
    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        LogUtils.d(TAG, "设备制造商: $manufacturer, 品牌: $brand")

        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                manufacturer.contains("redmi") || brand.contains("redmi") ||
                manufacturer.contains("mi") || brand.contains("mi")
    }

    /**
     * 检查是否为华为手机
     */
    fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        LogUtils.d(TAG, "设备制造商: $manufacturer, 品牌: $brand")
        return manufacturer.contains("huawei") || brand.contains("huawei")
    }
}