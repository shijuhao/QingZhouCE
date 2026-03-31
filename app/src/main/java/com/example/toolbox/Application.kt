package com.example.toolbox

import android.app.Application
import cat.ereza.customactivityoncrash.config.CaocConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder

object AppJson {
    private val default: JsonBuilder.() -> Unit = {
        prettyPrint = false
        coerceInputValues = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    val json: Json = Json{default()}

    operator fun invoke(config: JsonBuilder.() -> Unit): Json = Json {
        default()
        config()
    }
}

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .enabled(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .trackActivities(true)
            .errorActivity(CustomErrorActivity::class.java)
            .minTimeBetweenCrashesMs(2000)
            .restartActivity(MainActivity::class.java)
            .apply()
    }
}