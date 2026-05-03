package com.example.toolbox.message

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.ApiAddress
import com.example.toolbox.TokenManager
import com.example.toolbox.data.GroupInfo
import com.example.toolbox.data.GroupCreator
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.toolbox.AppJson
import com.example.toolbox.data.GroupInfoResponse
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getIntExtra("group_id", -1)
        val isJoined = intent.getBooleanExtra("is_joined", false)
        
        // Get initial group data from intent (passed from MessageActivity)
        val initialGroupInfo = if (intent.hasExtra("group_name")) {
            GroupInfo(
                id = groupId,
                name = intent.getStringExtra("group_name") ?: "",
                avatarUrl = intent.getStringExtra("group_avatar") ?: "",
                description = intent.getStringExtra("group_description") ?: "",
                isPrivate = intent.getBooleanExtra("group_is_private", false),
                membersCount = intent.getIntExtra("group_members_count", 0),
                createdAt = intent.getStringExtra("group_created_at") ?: "",
                isJoined = isJoined,
                creator = null
            )
        } else null
        
        setContent {
            ToolBoxTheme {
                val token = TokenManager.get(this)
                val viewModel: GroupInfoViewModel = viewModel(
                    factory = token?.let { GroupInfoViewModelFactory(it, groupId, isJoined, initialGroupInfo) }
                )
                GroupInfoScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    viewModel: GroupInfoViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.joinSuccess.collect {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("群聊信息") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.group == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.group != null) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    val group = uiState.group
                    if (group == null) {
                        return@Column
                    }
                    
                    // 群头像
                    Image(
                        painter = rememberAsyncImagePainter(
                            if (group.avatarUrl.startsWith("http")) group.avatarUrl 
                            else "${ApiAddress}uploads/${group.avatarUrl}"
                        ),
                        contentDescription = "群头像",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 群名称
                    Text(
                        text = group.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 群号
                    Text(
                        text = "群号: ${group.id}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 群信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 成员数
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("成员数: ${group.membersCount}")
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 创建时间
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("创建时间: ${formatGroupTime(group.createdAt)}")
                            }
                            
                            if (group.isPrivate) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("私有群", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 群简介
                    if (group.description.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "群简介",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    group.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 创建者信息
                    if (group.creator != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val creator = group.creator
                                if (creator == null) {
                                    return@Row
                                }
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        if (creator.avatarUrl.startsWith("http")) creator.avatarUrl
                                        else "${ApiAddress}uploads/${creator.avatarUrl}"
                                    ),
                                    contentDescription = "创建者头像",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("群主", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(creator.username, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 底部按钮 - 仅当未加入时显示加入按钮
                    if (!uiState.isJoined) {
                        Button(
                            onClick = { viewModel.joinGroup() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isJoining
                        ) {
                            if (uiState.isJoining) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("加入群聊")
                            }
                        }
                    }
                }
            }
            } else if (uiState.error != null) {
                Text(
                    text = "错误: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

fun formatGroupTime(timeStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr) ?: return timeStr
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        timeStr
    }
}

// GroupInfoViewModel
data class GroupInfoUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val group: GroupInfo? = null,
    val error: String? = null,
    val isJoined: Boolean = false,
    val isJoining: Boolean = false
)

class GroupInfoViewModel(
    private val token: String,
    private val groupId: Int,
    initialIsJoined: Boolean = false,
    initialGroupInfo: GroupInfo? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupInfoUiState(
        isJoined = initialIsJoined,
        group = initialGroupInfo,
        isLoading = initialGroupInfo == null
    ))
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _joinSuccess = MutableSharedFlow<Unit>()
    val joinSuccess: SharedFlow<Unit> = _joinSuccess.asSharedFlow()

    private val client = OkHttpClient()

    init {
        if (_uiState.value.group == null) {
            loadGroupInfo()
        }
    }

    fun refresh() {
        loadGroupInfo(isRefresh = true)
    }

    private fun loadGroupInfo(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            }
            
            try {
                val url = "${ApiAddress}group/info/$groupId"
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .get()
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    if (response.isSuccessful) {
                        try {
                            val parsed = AppJson.json.decodeFromString<GroupInfoResponse>(responseBody)
                            if (parsed.success) {
                                Result.success(parsed.group)
                            } else {
                                Result.failure(Exception(parsed.message ?: "获取群聊信息失败"))
                            }
                        } catch (e: Exception) {
                            Result.failure(Exception("解析响应失败: ${e.message}"))
                        }
                    } else {
                        try {
                            val parsed = AppJson.json.decodeFromString<GroupInfoResponse>(responseBody)
                            Result.failure(Exception(parsed.message ?: "请求失败"))
                        } catch (e: Exception) {
                            Result.failure(Exception("请求失败: ${response.code}"))
                        }
                    }
                }

                result.fold(
                    onSuccess = { groupInfo ->
                        if (groupInfo != null) {
                            _uiState.update { it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                group = groupInfo,
                                error = null,
                                isJoined = groupInfo.isJoined
                            ) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "群聊不存在") }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = exception.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun joinGroup() {
        val group = _uiState.value.group ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }

            try {
                val url = "${ApiAddress}group/join"
                val body = """{"group_id": ${group.id}}"""
                val request = Request.Builder()
                    .url(url)
                    .header("x-access-token", token)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val result = withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body.string()
                    try {
                        val jsonElement = Json.parseToJsonElement(responseBody)
                        val success = jsonElement.jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
                        if (success) {
                            Result.success(true)
                        } else {
                            val message = jsonElement.jsonObject["message"]?.jsonPrimitive?.content ?: "加入失败"
                            Result.failure(Exception(message))
                        }
                    } catch (e: Exception) {
                        Result.failure(Exception("解析响应失败: ${e.message}"))
                    }
                }

                result.fold(
                    onSuccess = {
                        _toastMessage.emit("加入成功")
                        _uiState.update { it.copy(isJoining = false, isJoined = true) }
                        _joinSuccess.emit(Unit)
                    },
                    onFailure = { exception ->
                        _toastMessage.emit(exception.message ?: "加入失败")
                        _uiState.update { it.copy(isJoining = false) }
                    }
                )
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "加入失败")
                _uiState.update { it.copy(isJoining = false) }
            }
        }
    }
}

class GroupInfoViewModelFactory(
    private val token: String,
    private val groupId: Int,
    private val isJoined: Boolean,
    private val initialGroupInfo: GroupInfo? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupInfoViewModel::class.java)) {
            return GroupInfoViewModel(token, groupId, isJoined, initialGroupInfo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
