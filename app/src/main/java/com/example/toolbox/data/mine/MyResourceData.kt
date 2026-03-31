package com.example.toolbox.data.mine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourceResponse(val user_resources: List<ResourceItem>)

@Serializable
data class ResourceItem(
    val id: Int,
    val name: String,
    val status: Int = 0,
    @SerialName("icon_url") val iconUrl: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("download_url") val downloadUrl: String = "",
    @SerialName("package_name") val packageName: String = "",
    val size: String = "",
    val version: String = "",
    val category: Category = Category(0)  // 默认值
)

@Serializable
data class Category(val id: Int)