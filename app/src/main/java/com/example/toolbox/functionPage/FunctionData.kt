package com.example.toolbox.functionPage

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.edit
import com.example.toolbox.R
import com.example.toolbox.data.function.FunctionCategory
import com.example.toolbox.data.function.FunctionItem
import com.example.toolbox.data.function.IconSource
import com.example.toolbox.data.function.SearchFunctionModel

object IconColorMap {
    private val map = mapOf(
        "blue" to Color(0xFF2196F3),
        "yellow" to Color(0xFFFFC107),
        "green" to Color(0xFF4CAF50),
        "orange" to Color(0xFFFF9800),
        "gray" to Color(0xFF9E9E9E),
        "red" to Color(0xFFF44336)
    )
    fun getColor(name: String?): Color? = name?.let { map[it] }
}

object IconColorPrefs {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_ICON_COLOR_ENABLED = "isIconColorEnabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ICON_COLOR_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ICON_COLOR_ENABLED, enabled) }
    }
}

object FavoriteManager {
    private const val PREF_NAME = "favorites"
    private const val FAVORITES_KEY = "favorite_activities"

    fun getFavorites(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(FAVORITES_KEY, emptySet())?.toList() ?: emptyList()
    }

    fun addFavorite(context: Context, activityName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(FAVORITES_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        favorites.add(activityName)
        prefs.edit {
            putStringSet(FAVORITES_KEY, favorites)
        }
    }

    fun removeFavorite(context: Context, activityName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(FAVORITES_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        favorites.remove(activityName)
        prefs.edit {
            putStringSet(FAVORITES_KEY, favorites)
        }
    }

    fun isFavorite(context: Context, activityName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val favorites = prefs.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
        return favorites.contains(activityName)
    }

    fun getFavoriteFunctions(context: Context, allFunctions: List<SearchFunctionModel>): List<SearchFunctionModel> {
        val favorites = getFavorites(context)
        return allFunctions.filter { searchModel ->
            favorites.contains(searchModel.function.activity)
        }
    }
}

val ImageVector.asIcon: IconSource
    get() = IconSource.Vector(this)

val Int.asIcon: IconSource
    get() = IconSource.Resource(this)

val functionData = listOf(
    FunctionCategory(
        name = "日常功能",
        icon = Icons.Default.WbSunny.asIcon,
        iconColorName = "blue",
        functions = listOf(
            FunctionItem("浏览器", "com.example.toolbox.webview.WebViewActivity", Icons.Outlined.Public, "blue"),
            FunctionItem("全屏时钟", "com.example.toolbox.function.daily.FullScreenClockActivity", Icons.Outlined.AccessTime, "green"),
            FunctionItem("秒表", "com.example.toolbox.function.daily.StopWatchActivity", Icons.Outlined.Timer, "orange"),
            FunctionItem("BMI指数", "com.example.toolbox.function.daily.BMIActivity", Icons.Outlined.Percent, "yellow"),
            FunctionItem("随机抽选", "com.example.toolbox.function.daily.randomChoose.RandomChooseActivity", Icons.Default.Casino, "red"),
            FunctionItem("计分板", "com.example.toolbox.function.daily.MarkBoardActivity", Icons.Default.MarkunreadMailbox, "gray"),
        )
    ),
    FunctionCategory(
        name = "视觉功能",
        icon = Icons.Default.Style.asIcon,
        iconColorName = "green",
        functions = listOf(
            FunctionItem("防OCR", "com.example.toolbox.function.visual.AntiOCRActivity", Icons.Outlined.HideImage, "blue"),
            FunctionItem("图片取色器", "com.example.toolbox.function.visual.ImageGetColorActivity", Icons.Outlined.Image, "yellow"),
            FunctionItem("MD3 配色参考", "com.example.toolbox.function.visual.MDColorSchemeActivity", Icons.Outlined.Style, "green"),
            FunctionItem("选色器", "com.example.toolbox.function.visual.ChooseColorActivity", Icons.Outlined.Style, "orange"),
            FunctionItem("随机颜色卡", "com.example.toolbox.function.visual.RandomColorActivity", Icons.Outlined.Style, "red"),
            FunctionItem("画板", "com.example.toolbox.function.visual.painter.PainterActivity", Icons.Outlined.Draw, "gray"),
        )
    ),
    FunctionCategory(
        name = "文本功能",
        icon = Icons.Default.TextFields.asIcon,
        iconColorName = "orange",
        functions = listOf(
            FunctionItem("SHA256哈希", "com.example.toolbox.function.text.SHA256Activity", Icons.Outlined.TextFields, "blue"),
            FunctionItem("进制转换", "com.example.toolbox.function.text.NumberBaseActivity", Icons.Outlined.TextFields, "green"),
            FunctionItem("特殊文本生成", "com.example.toolbox.function.text.SpecialTextActivity", Icons.Outlined.TextFields, "yellow"),
            FunctionItem("Base64编解码", "com.example.toolbox.function.text.Base64Activity", Icons.Outlined.TextFields, "orange"),
            FunctionItem("摩斯密码", "com.example.toolbox.function.text.MorseCodeActivity", Icons.Outlined.TextFields, "gray"),
            FunctionItem("RC4加解密", "com.example.toolbox.function.text.Rc4Activity", Icons.Outlined.Key, "red"),
            FunctionItem("AES加解密", "com.example.toolbox.function.text.AESActivity", Icons.Outlined.Key, "blue")
        )
    ),
    FunctionCategory(
        name = "数学功能",
        icon = Icons.Default.Numbers.asIcon,
        iconColorName = "yellow",
        functions = listOf(
            FunctionItem("函数图生成", "com.example.toolbox.function.math.FunctionImageActivity", Icons.Default.Functions, "orange"),
            FunctionItem("计算器", "com.example.toolbox.function.math.CalculatorActivity", Icons.Default.Calculate, "green"),
            FunctionItem("随机数生成", "com.example.toolbox.function.math.RandomNumberActivity", Icons.Default.Numbers, "yellow")
        )
    ),
    FunctionCategory(
        name = "系统功能",
        icon = Icons.Default.Settings.asIcon,
        iconColorName = "gray",
        functions = listOf(
            FunctionItem("设备信息", "com.example.toolbox.function.system.DeviceInfoActivity", Icons.Outlined.Info, "gray")
        )
    ),
    FunctionCategory(
        name = "休闲游戏",
        icon = Icons.Default.Games.asIcon,
        iconColorName = "red",
        functions = listOf(
            FunctionItem("2048", "com.example.toolbox.function.game.Game2048Activity", Icons.Default.VideogameAsset, "red")
        )
    ),
    FunctionCategory(
        name = "云湖功能",
        icon = R.drawable.yh.asIcon,
        iconColorName = "blue",
        functions = listOf(
            FunctionItem("YHBotMaker", "com.example.toolbox.function.yunhu.yhbotmaker.BotMainActivity", Icons.Outlined.Android, "blue")
        )
    )
)