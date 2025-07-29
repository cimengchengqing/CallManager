package com.convenient.salescall.network

/**
 * 网络配置类
 */
object NetworkConfig {
    // 基础URL
    const val BASE_URL = "http://8.137.165.135:8433/"

    // 超时时间
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L

    // HTTP状态码
    const val HTTP_SUCCESS = 200
    const val HTTP_UNAUTHORIZED = 401
    const val HTTP_FORBIDDEN = 403
    const val HTTP_NOT_FOUND = 404
    const val HTTP_INTERNAL_ERROR = 500

    // 业务状态码
    const val SUCCESS_CODE = 200
    const val ERROR_CODE = -1
    const val TOKEN_EXPIRED_CODE = 401

    // 请求头
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_USER_AGENT = "User-Agent"

    // Content-Type
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
}