package com.convenient.salescall.pages

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.convenient.salescall.adapter.RecordFileAdapter
import com.convenient.salescall.databinding.FragmentRecordsBinding
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.datas.UploadStatus
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.tools.PermissionHelper
import com.convenient.salescall.tools.PhoneRecordFileUtils
import com.convenient.salescall.tools.UploadRecordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordsFragment : Fragment() {
    // 声明绑定变量
    private var _binding: FragmentRecordsBinding? = null

    // 非空属性委托
    private val binding get() = _binding!!

    private lateinit var adapter: RecordFileAdapter
    private var recordFiles = mutableListOf<RecordFileInfo>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var uploadRecordManager: UploadRecordManager
    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 使用ViewBinding
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化上传记录管理器
        uploadRecordManager = UploadRecordManager(requireContext())

//        uploadRecordManager.queryFiles()  //test；查看上传录音文件状态信息的数据库
        // 初始化视图
        setupViews()
//         检查权限并加载数据
        checkPermissionsAndLoadData()
    }

    private fun setupViews() {
        // 初始化适配器
        adapter = RecordFileAdapter(requireContext(), recordFiles)
        binding.listRecords.adapter = adapter

        // 设置下拉刷新
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadRecordFiles()

//            val localIntent = Intent("com.call.ACTION_MSG").apply {
//                putExtra("msg", "15086693915")
//            }
//            LocalBroadcastManager.getInstance(CallApp.appContext).sendBroadcast(localIntent)
        }

        // 设置列表项点击事件
        binding.listRecords.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val selectedFile = recordFiles[position]
                showFileDetailsDialog(selectedFile)
            }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkPermissionsAndLoadData() {
        if (!PermissionHelper.hasStoragePermission(requireContext())) {
            showPermissionDialog()
        } else {
            loadRecordFiles()
            LogUtils.d(TAG, PermissionHelper.getPermissionStatus(requireContext()))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadRecordFiles() {
        coroutineScope.launch {
            try {
                // 显示加载状态
                binding.swipeRefreshLayout.isRefreshing = true
                // 在IO线程中获取录音文件
                val files = withContext(Dispatchers.IO) {
                    // 使用智能录音文件搜索
//                    val smartFiles = ScopedStorageRecordFileUtil.getRecordFiles(requireContext())
//                    // 如果智能搜索没有结果，尝试传统方法
//                    val allFiles = if (smartFiles.isNotEmpty()) {
//                        smartFiles
//                    } else {
//                        // 备用方案：传统文件搜索
//                        MiuiRecordFileUtil.getRecordFileList() +
//                                MiuiRecordFileUtil.deepSearchRecordFiles()
//                    }

                    //寻找可用的路径
                    val feasibleDir = PhoneRecordFileUtils.getRecordFiles()
                    val allFiles: List<RecordFileInfo> =
                        if (feasibleDir.isEmpty()) {
                            PhoneRecordFileUtils.getRecordFileList()
                        } else {
                            //将每个输入的路径映射到一个 List<MiuiRecordFileInfo>，然后合并返回
                            feasibleDir.flatMap { path ->
                                PhoneRecordFileUtils.searchInPath(path)
                            }
                        }

                    // 为每个文件查询上传状态并去重
                    allFiles.filter { it.createTime.time >= localDataUtils.getFistInstallTime() } // 只保留创建时间大于等于指定时间的
                        .distinctBy { it.filePath }.map { file ->
                            val uploadStatus = when {
                                uploadRecordManager.isFileUploaded(file.filePath) -> UploadStatus.UPLOAD_SUCCESS
                                uploadRecordManager.isFileUploading(file.filePath) -> UploadStatus.UPLOADING
                                uploadRecordManager.isFileFailed(file.filePath) -> UploadStatus.UPLOAD_FAILED
                                else -> UploadStatus.PENDING
                            }
                            // 使用工厂方法创建带上传状态的文件信息
                            createMiuiRecordFileInfoWithStatus(file, uploadStatus)
                        }.sortedByDescending { it.createTime } // 按时间排序
                }

                // 更新UI
                recordFiles.clear()
                recordFiles.addAll(files)

                adapter.updateData(recordFiles)
                // 显示结果统计
                showLoadResult(files)
            } catch (e: Exception) {
                LogUtils.e(TAG, "加载录音文件失败: ${e.message}")
                // 如果是权限问题，提供解决方案
                if (e.message?.contains("permission", true) == true) {
                    showPermissionDialog()
                }
            } finally {
                // 隐藏加载状态
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /**
     * 去打开管理所有文件的权限
     */
    private fun showPermissionDialog() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = ("package:" + requireActivity().packageName).toUri()
        startActivity(intent)
    }

    /**
     * 创建带上传状态的录音文件信息
     */
    private fun createMiuiRecordFileInfoWithStatus(
        originalFile: RecordFileInfo,
        uploadStatus: UploadStatus
    ): RecordFileInfo {
        val newFile = RecordFileInfo(
            filePath = originalFile.filePath,
            fileName = originalFile.fileName,
            fileSize = originalFile.fileSize,
            createTime = originalFile.createTime,
            duration = originalFile.duration,
            fileType = originalFile.fileType,
            remark = originalFile.remark,
            uploadStatus = uploadStatus
        )

        originalFile.getContentUri()?.let { uri ->
            newFile.setContentUri(uri)
        }
        return newFile
    }

    /**
     * 显示加载结果统计
     */
    private fun showLoadResult(files: List<RecordFileInfo>) {
        if (files.isEmpty()) {
            val hasPermission = PermissionHelper.hasStoragePermission(requireContext())
            val message = if (!hasPermission) {
                showPermissionDialog()
                "未找到录音文件，可能是权限问题"
            } else {
                "未找到录音文件，请确认已开启通话录音功能"
            }
            showToast(message)
        } else {
            val uploadedCount = files.count { it.uploadStatus == UploadStatus.UPLOAD_SUCCESS }
            val callRecordCount = files.count { it.isCallRecord() }

            val message = buildString {
                append("找到 ${files.size} 个录音文件")
                if (callRecordCount > 0) {
                    append("，其中 ${callRecordCount} 个通话录音")
                }
                append("，已上传 ${uploadedCount} 个")
            }
            LogUtils.d(TAG, message)
        }
    }

    /**
     * 显示文件详情对话框
     */
    private fun showFileDetailsDialog(recordFile: RecordFileInfo) {
        val details = buildString {
            append("文件名：${recordFile.fileName}\n")
            append("大小：${recordFile.getFormattedSize()}\n")
            append("时间：${recordFile.getFormattedTime()}\n")
            if (recordFile.duration > 0) {
                append("时长：${recordFile.getFormattedDuration()}\n")
            }
            append("类型：${recordFile.fileType}\n")
            append("通话录音：${if (recordFile.isCallRecord()) "是" else "否"}\n")
            recordFile.getCallTarget()?.let { target ->
                append("通话对象：$target\n")
            }
            append("上传状态：${recordFile.getUploadStatusText()}\n")
            append("访问方式：${if (recordFile.isUsingContentUri()) "ContentUri" else "File"}\n")
            append("路径：${recordFile.filePath}")
        }

        val options = arrayOf("播放录音", "复制到应用", "上传文件", "分享文件")

        AlertDialog.Builder(requireContext())
            .setTitle("录音文件详情")
            .setMessage(details)
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消协程
        coroutineScope.cancel()
        // 清除绑定引用，避免内存泄漏
        _binding = null
    }

    companion object {
        private const val TAG = "RecordsFragment"
    }
} 