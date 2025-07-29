package com.convenient.salescall.network

import com.convenient.salescall.datas.CallLogUploadBean
import com.convenient.salescall.datas.LoginRequest
import com.convenient.salescall.datas.UserVoiceInfo
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API接口服务
 */
interface ApiService {

    /**
     * 请求验证码图片
     */
    @GET("jzzz-api/captcha/captchaImage")
    suspend fun getCaptchaImage(
        @Query("type") type: String = "math"
    ): Response<ResponseBody>


    /**
     * 用户登录
     */
    @FormUrlEncoded
    @POST("jzzz-api/login")
    suspend fun loginByPassword(
        @Header("Cookie") cookie: String,
        @Field("registrationID") registrationID: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("validateCode") validateCode: Int,
        @Field("rememberMe") rememberMe: Boolean
    ): Response<AppResponse<Unit>>

    /**
     * 通话信息上传
     */
    @POST("jzzz-api/api/voice/record/add")
    suspend fun uploadCallLog(
        @Header("Cookie") cookie: String,
        @Body request: CallLogUploadBean
    ): Response<AppResponse<Unit>>

    /**
     * 通话录音上传
     */
    @Multipart
    @POST("jzzz-api/api/voice/record/file/save")
    suspend fun uploadRecordFile(
        @Header("Cookie") cookie: String,
        @Part("id") id: RequestBody,
        @Part recordFile: MultipartBody.Part
    ): Response<AppResponse<Unit>>

    /**
     * 通话录音上传
     */
    @Multipart
    @POST("jzzz-api/api/voice/record/add2")
    suspend fun uploadCallLogs(
        @Header("Cookie") cookie: String,
        @Part("id") id: RequestBody,    //唯一的ID
        @Part("callStartTime") callStartTime: RequestBody,    //拨号时间(时间戳)
        @Part("isConnected") isConnected: RequestBody,     //是否接通
        @Part("callEndTime") callEndTime: RequestBody,        //挂断时间(时间戳)
        @Part("durationMs") durationMs: RequestBody,          //通话时长
        @Part("callerNumber") callerNumber: RequestBody,    //本机号码
        @Part("mobile") mobile: RequestBody,     //目标号码
        @Part recordFile: MultipartBody.Part?         // 录音文件，可以为null
    ): Response<AppResponse<Unit>>

    /**
     * 退出登录
     */
    @GET("jzzz-api/logout")
    suspend fun doLogout(
        @Header("Cookie") cookie: String,
    ): Response<AppResponse<Unit>>

    /**
     * 通话统计
     */
    @GET("jzzz-api/api/voice/record/report")
    suspend fun getStatisticsData(
        @Header("Cookie") cookie: String,
    ): Response<AppResponse<List<UserVoiceInfo>>>


    /**
     * 请求响应结果
     */
    data class AppResponse<T>(
        val msg: String,
        val code: Int,
        val data: T
    )


    /****************************************  分割线   ******************************************/


    /**
     * 用户登录
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * 用户注册
     */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * 刷新Token
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<LoginResponse>>

    /**
     * 获取用户信息
     */
    @GET("user/profile")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    /**
     * 上传录音文件
     */
    @Multipart
    @POST("upload/audio")
    suspend fun uploadAudioFile(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("createTime") createTime: RequestBody
    ): Response<ApiResponse<UploadResponse>>

    /**
     * 获取录音文件列表
     */
    @GET("audio/list")
    suspend fun getAudioList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageResponse<AudioFileInfo>>>

    /**
     * 获取下载链接
     */
    @GET("audio/download/{fileId}")
    suspend fun getDownloadUrl(
        @Path("fileId") fileId: String
    ): Response<ApiResponse<DownloadResponse>>
}

/**
 * 注册请求
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val phone: String? = null
)

/**
 * 刷新Token请求
 */
data class RefreshTokenRequest(
    val refreshToken: String
)


/**
 * 登录结果
 */
data class LoginResponse(
    val msg: String,
    val code: Int
)

/**
 * 用户信息
 */
data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val phone: String?,
    val avatar: String?,
    val createTime: String
)

/**
 * 上传响应
 */
data class UploadResponse(
    val fileId: String,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long,
    val uploadTime: String
)

/**
 * 音频文件信息
 */
data class AudioFileInfo(
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val duration: Long,
    val createTime: String,
    val uploadTime: String,
    val downloadUrl: String? = null
)

/**
 * 下载响应
 */
data class DownloadResponse(
    val downloadUrl: String,
    val expiresIn: Long
)