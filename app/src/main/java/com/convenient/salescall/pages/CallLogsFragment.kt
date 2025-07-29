package com.convenient.salescall.pages

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.convenient.salescall.activity.LoginActivity
import com.convenient.salescall.activity.MainPageActivity
import com.convenient.salescall.activity.MainPageActivity.Companion
import com.convenient.salescall.adapter.CallLogAdapter
import com.convenient.salescall.databinding.FragmentRecordBinding
import com.convenient.salescall.datas.CallLogItem
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.network.NetworkManager
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.viewmodel.CallLogViewModel

class CallLogsFragment : Fragment() {
    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private val apiService by lazy {
        NetworkManager.getInstance(this.requireActivity().applicationContext)
            .createService(ApiService::class.java)
    }
    private lateinit var mViewModel: CallLogViewModel
    private lateinit var callLogAdapter: CallLogAdapter
    private val callLogList = mutableListOf<CallLogItem>()
    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = CallLogViewModel(apiService)

        mViewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Log.d(TAG, "uploadResult: 上传成功")
            }.onFailure { e ->
                if (e.message.equals("登录过期")) {
                    requireActivity().startActivity(
                        Intent(
                            requireActivity(), LoginActivity::class.java
                        )
                    )
                    requireActivity().finish()
                } else {
                    Log.d(TAG, "uploadResult: 上传失败")
                }
            }
        }

        setupListView()
    }

    private fun setupListView() {
        callLogAdapter = CallLogAdapter(requireContext(), callLogList)
        binding.lvCallLogs.adapter = callLogAdapter
        binding.swipeRefresh.setOnRefreshListener {
            // 刷新数据
            readCallLogs()
        }
    }


    override fun onResume() {
        super.onResume()
        readCallLogs()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Fragment被show出来
            readCallLogs()
        } else {
            // Fragment被hide
        }
    }

    fun readCallLogs() {
        LogUtils.d("测试权限", "readCallLogs")
        // 1. 判断是否有 READ_CALL_LOG 权限
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtils.d(TAG, "没有 READ_CALL_LOG 权限")
            // 可以在这里请求权限，或者直接 return
            return
        }
        val contentResolver: ContentResolver? = activity?.contentResolver
        if (contentResolver == null) {
            LogUtils.d(TAG, "contentResolver 获取失败")
            return
        }

        val uri: Uri = CallLog.Calls.CONTENT_URI
        val projection: Array<String> = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION
        )

        val sortOrder: String = CallLog.Calls.DATE + " DESC"
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, sortOrder)

        if (cursor != null) {
            try {
                callLogList.clear() // 清空旧数据

                val numberIndex: Int = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex: Int = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex: Int = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex: Int = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val number: String =
                        if (numberIndex != -1) cursor.getString(numberIndex) else "未知号码"
                    val type: Int =
                        if (typeIndex != -1) cursor.getInt(typeIndex) else CallLog.Calls.INCOMING_TYPE
                    val date: Long = if (dateIndex != -1) cursor.getLong(dateIndex) else 0
                    val duration: Int = if (durationIndex != -1) cursor.getInt(durationIndex) else 0

                    // 只添加呼出电话
                    if (type == CallLog.Calls.OUTGOING_TYPE && date > localDataUtils.getFistInstallTime()) {
                        callLogList.add(
                            CallLogItem(
                                number = number,
                                type = getTypeLabel(type),
                                date = date,
                                duration = duration
                            )
                        )
                    }
                }
                if (callLogList.isEmpty()) {
                    _binding?.nullDataRl?.visibility = View.VISIBLE
                    if (isVisible) {
                        Toast.makeText(requireContext(), "未检索到有效数据", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    _binding?.nullDataRl?.visibility = View.GONE
                }

                // 通知适配器数据已更新
                callLogAdapter.notifyDataSetChanged()
            } finally {
                cursor.close()
                // 停止刷新动画
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun getTypeLabel(type: Int): String {
        return when (type) {
            CallLog.Calls.OUTGOING_TYPE -> "呼出"
            else -> "其他"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "RecordFragment"
    }
}