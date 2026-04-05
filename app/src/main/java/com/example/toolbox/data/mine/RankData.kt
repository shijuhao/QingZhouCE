package com.example.toolbox.data.mine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RankResponse(
    @SerialName("rank_list") val rankList: List<RankUser>,
    @SerialName("success") val success: Boolean
)

@Serializable
data class RankUser(
    @SerialName("username") val name: String = "",
    @SerialName("avatar_url") val avatar: String = "",
    @SerialName("user_id") val userId: Int = 0,
    val gold: Int = 0,
    @SerialName("activity_points") val activityPoint: Int = 0,
    @SerialName("resource_count") val resourceCount: Int = 0,
    @SerialName("level") val level: Int = 1,
    @SerialName("experience") val exp: Int = 0
)