package com.convenient.salescall.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class LoggingInterceptor(
    private val enable: Boolean,
    private val tag: String = "HTTP"
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enable) return chain.proceed(chain.request())

        val request = chain.request()
        val t1 = System.nanoTime()

        // 打印请求
        try {
            val requestBody = request.body
            val reqBodyStr = if (requestBody != null) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                buffer.readString(Charset.forName("UTF-8"))
            } else null

            Log.d(tag, "--> ${request.method} ${request.url}")
            if (reqBodyStr?.isNotEmpty() == true) {
                Log.d(tag, "RequestBody: $reqBodyStr")
            }
            request.headers.forEach { Log.d(tag, "Header: ${it.first}: ${it.second}") }
        } catch (e: Exception) {
            Log.w(tag, "Log request error: ${e.message}")
        }

        // 执行并打印响应/异常
        return try {
            val response = chain.proceed(request)
            val t2 = System.nanoTime()
            Log.d(
                tag,
                "<-- ${response.code} ${response.message} ${request.url} (${
                    TimeUnit.NANOSECONDS.toMillis(t2 - t1)
                }ms)"
            )

            // 注意：大体量响应体不建议全量打印，可按需截断
            val responseBody = response.body
            val source = responseBody?.source()
            source?.request(Long.MAX_VALUE)
            val buffer = source?.buffer
            val charset = responseBody?.contentType()?.charset(Charset.forName("UTF-8"))
                ?: Charset.forName("UTF-8")
            val respStr = buffer?.clone()?.readString(charset)
            if (!respStr.isNullOrEmpty()) {
                Log.d(tag, "ResponseBody: $respStr")
            }
            response
        } catch (e: Exception) {
            // 关键：失败也打印
            Log.e(tag, "<-- HTTP FAILED: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }
}