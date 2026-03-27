package com.example.aarelay

import android.content.Context
import android.content.SharedPreferences

object MessageRepository {
    private const val PREFS_NAME = "AARelayPrefs"
    private const val KEY_SENDER = "last_sender"
    private const val KEY_MESSAGE = "last_message"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun update(context: Context, sender: String, message: String) {
        getPrefs(context).edit().apply {
            putString(KEY_SENDER, sender)
            putString(KEY_MESSAGE, message)
            apply()
        }
    }

    fun getLastSender(context: Context): String {
        return getPrefs(context).getString(KEY_SENDER, "") ?: ""
    }

    fun getLastMessage(context: Context): String {
        return getPrefs(context).getString(KEY_MESSAGE, "") ?: ""
    }
}
