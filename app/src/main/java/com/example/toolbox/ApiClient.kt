package com.example.toolbox

import android.content.Context
import com.example.toolbox.data.LoginRequest
import com.example.toolbox.data.LoginResponse
import com.example.toolbox.data.RegisterRequest
import com.example.toolbox.data.RegisterResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// HTTP 请求工具
object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun login(context: Context, loginRequest: LoginRequest): LoginResponse =
        withContext(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(loginRequest)
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

                val deviceId = try {
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    ) ?: "unknown_device"
                } catch (_: Exception) {
                    "unknown_device"
                }

                val request = Request.Builder()
                    .url("${ApiAddress}login")
                    .post(body)
                    .addHeader("X-Device-Model", loginRequest.device_type)
                    .addHeader("X-Device-ID", deviceId)
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseData = response.body.string()
                    if (response.isSuccessful && responseData.isNotEmpty()) {
                        AppJson.json.decodeFromString<LoginResponse>(responseData)
                    } else {
                        LoginResponse(success = false, message = "网络请求失败: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                LoginResponse(success = false, message = "请求异常: ${e.message}")
            }
        }

    suspend fun register(registerRequest: RegisterRequest): RegisterResponse =
        withContext(Dispatchers.IO) {
            try {
                val json = Json.encodeToString(registerRequest)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${ApiAddress}register")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseData = response.body.string()
                    if (response.isSuccessful && responseData.isNotEmpty()) {
                        AppJson.json.decodeFromString<RegisterResponse>(responseData)
                    } else {
                        RegisterResponse(success = false, message = "注册失败: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                RegisterResponse(success = false, message = "请求异常: ${e.message}")
            }
        }
}