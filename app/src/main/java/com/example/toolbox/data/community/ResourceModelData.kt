@file:Suppress("PropertyName")

package com.example.toolbox.data.community

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class ResourceResponse(
    val resources: List<ResourceItem>
)

@Parcelize
@Serializable
data class DeveloperInfo(
    val avatar_url: String,
    val id: Int,
    val nickname: String
) : Parcelable

@Serializable
@Parcelize
data class ResourceItem(
    val icon_url: String,
    val name: String,
    val release_date: String,
    val download_url: String,
    val developer_name: String,
    val package_name: String,
    val version: String,
    val developer_info: DeveloperInfo
) : Parcelable