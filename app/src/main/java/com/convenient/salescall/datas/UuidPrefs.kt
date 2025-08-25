package com.convenient.salescall.datas

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object UuidPrefs {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_UUID = "uuid"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveUuid(context: Context, uuid: String) {
        getPrefs(context).edit { putString(KEY_UUID, uuid) }
    }

    fun getUuid(context: Context): String? {
        return getPrefs(context).getString(KEY_UUID, null)
    }

    fun clearUuid(context: Context) {
        getPrefs(context).edit { remove(KEY_UUID) }
    }
}
