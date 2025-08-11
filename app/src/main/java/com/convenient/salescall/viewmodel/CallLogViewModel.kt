package com.convenient.salescall.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenient.salescall.call_db.CallRecord
import com.convenient.salescall.datas.CallLogUploadBean
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CallLogViewModel(private val apiService: ApiService) : ViewModel() {
    private val uploadMutex = Mutex() // 协程互斥锁
    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }
    private val _uploadResult = MutableLiveData<Result<ApiService.AppResponse<Unit>>>()
    val uploadResult: LiveData<Result<ApiService.AppResponse<Unit>>> = _uploadResult

    private val _recordResult = MutableLiveData<Result<RecordFileInfo>>()
    val recordResult: LiveData<Result<RecordFileInfo>> = _recordResult

    private val _uploadCallLogResult = MutableLiveData<Result<CallRecord>>()
    val uploadCallLogResult: LiveData<Result<CallRecord>> = _uploadCallLogResult

    /**
     * 上传单条通话信息
     */
    fun performLogBySingle(logBean: CallLogUploadBean) {
        viewModelScope.launch(Dispatchers.IO) {
            uploadMutex.withLock { // 保证串行执行
                try {
                    val response = apiService.uploadCallLog(
                        localDataUtils.getAuthCookie(),
                        logBean
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            if (it.code == 0) {
                                _uploadResult.postValue(Result.success(body))
                                if (logBean.callStartTime > localDataUtils.getLastTimeLog()) {
                                    localDataUtils.saveLastTimeLog(logBean.callStartTime)
                                }
                            } else if (it.code == 401) {
                                _uploadResult.postValue(Result.failure(Exception("登录过期")))
                                localDataUtils.clearAuthCookie()
                            } else {
                                _uploadResult.postValue(Result.failure(Exception("请求出错: ${it.msg}")))
                            }
                        }

                    } else {
                        _uploadResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                    }
                } catch (e: Exception) {
                    _uploadResult.postValue(Result.failure(e))
                }
            }
        }
    }

    /**
     * 上传录音文件
     */
    fun performUploadRecordFile(recordFile: RecordFileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            uploadMutex.withLock { // 保证串行执行
                val file = File(recordFile.filePath)
                if (!file.exists()) {
                    _recordResult.postValue(Result.failure(Exception("文件不存在")))
                } else {
                    // 创建文件部分
                    val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData(
                        "recordFile",
                        recordFile.fileName,
                        requestFile
                    )

                    val uuid = localDataUtils.generateFileUUID(file).toString()
                    LogUtils.d("测试UUID", "上传文件的UUID:$uuid ")
                    val response = apiService.uploadRecordFile(
                        localDataUtils.getAuthCookie(),
                        uuid.toRequestBody("text/plain".toMediaTypeOrNull()),
                        filePart
                    )

                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            if (it.code == 0) {
                                _recordResult.postValue(Result.success(recordFile))
                                if (recordFile.createTime.time > localDataUtils.getLastTimeRecording()) {
                                    localDataUtils.saveLastTimeRecording(recordFile.createTime.time)
                                }
                            } else if (it.code == 401) {
                                _recordResult.postValue(Result.failure(Exception("登录过期")))
                                localDataUtils.clearAuthCookie()
                            } else {
                                _recordResult.postValue(Result.failure(Exception("请求出错: ${it.msg}")))
                            }
                        }

                    } else {
                        _recordResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                    }
                }
            }
        }
    }

    /**
     * 上传单条通话信息（含录音）
     */
    fun performUploadCallLog(logBean: CallRecord) {
        Log.d("主页", "上传通话信息: ${logBean.callLogId}")
        viewModelScope.launch(Dispatchers.IO) {
            uploadMutex.withLock { // 保证串行执行
                try {
                    val response = apiService.uploadCallLogs(
                        cookie = localDataUtils.getAuthCookie(),
                        id = logBean.uuid.toRequestBody(),
                        callStartTime = logBean.callStartTime.toRequestBody(),
                        isConnected = logBean.isConnected.toRequestBody(),
                        callEndTime = logBean.callEndTime.toRequestBody(),
                        durationMs = logBean.durationMs.toRequestBody(),
                        callerNumber = logBean.callerNumber.toRequestBody(),
                        mobile = logBean.mobile.toRequestBody(),
                        recordFile = createFilePart(
                            "recordFile",
                            logBean.recordFilePath
                        ) // filePath为null或空时，file参数为null
                    )

                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            if (it.code == 0) {
                                _uploadCallLogResult.postValue(Result.success(logBean))
//                                if (logBean.callStartTime > localDataUtils.getLastTimeLog()) {
//                                    localDataUtils.saveLastTimeLog(logBean.callStartTime)
//                                }
                            } else if (it.code == 401) {
                                _uploadCallLogResult.postValue(Result.failure(Exception("登录过期")))
                                localDataUtils.clearAuthCookie()
                            } else {
                                _uploadCallLogResult.postValue(Result.failure(Exception("请求出错: ${it.msg}")))
                            }
                        } ?: _uploadCallLogResult.postValue(Result.failure(Exception("请求体为空")))
                    } else {
                        _uploadCallLogResult.postValue(Result.failure(Exception("请求失败: ${response.code()}")))
                    }
                } catch (e: Exception) {
                    _uploadCallLogResult.postValue(Result.failure(e))
                }
            }
        }
    }
}

// 构造RequestBody
fun String.toRequestBody() = RequestBody.create("text/plain".toMediaTypeOrNull(), this)
fun Long.toRequestBody() = RequestBody.create("text/plain".toMediaTypeOrNull(), this.toString())
fun Boolean.toRequestBody() =
    RequestBody.create("text/plain".toMediaTypeOrNull(), this.toString())

// 构造文件Part（可为null）
fun createFilePart(paramName: String, filePath: String?): MultipartBody.Part? {
    return if (!filePath.isNullOrEmpty()) {
        val file = File(filePath)
        val requestFile = RequestBody.create("audio/*".toMediaTypeOrNull(), file)
        MultipartBody.Part.createFormData(paramName, file.name, requestFile)
    } else {
        null
    }
}