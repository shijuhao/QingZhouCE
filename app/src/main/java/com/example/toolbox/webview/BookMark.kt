package com.example.toolbox.webview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Bookmark(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val url: String,
    val timeAdded: Long = System.currentTimeMillis()
) : Parcelable