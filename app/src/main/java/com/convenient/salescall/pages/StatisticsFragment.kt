package com.convenient.salescall.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.convenient.salescall.activity.LoginActivity
import com.convenient.salescall.adapter.UserVoiceInfoAdapter
import com.convenient.salescall.databinding.FragmentStatisticsBinding
import com.convenient.salescall.datas.UserVoiceInfo
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.network.NetworkManager
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.viewmodel.StatisticsViewModel

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mViewModel: StatisticsViewModel
    private val apiService by lazy {
        NetworkManager.getInstance(requireContext()).createService(ApiService::class.java)
    }

    private val localDataUtils: LocalDataUtils by lazy {
        LocalDataUtils()
    }

    private lateinit var dataList: MutableList<UserVoiceInfo>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        mViewModel = StatisticsViewModel(apiService)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        initViews()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // Fragment被show出来
            mViewModel.getStatisticsData()
        } else {
            // Fragment被hide
        }
    }

    private fun initData() {
        dataList = mutableListOf<UserVoiceInfo>()

//        val datas = listOf(
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 1),
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 2),
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 3),
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 4),
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 4),
//            UserVoiceInfo(2, "测试员", 1, 0, 1, 0, "0时0分0秒", 4),
//        )
//        (dataList as MutableList<UserVoiceInfo>).addAll(datas)
//        if (dataList.size > 0) {
//            dataList[0].userName.let {
//                binding.userAccount.text = "用户：$it"
//            }
//        }
    }

    private fun initViews() {
        val adapter = UserVoiceInfoAdapter(requireContext(), dataList)
        binding.listView.adapter = adapter

        binding.logoutBtn.setOnClickListener {
            showLogoutDialog()
        }

        mViewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "登出成功", Toast.LENGTH_SHORT).show()
                localDataUtils.setLogin(false)
                startActivity(Intent(requireActivity(), LoginActivity::class.java))
                requireActivity().finish()
            }.onFailure { e ->
                e.message?.let {
                    Toast.makeText(requireContext(), "${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        mViewModel.statisticsResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { bean ->
                if (bean.isNotEmpty()) {
                    dataList.clear()
                    dataList.addAll(bean)
                    if (dataList.size > 0) {
                        if (dataList[0].userName == null) {
                            binding.userAccount.text = "用户：匿名"
                        } else {
                            dataList[0].userName.let {
                                if (it == "null") {
                                    binding.userAccount.text = "用户：匿名"
                                } else
                                    binding.userAccount.text = "用户：$it"
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    LogUtils.d(TAG, "请求数据为空")
                }
            }.onFailure { e ->
                LogUtils.d(TAG, "请求统计数据失败${e.message}")
                if (e.message.equals("登录过期")) {
                    localDataUtils.setLogin(false)
                    Toast.makeText(requireContext(), "登录失效", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(
                            requireActivity(), LoginActivity::class.java
                        )
                    )
                    requireActivity().finish()
                } else {
                    LogUtils.d(TAG, "请求统计数据失败${e.message}")
                }
            }
        }

    }

    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("确认退出吗")
        builder.setMessage("您确定要退出登录吗？")
        builder.setPositiveButton("确认") { dialog, _ ->
            // 这里写退出登录的逻辑
            mViewModel.onLogout()
            dialog.dismiss()
            // 例如：finish() 或跳转到登录页
        }
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    companion object {
        private const val TAG = "StatisticsFragment"
    }
}