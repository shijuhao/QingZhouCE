package com.example.toolbox.function.yunhu.yhbotmaker.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YunHuResponse(
    val code: Int,
    val message: String?,
    val data: JsonElement?
)

@Serializable
data class SendMessageRequest(
    val recvId: String,
    val recvType: String,
    val contentType: String,
    val content: Content
) {
    @Serializable
    data class Content(val text: String)
}

@Serializable
data class RecallMessageRequest(
    val chatId: String,
    val chatType: String,
    val msgId: String
)