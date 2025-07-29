package com.convenient.salescall.datas

data class LoginRequest(
    val username: String,
    val password: String,
    val validateCode: Int,
    val rememberMe: Boolean = true
)
