package com.example.toolbox.data.mine

import kotlinx.serialization.Serializable

@Serializable
data class PendingResourceResponse(val pending_resources: List<LocalResourceItem>?)

@Serializable
data class LocalResourceItem(
    val id: Int,
    val name: String,
    val developer_name: String,
    val release_date: String,
    val icon_url: String?,
    val download_url: String,
    val version: String,
    val description: String,
    val status: Int,
    val category: ResourceCategory
)

@Serializable
data class ResourceCategory(val name: String)
