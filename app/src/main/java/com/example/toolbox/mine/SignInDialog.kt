@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package com.example.toolbox.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.TokenManager
import com.example.toolbox.data.mine.RankItem
import com.example.toolbox.data.mine.RankingResponse
import com.example.toolbox.data.mine.SignInResponse
import com.example.toolbox.data.mine.SignInResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInDialog(
    hasSigned: Int,
    onDismiss: () -> Unit,
    onUserClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 状态管理
    var rankingList by remember { mutableStateOf<List<RankItem>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSigned by remember { mutableIntStateOf(hasSigned) }
    var isLoading by remember { mutableStateOf(false) }

    // 获取排名数据
    fun loadRanking() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                token?.let {
                    getRankingList(it) { result ->
                        isRefreshing = false
                        if (result.isSuccess) {
                            rankingList = result.getOrNull() ?: emptyList()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("获取排名失败")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isRefreshing = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("获取排名失败: ${e.message}")
                }
            }
        }
    }

    // 签到函数
    fun signIn() {
        coroutineScope.launch {
            isLoading = true
            try {
                token?.let {
                    signInRequest(it) { result ->
                        isLoading = false
                        if (result.isSuccess) {
                            isSigned = 1
                            val signInResult = result.getOrNull()
                            if (signInResult != null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "签到成功，获得${signInResult.goldAwarded}枚金币 ${signInResult.experienceAdded}点经验"
                                    )
                                }
                                // 重新加载排名
                                loadRanking()
                            }
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "签到失败"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("签到失败: ${e.message}")
                }
            }
        }
    }

    // 初始加载数据
    LaunchedEffect(Unit) {
        loadRanking()
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("签到") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                        },
                        actions = {
                            if (isSigned == 1) {
                                Text(
                                    text = "恭喜你已签到~",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 签到按钮
                    if (isSigned == 0) {
                        Button(
                            onClick = { signIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("签到中...")
                            } else {
                                Text("签到")
                            }
                        }
                    }

                    // 使用 PullToRefreshBox 替代 SwipeRefresh
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { loadRanking() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(rankingList) { index, item ->
                                RankListItem(
                                    rank = index + 1,
                                    rankItem = item,
                                    onClick = { onUserClick(item.userId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 排名列表项
@Composable
fun RankListItem(
    rank: Int,
    rankItem: RankItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左边部分：排名 + 头像 + 用户信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 排名
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(32.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 头像
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    AsyncImage(
                        model = rankItem.avatarUrl,
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 用户信息
                Column {
                    Text(
                        text = rankItem.username,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "总共签到${rankItem.totalCheckIns}天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 右边部分：连续签到天数
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${rankItem.consecutiveCheckIns}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "连续签到",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

private suspend fun getRankingList(
    token: String,
    onResult: (Result<List<RankItem>>) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("${ApiAddress}rank")
            .addHeader("x-access-token", token)
            .post(FormBody.Builder().build())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401 -> "未授权，请重新登录"
                    403 -> "权限不足"
                    else -> "获取排名失败: ${response.code}"
                }
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception(errorMsg)))
                }
                return@withContext
            }

            val body = response.body.string()
            if (body.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception("服务器返回空数据")))
                }
                return@withContext
            }

            // 自动解析
            val rankingResponse = AppJson.json.decodeFromString<RankingResponse>(body)
            withContext(Dispatchers.Main) {
                onResult(Result.success(rankingResponse.rankList))
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onResult(Result.failure(e))
        }
    }
}

private suspend fun signInRequest(
    token: String,
    onResult: (Result<SignInResult>) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("${ApiAddress}online_qingzhougood")
            .addHeader("x-access-token", token)
            .post(FormBody.Builder().build())
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val errorMessage = try {
                    val json = AppJson.json.parseToJsonElement(body).jsonObject
                    val message = json["message"]?.jsonPrimitive?.contentOrNull ?: "签到失败"
                    if (message.contains("MySQL")) "签到失败：未知错误" else "签到失败：$message"
                } catch (_: Exception) {
                    "签到失败: ${response.code}"
                }
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception(errorMessage)))
                }
                return@withContext
            }

            if (body.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception("服务器返回空数据")))
                }
                return@withContext
            }

            val signInResponse = AppJson.json.decodeFromString<SignInResponse>(body)
            if (signInResponse.success) {
                val signInResult = SignInResult(
                    goldAwarded = signInResponse.onlineInformation.goldAwarded,
                    experienceAdded = signInResponse.onlineInformation.experienceAdded
                )
                withContext(Dispatchers.Main) {
                    onResult(Result.success(signInResult))
                }
            } else {
                // 如果 success 为 false，可能还有 message 字段，但根据现有结构没有，可自定义异常
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(Exception("签到失败")))
                }
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onResult(Result.failure(e))
        }
    }
}