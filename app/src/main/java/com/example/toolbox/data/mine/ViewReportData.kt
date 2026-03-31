package com.example.toolbox.data.mine

import kotlinx.serialization.Serializable

@Serializable
data class ReportResponse(val reports: List<ReportItem>?)

@Serializable
data class ReportItem(
    val content: String?,
    val report_time: String?,
    val report_type: Int,
    val reporter: User,
    val reported_user: User
)

@Serializable
data class User(val username: String)