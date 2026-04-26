package com.example.toolbox.lanzou.service

import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink

data class LanzouUploadProgress(
    val progress: Int,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long
) {
    val speedText: String
        get() {
            if (speedBytesPerSec <= 0L) return "0 KB/s"
            val kb = speedBytesPerSec / 1024.0
            return if (kb >= 1024.0) {
                String.format(Locale.US, "%.2f MB/s", kb / 1024.0)
            } else {
                String.format(Locale.US, "%.1f KB/s", kb)
            }
        }
}

data class LanzouShareInfo(
    val shareUrl: String,
    val password: String?
)

class LanzouCloudService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    fun uploadApkAndGetShareInfo(
        cookie: String,
        apkFile: File,
        onProgress: (LanzouUploadProgress) -> Unit
    ): LanzouShareInfo? {
        if (!apkFile.exists() || !apkFile.isFile) return null
        val fileId = uploadFile(cookie, apkFile, onProgress) ?: return null
        return getShareInfo(cookie, fileId)
    }

    private fun uploadFile(
        cookie: String,
        file: File,
        onProgress: (LanzouUploadProgress) -> Unit
    ): Long? {
        val uploadBody = FileProgressRequestBody(file, onProgress)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("task", "1")
            .addFormDataPart("folder_id", "-1")
            .addFormDataPart("upload_file", file.name, uploadBody)
            .build()

        val request = Request.Builder()
            .url("https://up.woozooo.com/html5up.php")
            .addHeader("Cookie", cookie)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body.string()
            val json = Json.parseToJsonElement(body).jsonObject
            val status = json["zt"]?.jsonPrimitive?.content?.toIntOrNull() ?: return null
            if (status != 1) return null
            val uploadInfo = json["text"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
            return uploadInfo["id"]?.jsonPrimitive?.content?.toLongOrNull()
        }
    }

    private fun getShareInfo(cookie: String, fileId: Long): LanzouShareInfo? {
        val body = "task=22&file_id=$fileId".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://up.woozooo.com/doupload.php")
            .addHeader("Cookie", cookie)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyStr = response.body.string()
            val json = Json.parseToJsonElement(bodyStr).jsonObject
            val status = json["zt"]?.jsonPrimitive?.content?.toIntOrNull() ?: return null
            if (status != 1) return null
            val info = json["info"]?.jsonObject ?: return null
            val host = info["is_newd"]?.jsonPrimitive?.content.orEmpty()
            val shareId = info["f_id"]?.jsonPrimitive?.content.orEmpty()
            if (host.isBlank() || shareId.isBlank()) return null
            val pwd = info["pwd"]?.jsonPrimitive?.content
            return LanzouShareInfo(
                shareUrl = "$host/tp/$shareId",
                password = pwd
            )
        }
    }
}

private class FileProgressRequestBody(
    private val file: File,
    private val onProgress: (LanzouUploadProgress) -> Unit
) : RequestBody() {
    override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L
        var lastTimeNs = System.nanoTime()
        var lastUploaded = 0L

        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read

                val nowNs = System.nanoTime()
                if (nowNs - lastTimeNs >= 100_000_000L || uploaded == total) {
                    val deltaBytes = uploaded - lastUploaded
                    val deltaNs = nowNs - lastTimeNs
                    val speed = if (deltaNs > 0L) deltaBytes * 1_000_000_000L / deltaNs else 0L
                    val progress = if (total > 0L) (uploaded * 100 / total).toInt() else 0
                    onProgress(
                        LanzouUploadProgress(
                            progress = progress.coerceIn(0, 100),
                            uploadedBytes = uploaded,
                            totalBytes = total,
                            speedBytesPerSec = speed
                        )
                    )
                    lastTimeNs = nowNs
                    lastUploaded = uploaded
                }
            }
        }
    }
}
