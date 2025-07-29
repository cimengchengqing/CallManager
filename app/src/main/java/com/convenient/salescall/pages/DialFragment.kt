package com.convenient.salescall.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.convenient.salescall.databinding.FragmentDialBinding
import com.convenient.salescall.receiver.MessageCenter
import com.convenient.salescall.tools.LogUtils

class DialFragment : Fragment() {

    private var _binding: FragmentDialBinding? = null
    private val binding get() = _binding!!
    private var phoneNumber = StringBuilder()

    private val messageListener: (String) -> Unit = { msg ->
        // 这里处理消息
        LogUtils.d("拨号", "收到拨号请求$msg")
        phoneNumber.clear().append(msg)
        updatePhoneNumberDisplay()
        makePhoneCall(msg)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialPad()
        MessageCenter.addListener(messageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        MessageCenter.removeListener(messageListener)
    }

    private fun setupDialPad() {
        // 设置数字按钮点击事件
        val numberButtons = arrayOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                appendNumber(index.toString())
            }
        }

        // 设置特殊按钮点击事件
        binding.btnStar.setOnClickListener { appendNumber("*") }
        binding.btnPound.setOnClickListener { appendNumber("#") }

        // 设置删除按钮点击事件
        binding.btnDelete.setOnClickListener {
            if (phoneNumber.isNotEmpty()) {
                phoneNumber.deleteCharAt(phoneNumber.length - 1)
                updatePhoneNumberDisplay()
            }
        }

        // 设置拨号按钮点击事件
        binding.btnCall.setOnClickListener {
            if (phoneNumber.isNotEmpty()) {
                makePhoneCall(phoneNumber.toString())
            } else {
                Toast.makeText(context, "请输入电话号码", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun appendNumber(number: String) {
        phoneNumber.append(number)
        updatePhoneNumberDisplay()
    }

    private fun updatePhoneNumberDisplay() {
        binding.tvPhoneNumber.text = phoneNumber.toString()
        if (binding.tvPhoneNumber.text.isNotEmpty()) {
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        val telephonyManager =
            requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.callState == TelephonyManager.CALL_STATE_IDLE) {
            // 可以拨号
            try {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:$phoneNumber")
                startActivity(intent)
            } catch (e: SecurityException) {
                Toast.makeText(context, "请授予拨打电话权限", Toast.LENGTH_SHORT).show()
            }
        } else {
            LogUtils.d("拨号", "当前正在通话中，请稍后再拨")
            Toast.makeText(context, "当前正在通话中，请稍后再拨", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}