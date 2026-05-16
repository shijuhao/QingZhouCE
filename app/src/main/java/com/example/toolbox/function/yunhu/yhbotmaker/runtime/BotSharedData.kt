package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.content.Context
import android.content.SharedPreferences

object BotSharedData {
    private var prefs: SharedPreferences? = null
    
    fun init(context: Context, botName: String) {
        prefs = context.getSharedPreferences("botSharedDataPrefs_$botName", Context.MODE_PRIVATE)
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