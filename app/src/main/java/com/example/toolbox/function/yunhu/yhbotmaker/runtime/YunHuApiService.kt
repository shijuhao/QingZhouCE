package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import android.util.Log
import com.example.toolbox.AppJson
import com.example.toolbox.function.yunhu.yhbotmaker.data.RecallMessageRequest
import com.example.toolbox.function.yunhu.yhbotmaker.data.SendMessageRequest
import com.example.toolbox.function.yunhu.yhbotmaker.data.YunHuResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class YunHuApiService(private val token: String) {
    private val client = OkHttpClient()
    private val baseUrl = "https://chat-go.jwzhd.com/open-apis/v1"

    companion object {
        private const val TAG = "YunHuApi"
    }

    /**
     * 发送消息
     * @param recvId 接收者ID（chatId）
     * @param recvType 接收者类型（user/group）
     * @param contentType 内容类型（text/markdown/html）
     * @param content 文本内容
     * @param onSuccess 成功回调（返回响应码和消息）
     * @param onError 失败回调（返回错误信息）
     */
    fun sendMessage(
        recvId: String,
        recvType: String,
        contentType: String,
        content: String,
        onSuccess: (Int, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/bot/send?token=$token"
        val requestBody = SendMessageRequest(
            recvId = recvId,
            recvType = recvType,
            contentType = contentType,
            content = SendMessageRequest.Content(content)
        )
        val json = AppJson.json.encodeToString(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendMessage onFailure", e)
                onError(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onError("HTTP ${response.code}")
                        return
                    }
                    val responseBody = response.body.string()
                    try {
                        val yunHuResp = AppJson.json.decodeFromString<YunHuResponse>(responseBody)
                        if (yunHuResp.code == 1) {
                            onSuccess(yunHuResp.code, yunHuResp.message ?: "发送成功")
                        } else {
                            onError(yunHuResp.message ?: "未知错误")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parse error", e)
                        onError("解析响应失败")
                    }
                }
            }
        })
    }

    // 后续可添加撤回消息等方法
    fun recallMessage(
        chatId: String,
        chatType: String,
        msgId: String,
        onSuccess: (Int, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$baseUrl/bot/recall?token=$token"
        val requestBody = RecallMessageRequest(
            chatId = chatId,
            chatType = chatType,
            msgId = msgId
        )
        val json = AppJson.json.encodeToString(requestBody)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onError("HTTP ${response.code}")
                        return
                    }
                    val responseBody = response.body.string()
                    try {
                        val yunHuResp = AppJson.json.decodeFromString<YunHuResponse>(responseBody)
                        if (yunHuResp.code == 1) {
                            onSuccess(yunHuResp.code, "撤回成功")
                        } else {
                            onError(yunHuResp.message ?: "撤回失败")
                        }
                    } catch (_: Exception) {
                        onError("解析响应失败")
                    }
                }
            }
        })
    }
}