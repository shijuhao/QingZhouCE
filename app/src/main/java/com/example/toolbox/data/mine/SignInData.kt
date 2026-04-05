package com.example.toolbox.data.mine

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RankingResponse(
    @SerialName("rank_list") val rankList: List<RankItem>
)

@Serializable
data class RankItem(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("user_id") val userId: Int = 0,
    val gold: Int,
    @SerialName("consecutive_check_ins") val consecutiveCheckIns: Int,
    @SerialName("total_check_ins") val totalCheckIns: Int
)

@Serializable
data class SignInResponse(
    val success: Boolean,
    @SerialName("online_information") val onlineInformation: SignInInfo
)

@Serializable
data class SignInInfo(
    @SerialName("gold_awarded") val goldAwarded: Int,
    @SerialName("experience_added") val experienceAdded: Int
)

data class SignInResult(
    val goldAwarded: Int,
    val experienceAdded: Int
)