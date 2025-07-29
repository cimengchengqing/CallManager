package com.convenient.salescall.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> get() = _message
    fun setMessage(msg: String) {
        _message.value = msg
    }
}