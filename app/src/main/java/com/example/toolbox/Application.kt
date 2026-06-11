package com.example.toolbox

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.example.toolbox.message.ChatSocketManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder

const val ApiAddress = "https://hehenya.dpdns.org:8505/"

object AppJson {
    private val default: JsonBuilder.() -> Unit = {
        prettyPrint = false
        coerceInputValues = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    val json: Json = Json{default()}

    operator fun invoke(config: JsonBuilder.() -> Unit): Json = Json {
        default()
        config()
    }
}

class MyApplication : Application(), SingletonImageLoader.Factory {
    override fun onTerminate() {
        super.onTerminate()
        ChatSocketManager.disconnect()
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
