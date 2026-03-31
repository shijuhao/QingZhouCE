package com.example.toolbox.liFangCommunity

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AuthManager {
    private const val PREFS_NAME = "CubeAuthPrefs"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_USERNAME = "username"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLoginState(username: String) {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("AuthManager must be initialized before use.")
        }
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USERNAME, username)
        }
    }

    fun clearLoginState() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("AuthManager must be initialized before use.")
        }
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, false)
                .putString(KEY_USERNAME, null)
        }
    }

    fun getIsLoggedIn(): Boolean {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("AuthManager must be initialized before use.")
        }
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUsername(): String? {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("AuthManager must be initialized before use.")
        }
        return prefs.getString(KEY_USERNAME, null)
    }
}