@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.community.uploadImage
import com.example.toolbox.data.main.UserInfo
import com.example.toolbox.data.mine.ApiResponse
import com.example.toolbox.mine.DeviceManagerActivity
import com.example.toolbox.mine.UserCard
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UserSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ToolBoxTheme { UserSettingsScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var userInfo by remember {
        mutableStateOf(UserInfo())
    }

    var uploadType by remember { mutableIntStateOf(1) }

    var showNameDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newBio by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checkPassword by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }

    var loadFailed by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(TokenManager.get(context) ?: "null") }

    var onePointLoginEnabled by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val client = OkHttpClient()

    fun loadUserInfo() {
        loadFailed = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val headers =
                    Headers.Builder()
                        .add("Content-Type", "application/json")
                        .add("x-access-token", token)
                        .build()

                val requestBody =
                    "{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val url = "${ApiAddress}user_info_token"
                val request = Request.Builder()
                    .url(url)
                    .headers(headers)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body.string()

                if (response.isSuccessful && responseData.isNotEmpty()) {
                    val apiResponse = AppJson.json.decodeFromString<ApiResponse>(responseData)
                    if (apiResponse.success) {
                        withContext(Dispatchers.Main) {
                            userInfo = userInfo.copy(
                                isLoaded = true,
                                tagStatus = apiResponse.user_info.title_status,
                                tag = apiResponse.user_info.title.ifEmpty { "" },
                                name = apiResponse.user_info.username,
                                level = apiResponse.user_info.level,
                                exp = apiResponse.user_info.experience,
                                bio = apiResponse.user_info.bio,
                                avatar = apiResponse.user_info.avatar_url,
                                background = apiResponse.user_info.background_url ?: "",
                                coin = apiResponse.user_info.gold,
                                notice = apiResponse.user_info.has_unread_notifications,
                                id = apiResponse.user_info.id,
                                signed = apiResponse.user_info.has_checked_in,
                                hasPendingAudit = apiResponse.user_info.has_pending_audit,
                                following = apiResponse.user_info.following_count,
                                followers = apiResponse.user_info.followers_count
                            )
                            newBio = userInfo.bio
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            userInfo = userInfo.copy(isLoaded = true)
                            loadFailed = true
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        userInfo = userInfo.copy(isLoaded = true)
                        loadFailed = true
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    userInfo = userInfo.copy(isLoaded = true)
                    loadFailed = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (token != "null") {
            loadUserInfo()
        }
    }

    fun exitLogin(localToken: String) {
        TokenManager.clear(context)
        token = "null"
        scope.launch(Dispatchers.IO) {
            val json = JSONObject().put("ha", "ha").toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("${ApiAddress}exit_qingzhou")
                .post(requestBody)
                .addHeader("x-access-token", localToken)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "成功发送退出请求", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("NetworkError", "Code: ${response.code}, Body: $responseBody")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeUsername(name: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().put("new_username", name).toString()
                val request = Request.Builder()
                    .url("${ApiAddress}change_username")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .addHeader("x-access-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            userInfo = userInfo.copy(name = name)
                            Toast.makeText(context, "名称修改成功", Toast.LENGTH_SHORT).show()
                        } else {
                            val message = try {
                                val jsonRes = JSONObject(bodyString)
                                val msg = jsonRes.optString("message", "未知错误")
                                if (msg.contains("MySQL")) "修改失败：未知错误" else "修改失败：$msg"
                            } catch (_: Exception) {
                                "修改失败：状态码 ${response.code}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun changeUserBio(bio: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().put("bio", bio).toString()
                val request = Request.Builder()
                    .url("${ApiAddress}set_bio")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .addHeader("x-access-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            userInfo = userInfo.copy(bio = bio)
                            Toast.makeText(context, "简介修改成功", Toast.LENGTH_SHORT).show()
                        } else {
                            val message = try {
                                val jsonRes = JSONObject(bodyString)
                                val msg = jsonRes.optString("message", "未知错误")
                                if (msg.contains("MySQL")) "修改失败：未知错误" else "修改失败：$msg"
                            } catch (_: Exception) {
                                "修改失败：状态码 ${response.code}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun changeUserPassword(password: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().put("new_password", password).toString()
                val request = Request.Builder()
                    .url("${ApiAddress}chance_password")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .addHeader("x-access-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "密码修改成功，请重新登录", Toast.LENGTH_SHORT)
                                .show()
                            exitLogin(token)
                        } else {
                            val message = try {
                                val jsonRes = JSONObject(bodyString)
                                val msg = jsonRes.optString("message", "未知错误")
                                if (msg.contains("MySQL")) "修改失败：未知错误" else "修改失败：$msg"
                            } catch (_: Exception) {
                                "修改失败：状态码 ${response.code}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateAvatarByUrl(url: String) {
        scope.launch(Dispatchers.IO) {
            val json = JSONObject().put("avatar_url", url).toString()
            val request = Request.Builder()
                .url("${ApiAddress}avatar")
                .post(json.toRequestBody("application/json".toMediaType()))
                .addHeader("x-access-token", token)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    withContext(Dispatchers.Main) {
                        val json = JSONObject(body)
                        val code = json.optInt("code", 0)
                        if (code == 200 || json.has("image_url")) {
                            val newUrl = if (url == "null") {
                                json.optString("image_url", inputUrl)
                            } else {
                                url
                            }
                            userInfo = userInfo.copy(avatar = newUrl)
                            Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                userInfo = userInfo.copy(avatar = url)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "网络错误",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun changeOPLState(value: Boolean) {
        scope.launch(Dispatchers.IO) {
            val deviceId = try {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "unknown_device"
            } catch (_: Exception) {
                "unknown_device"
            }

            val json = JSONObject()
                .put("device_id", deviceId)
                .put("enabled", value)
                .toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("${ApiAddress}toggle_quick_login")
                .post(requestBody)
                .addHeader("x-access-token", token)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (!response.isSuccessful) {
                            Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                        } else {
                            onePointLoginEnabled = !onePointLoginEnabled
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                token.let { tk ->
                    val filePath = try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val tempFile = java.io.File(
                                context.cacheDir,
                                "temp_upload_${System.currentTimeMillis()}.jpg"
                            )
                            java.io.FileOutputStream(tempFile).use { output ->
                                inputStream.copyTo(output)
                            }
                            tempFile.absolutePath
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }

                    if (filePath == null) {
                        Toast.makeText(context, "无法读取图片", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val url = uploadImage(filePath, tk, uploadType) { _ -> }

                    if (url != null) {
                        when (uploadType) {
                            1 -> {
                                userInfo = userInfo.copy(avatar = url)
                                Toast.makeText(context, "头像修改成功", Toast.LENGTH_SHORT).show()
                            }
                            2 -> {
                                userInfo = userInfo.copy(background = url)
                                Toast.makeText(context, "背景修改成功", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "上传失败", Toast.LENGTH_LONG).show()
                    }

                    java.io.File(filePath).delete()
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改账号名称") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        changeUsername(newName)
                        showNameDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("取消") }
            }
        )
    }

    if (showBioDialog) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text("修改账号简介") },
            text = {
                OutlinedTextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = { Text("新简介") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newBio.isNotBlank()) {
                        changeUserBio(newBio)
                        showBioDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showBioDialog = false }) { Text("取消") }
            }
        )
    }

    if (showPasswordDialog) {
        var passwordVisible by remember { mutableStateOf(false) }
        var checkPasswordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text("修改账号密码") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("新密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image =
                                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = checkPassword,
                        onValueChange = { checkPassword = it },
                        label = { Text("确认密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (checkPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image =
                                if (checkPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { checkPasswordVisible = !checkPasswordVisible }) {
                                Icon(image, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (password.isNotBlank() && password == checkPassword) {
                        changeUserPassword(password)
                        showPasswordDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("取消") }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改头像直链") },
            text = {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputUrl.isNotBlank()) {
                        updateAvatarByUrl(inputUrl)
                        showEditDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("账号设置") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (token != "") {
                when {
                    !userInfo.isLoaded -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }

                    loadFailed -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("加载失败")
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        loadUserInfo()
                                    }
                                ) {
                                    Text("重试")
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            SettingsGroup(
                                items = listOf {
                                    SettingsCustomItem {
                                        UserCard(token, userInfo)
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            SettingsGroup(
                                title = "个人资料",
                                items = listOf(
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.Edit,
                                            title = "修改账号名称",
                                            subtitle = "更改您的显示名称",
                                            onClick = {
                                                newName = userInfo.name
                                                showNameDialog = true
                                            }
                                        )
                                    },
                                    {
                                        SettingsItemCell(
                                            icon = Icons.AutoMirrored.Filled.TextSnippet,
                                            title = "修改账号简介",
                                            subtitle = "更改您的简介",
                                            onClick = {
                                                newBio = userInfo.bio
                                                showBioDialog = true
                                            }
                                        )
                                    },
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.Person,
                                            title = "修改头像直链",
                                            subtitle = "使用图片 URL 更新头像",
                                            onClick = {
                                                inputUrl = userInfo.avatar
                                                showEditDialog = true
                                            }
                                        )
                                    },
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.CloudUpload,
                                            title = "上传头像",
                                            subtitle = "选取本地图片并更新头像",
                                            onClick = {
                                                uploadType = 1
                                                launcher.launch("image/*")
                                            }
                                        )
                                    },
                                    {
                                        SettingsItemCell(
                                            icon = Icons.Default.Image,
                                            title = "修改背景图片",
                                            subtitle = "选取本地图片并更新背景",
                                            onClick = {
                                                uploadType = 2
                                                launcher.launch("image/*")
                                            }
                                        )
                                    }
                                )
                            )
                        }
                    }
                }

                item {
                    SettingsGroup(
                        title = "账号安全",
                        items = listOf(
                            {
                                SettingsItemCell(
                                    icon = Icons.Default.Key,
                                    title = "修改密码",
                                    subtitle = "修改账号密码",
                                    onClick = {
                                        showPasswordDialog = true
                                        password = ""
                                        checkPassword = ""
                                    }
                                )
                            },
                            {
                                SettingsSwitchItem(
                                    icon = Icons.Default.TouchApp,
                                    title = "启用一键登录",
                                    subtitle = "在登录过此账号的设备上无需密码即可登录",
                                    checked = onePointLoginEnabled,
                                    onCheckedChange = {
                                        changeOPLState(!onePointLoginEnabled)
                                    }
                                )
                            }
                        )
                    )
                }

                item {
                    SettingsGroup(
                        title = "账号操作",
                        items = listOf(
                            {
                                SettingsItemCell(
                                    icon = Icons.Default.Devices,
                                    title = "设备管理",
                                    subtitle = "管理登录了此账号的设备",
                                    onClick = {
                                        val intent =
                                            Intent(context, DeviceManagerActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                            },
                            {
                                SettingsItemCell(
                                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                                    title = "退出登录",
                                    subtitle = "退出当前账号",
                                    isDestructive = true,
                                    onClick = {
                                        exitLogin(token)
                                    }
                                )
                            }
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
                }
            } else {
                item { EmptyState() }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.HourglassEmpty,
            contentDescription = null,
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("没有可用的选项，请登录")
    }
}