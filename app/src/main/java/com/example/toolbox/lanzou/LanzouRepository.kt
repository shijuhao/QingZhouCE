package com.example.toolbox.lanzou

import android.content.Context
import com.example.toolbox.TokenManager
import com.example.toolbox.lanzou.service.LanzouCloudService
import com.example.toolbox.lanzou.service.LanzouShareInfo
import com.example.toolbox.lanzou.service.LanzouUploadProgress
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LanzouRepository(
    private val service: LanzouCloudService = LanzouCloudService()
) {
    fun isLoggedIn(context: Context): Boolean = !TokenManager.getLanzouCookie(context).isNullOrBlank()

    fun logout(context: Context) {
        TokenManager.clearLanzouCookie(context)
    }

    suspend fun uploadApkAndGetShareInfo(
        context: Context,
        apkPath: String,
        onProgress: (LanzouUploadProgress) -> Unit
    ): LanzouShareInfo? = withContext(Dispatchers.IO) {
        val cookie = TokenManager.getLanzouCookie(context) ?: return@withContext null
        val apkFile = File(apkPath)
        return@withContext service.uploadApkAndGetShareInfo(cookie, apkFile, onProgress)
    }
}
