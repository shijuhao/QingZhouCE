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
    context: Context,
    uri: Uri,
    token: String,
    status: Int = 1,
    onProgress: (Int) -> Unit
): String? = withContext(Dispatchers.IO) {
    var tempFile: File? = null
    try {
        tempFile = createTempFileFromUri(context, uri) ?: return@withContext null

        val uploader = HttpUpload(
            panUrl = "${ApiAddress}upload_image",
            token = token
        )

        val resultJson = uploader.uploadFile(
            filePath = tempFile.absolutePath,
            onProgress = onProgress,
            status = status
        )

        val result = try {
            val jsonElement = Json.parseToJsonElement(resultJson)  // 解析为 JsonElement
            val jsonObject = jsonElement.jsonObject                 // 转为 JsonObject
            val imageUrl = jsonObject["image_url"]?.jsonPrimitive?.content  // 安全获取字符串
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
    } finally {
        tempFile?.delete()
    }
}


suspend fun uploadImageFile(
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

private fun createTempFileFromUri(context: Context, uri: Uri): File? {
    return try {
        println("开始处理Uri: $uri")
        println("Uri scheme: ${uri.scheme}")
        println("Uri authority: ${uri.authority}")
        println("Uri path: ${uri.path}")

        // 对于 Android 10+，需要特殊处理 content:// 类型的 Uri
        val tempFile = createTempFile(context)
        println("创建临时文件: ${tempFile.absolutePath}")

        // 使用 ContentResolver 直接打开原始文件描述符
        var inputStream: java.io.InputStream? = null
        var outputStream: java.io.FileOutputStream? = null

        try {
            // 尝试打开输入流
            inputStream = context.contentResolver.openInputStream(uri)

            if (inputStream == null) {
                println("错误：无法打开输入流，尝试替代方法...")
                // 尝试使用 openFileDescriptor
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val fd = pfd.fileDescriptor
                    inputStream = java.io.FileInputStream(fd)
                    println("使用 openFileDescriptor 成功打开文件")
                }
            }

            if (inputStream == null) {
                println("错误：无法读取文件内容")
                return null
            }

            outputStream = java.io.FileOutputStream(tempFile)

            // 读取并写入文件
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead

                // 每读取1MB输出一次日志
                if (totalBytes % (1024 * 1024) == 0L) {
                    println("已读取 ${totalBytes / 1024}KB")
                }
            }

            outputStream.flush()
            println("成功读取 $totalBytes 字节 (${totalBytes / 1024}KB)")

        } catch (e: Exception) {
            println("读取文件时出错: ${e.message}")
            e.printStackTrace()

            // 如果标准方法失败，尝试使用更底层的方法
            if (tempFile.length() == 0L) {
                println("尝试替代方法读取文件...")
                tryAlternativeRead(context, uri, tempFile)
            }

        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        // 检查文件是否有效
        if (tempFile.exists() && tempFile.length() > 1024) { // 要求至少1KB
            println("文件创建成功，大小: ${tempFile.length()} 字节 (${tempFile.length() / 1024}KB)")
            return tempFile
        } else {
            println("文件无效或太小: ${tempFile.length()} 字节")
            tempFile.delete()
            return null
        }

    } catch (e: Exception) {
        println("创建临时文件时出错: ${e.message}")
        e.printStackTrace()
        null
    }
}

// 创建临时文件
private fun createTempFile(context: Context): File {
    val timestamp = System.currentTimeMillis()
    val tempDir = File(context.cacheDir, "uploads")
    if (!tempDir.exists()) {
        tempDir.mkdirs()
    }

    // 使用带时间戳的临时文件
    return File(tempDir, "upload_${timestamp}.jpg")
}

// 替代读取方法
private fun tryAlternativeRead(context: Context, uri: Uri, tempFile: File): Boolean {
    return try {
        // 尝试使用 Kotlin 的 copyTo 扩展函数
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
                println("使用 copyTo 方法读取成功")
                true
            }
        } ?: false
    } catch (e: Exception) {
        println("替代方法也失败: ${e.message}")
        false
    }
}

