package com.example.toolbox.community

import android.util.Log
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.community.ApiResponse
import com.example.toolbox.data.community.CategoriesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

suspend fun loadCategories(
    client: OkHttpClient,
    userToken: String
): CategoriesResponse = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("${ApiAddress}get_categories")
        .addHeader("x-access-token", userToken)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }
        val responseData = response.body.string()
        if (responseData.isEmpty()) {
            throw Exception("返回数据为空")
        }
        AppJson.json.decodeFromString<CategoriesResponse>(responseData)
    }
}

suspend fun loadMessages(
    client: OkHttpClient,
    userToken: String,
    page: Int = 1,
    perPage: Int = 20,
    categoryId: Int = 1
): ApiResponse = withContext(Dispatchers.IO) {
    val url = "${ApiAddress}v3/get_message?page=$page&per_page=$perPage&category_id=$categoryId"
    val request = Request.Builder()
        .url(url)
        .addHeader("x-access-token", userToken)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }
        val responseData = response.body.string()
        if (responseData.isEmpty()) {
            throw Exception("返回数据为空")
        }
        AppJson.json.decodeFromString<ApiResponse>(responseData)
    }
}

suspend fun toggleLike(
    client: OkHttpClient,
    token: String,
    messageId: Int,
    wasLiked: Boolean,  // 原来的点赞状态
    currentLikeCount: Int  // 原来的点赞数
): Pair<Boolean, Int> = withContext(Dispatchers.IO) {
    val json = AppJson.json.encodeToString(mapOf("message_id" to messageId.toString()))
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("${ApiAddress}like_message")
        .post(body)
        .addHeader("x-access-token", token)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            return@withContext Pair(wasLiked, currentLikeCount)
        }
        val newIsLiked = !wasLiked
        val newLikeCount = if (newIsLiked) currentLikeCount + 1 else currentLikeCount - 1
        Pair(newIsLiked, newLikeCount)
    }
}

suspend fun deleteMessage(
    client: OkHttpClient,
    token: String,
    userStatus: Int,
    messageId: Int
): Unit = withContext(Dispatchers.IO) {
    val api = "${ApiAddress}${ if(userStatus==1) "admin/" else "" }delete_message"
    val json = AppJson.json.encodeToString(mapOf("message_id" to messageId, "status" to 1))
    val body = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(api)
        .post(body)
        .addHeader("x-access-token", token)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw Exception("删除失败")
        }
    }
}

suspend fun postReply(
    client: OkHttpClient,
    token: String,
    content: String,
    categoryId: Int,
    refId: Int,
    isPrivate: Boolean,
    targetUserId: Int
): Unit = withContext(Dispatchers.IO) {
    val jsonObject = buildJsonObject {
        put("content", content)
        put("category_id", categoryId)
        if (refId != null) {
            put("referenced_message_id", refId)
        }
        if (isPrivate) {
            put("visible_to", buildJsonArray {
                add(JsonPrimitive(targetUserId))
            })
        }
    }
    val body = jsonObject.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url("${ApiAddress}post_referenced_message")
        .post(body)
        .addHeader("x-access-token", token)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw Exception("回复失败")
        }
    }
}

suspend fun addOne(token: String, messageId: Int): Boolean {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val jsonBody = JSONObject().toString()
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonBody.toRequestBody(mediaType)

    val request = Request.Builder()
        .url("${ApiAddress}add_one/$messageId")
        .post(requestBody)
        .addHeader("x-access-token", token)
        .build()

    return withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext false
                }

                val responseBody = response.body.string()
                if (responseBody.isBlank()) {
                    return@withContext false
                }

                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (_: Exception) {
                    return@withContext false
                }

                jsonResponse.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.e("NetworkError", "请求失败", e)
            false
        }
    }
}