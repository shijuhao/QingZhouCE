package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import android.content.SharedPreferences

object BotSharedData {
    private var prefs: SharedPreferences? = null
    private var currentIndex: Int = -1
    
    fun init(context: Context, botIndex: Int) {
        if (currentIndex != botIndex || prefs == null) {
            currentIndex = botIndex
            prefs = context.getSharedPreferences("botSharedDataPrefs_$botIndex", Context.MODE_PRIVATE)
        }
    }
    
    fun set(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }
    
    fun get(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }
    
    fun getAll(): Map<String, *> {
        return prefs?.all ?: emptyMap<String, Any>()
    }
    
    fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
    
    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}