package com.example.toolbox.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val isPreRelease: Boolean = false
)

suspend fun checkForUpdateWithDetails(
    context: Context,
    owner: String = "shijuhao",
    repo: String = "QingZhouCE",
    includePreRelease: Boolean = false
): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val currentInfo = context.getAppVersionInfo()
        val isSnapshot = currentInfo.isSnapShotVersion
        val currentBaseVersion = currentInfo.baseVersion
        val currentCommit = currentInfo.commitHash

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

        val targetRelease = if (includePreRelease) {
            val allReleases = parseAllReleases(body)
            allReleases.firstOrNull()
        } else {
            val latestRelease = parseReleaseInfo(body)
            val version = latestRelease.version
            if (version.contains("-") && !version.startsWith("v")) null else latestRelease
        }

        if (targetRelease == null) return@withContext null

        val latestVersion = targetRelease.version
        val releaseUrl = targetRelease.url
        val isLatestPreRelease = latestVersion.contains("-") && !latestVersion.startsWith("v")
        val latestBaseVersion = if (isLatestPreRelease) {
            latestVersion.substringBefore("-")
        } else {
            latestVersion.removePrefix("v")
        }
        val latestCommit = if (isLatestPreRelease) {
            latestVersion.substringAfterLast("-")
        } else {
            ""
        }

        val shouldUpdate = when {
            isSnapshot && !isLatestPreRelease -> {
                compareVersion(latestBaseVersion, currentBaseVersion) > 0
            }
            isSnapshot -> {
                currentCommit != latestCommit && latestCommit.isNotEmpty()
            }
            else -> {
                compareVersion(latestVersion, currentInfo.versionName) > 0
            }
        }

        if (shouldUpdate) {
            UpdateInfo(
                version = latestVersion,
                releaseUrl = releaseUrl,
                isPreRelease = isLatestPreRelease
            )
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun parseAllReleases(json: String): List<ReleaseData> {
    return try {
        val releases = mutableListOf<ReleaseData>()
        val items = json.removeSurrounding("[", "]").split("},{")
        
        for (item in items) {
            val finalItem = if (!item.startsWith("{")) "{$item" else item
            val version = finalItem
                .substringAfter("\"tag_name\":\"")
                .substringBefore("\"")
            
            val url = finalItem
                .substringAfter("\"html_url\":\"")
                .substringBefore("\"")
            
            val publishedAt = finalItem
                .substringAfter("\"published_at\":\"")
                .substringBefore("\"")
            
            releases.add(ReleaseData(version, url, publishedAt))
        }
        
        releases.sortedByDescending { it.publishedAt }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseReleaseInfo(json: String): ReleaseData {
    return try {
        val version = json
            .substringAfter("\"tag_name\":\"")
            .substringBefore("\"")
        val url = json
            .substringAfter("\"html_url\":\"")
            .substringBefore("\"")
        val publishedAt = json
            .substringAfter("\"published_at\":\"")
            .substringBefore("\"")
        ReleaseData(version, url, publishedAt)
    } catch (_: Exception) {
        ReleaseData("", "", "")
    }
}

private data class ReleaseData(
    val version: String,
    val url: String,
    val publishedAt: String = ""
)

private fun compareVersion(v1: String, v2: String): Int {
    val cleanV1 = v1.removePrefix("v").substringBefore("-")
    val cleanV2 = v2.removePrefix("v").substringBefore("-")
    
    val parts1 = cleanV1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = cleanV2.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val num1 = parts1.getOrNull(i) ?: 0
        val num2 = parts2.getOrNull(i) ?: 0
        if (num1 != num2) return if (num1 > num2) 1 else -1
    }
    return 0
}