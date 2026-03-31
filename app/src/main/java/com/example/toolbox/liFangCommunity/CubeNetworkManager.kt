package com.example.toolbox.liFangCommunity

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor

object CubeNetworkManager {
    private var _client: OkHttpClient? = null
    val client: OkHttpClient
        get() = _client ?: throw IllegalStateException("CubeNetworkManager 必须使用 Context 进行初始化。请在 Application 或主 Activity 中调用 initialize() 方法。")

    const val BASE_URL = "https://JFMinus.pythonanywhere.com"

    private var persistentCookieJar: PersistentCookieJar? = null

    fun initialize(context: Context) {
        if (_client == null) {
            persistentCookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context.applicationContext))

            _client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 连接超时 30 秒
                .readTimeout(30, TimeUnit.SECONDS)    // 读取超时 30 秒
                .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时 30 秒
                .cookieJar(persistentCookieJar!!)     // 使用 PersistentCookieJar 库
                .build()
        }
    }

    fun clearCookies() {
        persistentCookieJar?.clear()
    }
}