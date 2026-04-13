package com.example.toolbox.community

import android.content.Context
import android.net.Uri
import com.example.toolbox.ApiAddress
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer as OkioBuffer
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HttpUpload(
    private val panUrl: String,
    private val token: String
) {
    private var call: Call? = null

    suspend fun uploadFile(
        filePath: String,
        status: Int,
        postData: Map<String, String> = emptyMap(),
        onProgress: (Int) -> Unit  // 添加这个参数
    ): String = suspendCancellableCoroutine { continuation ->
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            continuation.resumeWithException(Exception("文件不存在"))
            return@suspendCancellableCoroutine
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        try {
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            postData.forEach { (key, value) -> builder.addFormDataPart(key, value) }

            // 修改2: 传递 onProgress
            val progressBody = createProgressRequestBody(
                file.asRequestBody("application/octet-stream".toMediaType()),
                onProgress  // 传递进去
            )

            builder.addFormDataPart("file", file.name, progressBody)
            builder.addFormDataPart("status", status.toString())

            val request = Request.Builder()
                .url(panUrl)
                .addHeader("x-access-token", token)
                .post(builder.build())
                .build()

            call = client.newCall(request)

            // 修改3: 设置取消回调
            continuation.invokeOnCancellation {
                call?.cancel()
            }

            call?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val result = response.body.string()
                        continuation.resume(result)
                    } else {
                        continuation.resumeWithException(Exception("上传失败: ${response.code}"))
                    }
                }
            })
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    private fun createProgressRequestBody(
        requestBody: RequestBody,
        onProgress: (Int) -> Unit  // 添加这个参数
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? = requestBody.contentType()
            override fun contentLength(): Long = requestBody.contentLength()
            override fun writeTo(sink: okio.BufferedSink) {
                val totalBytes = contentLength()
                var bytesWritten = 0L
                val buffer = OkioBuffer()
                requestBody.writeTo(buffer)

                while (!buffer.exhausted()) {
                    sink.write(buffer, buffer.size)
                    bytesWritten += buffer.size
                    val progress = if (totalBytes > 0) (bytesWritten * 100 / totalBytes).toInt() else 0
                    onProgress(progress)  // 使用传入的 onProgress
                }
            }
        }
    }

    fun cancelUpload() {
        call?.cancel()
    }
}

suspend fun uploadImage(
    filePath: String,
    token: String,
    status: Int = 1,
    onProgress: (Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        val uploader = HttpUpload(
            panUrl = "${ApiAddress}upload_image",
            token = token
        )
        val resultJson = uploader.uploadFile(
            filePath = filePath,
            onProgress = onProgress,
            status = status
        )
        val result = try {
            val jsonElement = Json.parseToJsonElement(resultJson)
            val jsonObject = jsonElement.jsonObject
            val imageUrl = jsonObject["image_url"]?.jsonPrimitive?.content
            if (imageUrl?.startsWith("http") == true) {
                imageUrl
            } else {
                "${ApiAddress}uploads/$imageUrl"
            }
        } catch (_: Exception) {
            null
        }
        return@withContext result
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}