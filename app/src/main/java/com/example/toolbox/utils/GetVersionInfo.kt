package com.example.toolbox.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long
)

fun Context.getAppVersionInfo(): AppVersionInfo {
    val packageManager = packageManager
    val packageName = packageName
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        val versionName = packageInfo.versionName ?: "未知"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        AppVersionInfo(versionName, versionCode)
    } catch (_: PackageManager.NameNotFoundException) {
        AppVersionInfo("未知", 0L)
    }
}