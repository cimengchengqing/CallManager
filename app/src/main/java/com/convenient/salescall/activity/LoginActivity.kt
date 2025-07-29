package com.convenient.salescall.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.jpush.android.api.JPushInterface
import com.convenient.salescall.databinding.LoginLayoutBinding
import com.convenient.salescall.network.ApiService
import com.convenient.salescall.network.NetworkManager
import com.convenient.salescall.tools.LocalDataUtils
import com.convenient.salescall.tools.LogUtils
import com.convenient.salescall.viewmodel.LoginViewModel
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream

fun Activity.hideKeyboard() {
    val view = currentFocus
    view?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

class LoginActivity : AppCompatActivity() {

    companion object {
        val TAG: String get() = this::class.simpleName ?: "LoginActivity"
        lateinit var JSESSIONID: String
    }

    private lateinit var localDataUtils: LocalDataUtils
    private lateinit var mViewModel: LoginViewModel
    private var bitmap: Bitmap? = null

    private var _binding: LoginLayoutBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy {
        NetworkManager.getInstance(this.applicationContext).createService(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        localDataUtils = LocalDataUtils()
        //校验并设置首次安装的时间信息
        if (localDataUtils.getFistInstallTime() == 0L) {
            localDataUtils.setFistInstallTime(System.currentTimeMillis())
        }

        mViewModel = LoginViewModel(apiService)

        _binding = LoginLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val registrationId = JPushInterface.getRegistrationID(applicationContext)
        LogUtils.d(TAG, "极光registrationId：$registrationId")

        if (localDataUtils.getAutoLogin() && localDataUtils.getAuthCookie().isNotEmpty()
            && localDataUtils.isLogin()
        ) {
            startActivity(Intent(this, MainPageActivity::class.java))
            finish()
            return
        }
        initViews()
        initListeners()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        bitmap?.recycle()
        bitmap = null
    }

    private fun initViews() {
        binding.autoLoginCb.isChecked = localDataUtils.getAutoLogin()
    }

    private fun initListeners() {
        mViewModel.captchaImageResult.observe(this) { result ->
            result.onSuccess {
                LogUtils.d(TAG, "获取验证码成功")
//                Toast.makeText(this, "获取成功", Toast.LENGTH_SHORT).show()
                bitmap = bodyToBitmap(result.getOrNull()!!)
                binding.verifyIv.setImageBitmap(bitmap)
            }.onFailure { e ->
                Log.e(TAG, "captchaImageResult: ${e.message}")
                Toast.makeText(this, "获取失败", Toast.LENGTH_SHORT).show()
            }
        }

        mViewModel.loginResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainPageActivity::class.java))
                localDataUtils.setLogin(true)
                this@LoginActivity.finish()
            }.onFailure { e ->
                localDataUtils.setLogin(false)
                mViewModel.getCaptchaImage()
                Toast.makeText(this@LoginActivity, e.message ?: "登录失败", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.loginBtn.setOnClickListener {
            if (localDataUtils.getRegistrationId().isEmpty()) {
                Toast.makeText(this, "正在获取设备的注册ID，请稍后再试...", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val username = binding.userInput.text.trim().toString()
            val pw = binding.pwInput.text.trim().toString()
            val code = binding.codeInput.text.trim().toString()

            when {
                username.isEmpty() -> {
                    Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                pw.isEmpty() -> {
                    Toast.makeText(this, "登录密码不能为空", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                code.isEmpty() -> {
                    Toast.makeText(this, "验证码不能为空", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                else -> {
                    // 所有校验通过后执行登录逻辑
                    hideKeyboard()
                    mViewModel.performLogin(username, pw, code.toInt())
                }
            }
        }

        binding.autoLoginCb.setOnCheckedChangeListener { buttonView, isChecked ->
            localDataUtils.setAutoLogin(isChecked)
        }

        binding.verifyIv.setOnClickListener {
            mViewModel.getCaptchaImage()
        }
    }

    private fun initData() {
        mViewModel.getCaptchaImage()

        val registrationId = JPushInterface.getRegistrationID(applicationContext)
        LogUtils.d(TAG, "极光registrationId：$registrationId")
        registrationId?.let {
            localDataUtils.setRegistrationId(it)
        }
    }

    private fun bodyToBitmap(body: ResponseBody): Bitmap {
        body.byteStream().use { input ->
            val output = ByteArrayOutputStream()
            input.copyTo(output)
            val byteArray = output.toByteArray()
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }
}