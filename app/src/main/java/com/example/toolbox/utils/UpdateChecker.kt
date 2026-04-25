package com.example.toolbox.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val version: String,           // 版本号
    val releaseNotes: String,      // 更新日志
    val releaseUrl: String,        // Release 页面链接
    val isPreRelease: Boolean = false  // 是否是预发布版
)

suspend fun checkForUpdateWithDetails(
    context: Context,
    owner: String = "shijuhao",
    repo: String = "QingZhouCE",
    includePreRelease: Boolean = false
): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val currentVersion = context.getAppVersionInfo().versionName

        val client = OkHttpClient()

        val url = if (includePreRelease) {
            "https://api.github.com/repos/$owner/$repo/releases"
        } else {
            "https://api.github.com/repos/$owner/$repo/releases/latest"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QingZhouCE")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val body = response.body.string()

        val (latestVersion, releaseNotes, releaseUrl, isPreRelease) = if (includePreRelease) {
            val firstRelease = body.substringAfter("[")
            parseReleaseInfo(firstRelease)
        } else {
            parseReleaseInfo(body)
        }

        if (isPreRelease && !includePreRelease) {
            return@withContext null
        }

        val updateInfo = UpdateInfo(
            version = latestVersion.trimStart('v'),
            releaseNotes = releaseNotes,
            releaseUrl = releaseUrl,
            isPreRelease = isPreRelease
        )

        val updateStatus = compareVersion(updateInfo.version, currentVersion)

        if ( updateStatus > 0 || (context.getAppVersionInfo().isSnapShotVersion && updateStatus >= 0)) {
            updateInfo
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 解析Release信息
 */
private fun parseReleaseInfo(json: String): ReleaseData {
    return try {
        val version = json
            .substringAfter("\"tag_name\":\"")
            .substringBefore("\"")

        val notes = json
            .substringAfter("\"body\":\"")
            .substringBefore("\"}")
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

        val url = json
            .substringAfter("\"html_url\":\"")
            .substringBefore("\"")

        val isPreRelease = json.contains("\"prerelease\":true")

        ReleaseData(version, notes, url, isPreRelease)
    } catch (_: Exception) {
        ReleaseData("", "", "", false)
    }
}

private data class ReleaseData(
    val version: String,
    val notes: String,
    val url: String,
    val isPreRelease: Boolean
)

private fun compareVersion(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val num1 = parts1.getOrNull(i) ?: 0
        val num2 = parts2.getOrNull(i) ?: 0
        if (num1 != num2) {
            return if (num1 > num2) 1 else -1
        }
    }
    return 0
}