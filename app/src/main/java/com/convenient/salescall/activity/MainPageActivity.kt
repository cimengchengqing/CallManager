package com.convenient.salescall.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.convenient.salescall.R
import com.convenient.salescall.call_db.CallRecord
import com.convenient.salescall.call_db.CallRecordRepository
import com.convenient.salescall.datas.RecordFileInfo
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.network.NetworkManager
import com.convenient.salescall.pages.CallLogsFragment
import com.convenient.salescall.pages.DialFragment
import com.convenient.salescall.pages.StatisticsFragment
import com.convenient.salescall.receiver.MessageCenter
import com.convenient.salescall.service.CallStateService
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.tools.PermissionHelper
import com.convenient.salescall.tools.PhoneRecordFileUtils
import com.convenient.salescall.viewmodel.CallLogViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainPageActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "主页"
        private const val PERMISSION_REQUEST_CODE = 123
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_NUMBERS
        )
    }

    private val dialFragment = DialFragment()

    //    private val recordsFragment = RecordsFragment()
    private val statisticsFragment = StatisticsFragment()
    private val recordFragment = CallLogsFragment()
    private var activeFragment: Fragment = dialFragment

    private lateinit var mViewModel: CallLogViewModel
    private val apiService by lazy {
        NetworkManager.getInstance(applicationContext)
            .createService(ApiService::class.java)
    }
    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }

    private val callRepository: CallRecordRepository by lazy {
        CallRecordRepository(applicationContext)
    }

    //通信相关类
    private lateinit var localReceiver: BroadcastReceiver

    private var allGranted: Boolean = false //标记所有权限是否都授予

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main_page)

        localReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "com.call.ACTION_MSG" -> {
                        val msg = intent.getStringExtra("msg")
                        LogUtils.d(TAG, "收到消息$msg")
                        msg?.let {
                            MessageCenter.post(it)
                        }
                    }

                    "com.call.ACTION_DOWN" -> {
                        // 这里处理 ACTION_DOWN 的逻辑
                        LogUtils.d(TAG, "收到通话结束的通知")
//                        lifecycleScope.launch {
//                            delay(500) // 延迟500毫秒
//                            withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(
//                                allOf = [android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_PHONE_NUMBERS, android.Manifest.permission.READ_PHONE_STATE]
//                            ) {
//                                readCallLogs()
//                            }
//                        }
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(localReceiver, IntentFilter("com.call.ACTION_MSG"))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(localReceiver, IntentFilter("com.call.ACTION_DOWN"))

        mViewModel = CallLogViewModel(apiService)

        mViewModel.uploadCallLogResult.observe(this) { result ->
            result.onSuccess { bean ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        bean.isUploaded = true
                        callRepository.update(bean)
                        LogUtils.d(TAG, "通话记录上传成功(含录音文件)")
                    }
                }
            }.onFailure { e ->
                if (e.message.equals("登录过期")) {
                    localDataUtils.setLogin(false)
                    Toast.makeText(applicationContext, "登录失效", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(
                            this,
                            LoginActivity::class.java
                        )
                    )
                    finish()
                } else {
                    LogUtils.d(TAG, "uploadResult: 上传失败(录音文件)")
                }
            }
        }

        // 初始化试图
        initViews()
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        allGranted = true
        registerService()
    }

    @RequiresPermission(allOf = [Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE])
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")

        lifecycleScope.launch {
            delay(500) // 延迟500毫秒
            withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(
                allOf = [android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_PHONE_NUMBERS, android.Manifest.permission.READ_PHONE_STATE]
            ) {
                readCallLogs()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        stopService(Intent(this, CallStateService::class.java))
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver)
    }

    private fun initViews() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val fragmentManager = supportFragmentManager

        // 预先添加所有Fragment
        fragmentManager.beginTransaction()
            .add(R.id.fragment_container, dialFragment, "dial")
            .add(R.id.fragment_container, recordFragment, "record").hide(recordFragment)
            .add(R.id.fragment_container, statisticsFragment, "statistics").hide(statisticsFragment)
            .commit()

        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_dial -> switchFragment(dialFragment)
                R.id.menu_statistics -> switchFragment(statisticsFragment)
                R.id.menu_history -> switchFragment(recordFragment)
            }
            true
        }
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment != target) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
        }
    }

    /**
     * 检查必须要权限
     */
    private fun checkPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                LogUtils.d(TAG, "未授予的权限: " + permission)
                return false
            }
        }
        return true
    }

    /**
     * 请求权限
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    /**
     * 权限请求回调处理
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (!allGranted) {
                LogUtils.d(TAG, "需要所有权限才能正常使用功能 ")
            }
            registerService()
        }
    }

    private fun registerService() {
        val serviceIntent = Intent(this, CallStateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * 读取通话记录
     */
    @RequiresPermission(allOf = [Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE])
    suspend fun readCallLogs() {
        LogUtils.d(TAG, "readCallLogs")
        // 1. 判断是否有 READ_CALL_LOG 权限
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtils.d(TAG, "没有 READ_CALL_LOG 权限")
            // 可以在这里请求权限，或者直接 return
            return
        }
        // 2. 判断是否有 READ_PHONE_STATE 权限
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtils.d(TAG, "没有 READ_PHONE_STATE 权限")
            // 可以在这里请求权限，或者直接 return
            return
        }

        // 检查内存读取权限
        if (!PermissionHelper.hasStoragePermission(applicationContext)) {
            LogUtils.d(TAG, "没有内存读取权限")
            showPermissionDialog()
            return
        }

        //开始去查询
        val contentResolver: ContentResolver? = this.contentResolver
        if (contentResolver == null) {
            LogUtils.d(TAG, "contentResolver 获取失败")
            return
        }

        val uri: Uri = CallLog.Calls.CONTENT_URI
        val projection: Array<String> = arrayOf(
            CallLog.Calls._ID,         // 加入ID字段
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val sortOrder: String = CallLog.Calls.DATE + " ASC" //升序查找

        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idIndex: Int = cursor.getColumnIndex(CallLog.Calls._ID)         // 获取ID下标
            val numberIndex: Int = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex: Int = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex: Int = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex: Int = cursor.getColumnIndex(CallLog.Calls.DURATION)

            // 如果idIndex为-1，直接结束
            if (idIndex == -1) {
                LogUtils.d(TAG, "查询通话记录失败")
                return
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val number: String =
                    if (numberIndex != -1) cursor.getString(numberIndex) else "未知号码"
                val type: Int =
                    if (typeIndex != -1) cursor.getInt(typeIndex) else CallLog.Calls.INCOMING_TYPE
                val date: Long = if (dateIndex != -1) cursor.getLong(dateIndex) else 0
                val duration: Int = if (durationIndex != -1) cursor.getInt(durationIndex) else 0

                // 只添加呼出电话
                if (type == CallLog.Calls.OUTGOING_TYPE) {
                    val logTime = localDataUtils.getFistInstallTime()
                    if (date > logTime) {
                        LogUtils.d(TAG, "查询的通话ID${id}")
                        val readRecord = callRepository.getByCallLogId(id)
                        //还未插入本地的数据库则插入
                        if (readRecord == null) {
                            LogUtils.d(TAG, "通话${id}第一次插入")
                            var path = ""   //录音文件地址
                            if (duration != 0) {
                                //通话已接通则去查找匹配的录音文件
                                path = findRecordFile(number, date)
                            }

                            val telephonyManager =
                                applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                            var phoneNumber = telephonyManager.line1Number ?: "无法获取号码"
                            phoneNumber = phoneNumber.replace("+86", "")

                            val callRecord = CallRecord(
                                id,
                                UUID.randomUUID().toString(),
                                phoneNumber,
                                number,
                                date,
                                duration != 0,
                                date + duration * 1000L,
                                duration * 1000L,
                                false,
                                path
                            )
                            //插入一条新数据
                            callRepository.insert(callRecord)
                            //插入完成后执行上传
                            mViewModel.performUploadCallLog(callRecord)
                        } else {
                            if (!readRecord.isUploaded) {
                                LogUtils.d(TAG, "通话${id}未上传，请求上传")
                                if (readRecord.isConnected && readRecord.recordFilePath.isEmpty()) {
                                    var path = ""   //录音文件地址
                                    if (duration != 0) {
                                        //通话已接通则去查找匹配的录音文件
                                        path = findRecordFile(number, date)
                                    }
                                    readRecord.recordFilePath = path
                                }
                                //处理未上传的情况
                                mViewModel.performUploadCallLog(readRecord)
                            } else {
                                LogUtils.d(TAG, "通话${id} 已上传")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 根据拨打的电话号码查找录音文件并返回文件位置
     */
    private suspend fun findRecordFile(callNumber: String, date: Long): String {
        var path = ""
        try {
            // 在IO线程中获取录音文件
            withContext(Dispatchers.Default) {
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
                allFiles.sortedByDescending { it.createTime } // 按时间排序
                if (allFiles.isNotEmpty()) {
                    allFiles.forEach { file ->
                        //录音文件名包含被呼叫的电话，以及创建时间是在呼叫的某个时间范围内
                        LogUtils.d(TAG, "fileName：${file.fileName}______callNumber:$callNumber")
                        if (file.fileName.contains(callNumber)
//                            && isWithinRange(
//                                file.createTime.time,
//                                date
//                            )
                        ) {
                            path = file.filePath
                            return@forEach
                        }
                    }
                } else {
                    if (!PermissionHelper.hasStoragePermission(applicationContext)) {
                        showPermissionDialog()
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "加载录音文件失败: ${e.message}")
            // 如果是权限问题，提供解决方案
            if (e.message?.contains("permission", true) == true) {
                showPermissionDialog()
            }
        }
        LogUtils.d(TAG, "$callNumber 对应的录音文件位置：$path")
        return path
    }

    /**
     * 去打开管理所有文件的权限
     */
    private fun showPermissionDialog() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = ("package:$packageName").toUri()
        startActivity(intent)
    }

    /**
     * 判断 time1 是否在 time2 的前后30秒（60秒区间）内
     * @param time1 需要判断的时间（毫秒）
     * @param time2 参考时间（毫秒）
     * @param rangeSeconds 允许的前后秒数，默认30秒
     * @return Boolean
     */
    fun isWithinRange(time1: Long, time2: Long, rangeSeconds: Int = 60): Boolean {
        val diff = time1 - time2
        return diff <= rangeSeconds * 1000 && diff >= -2000
    }
}
