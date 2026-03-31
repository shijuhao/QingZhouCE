package com.example.toolbox.liFangCommunity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.webview.WebViewActivity
import com.example.toolbox.ui.theme.ToolBoxTheme

class PostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                PostScreen(
                    onBackClick = { finish() },
                    onPostSuccess = {
                        Toast.makeText(this, "发帖成功！", Toast.LENGTH_SHORT).show()
                        finish() // 发帖成功后关闭页面，返回帖子列表页
                    },
                    onLoginClick = {
                        // 跳转到登录页面
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        // finish() // 不关闭发帖页面，让用户登录后返回继续发帖，或者根据需求决定是否关闭
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    viewModel: PostViewModel = viewModel(),
    onBackClick: () -> Unit,
    onPostSuccess: () -> Unit,
    onLoginClick: () -> Unit // 新增登录回调
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发表新帖") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding) // 应用 Scaffold 提供的 padding
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 如果未登录，显示提示并提供登录按钮
            if (!uiState.isLoggedIn) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "您尚未登录，请登录后发帖。",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onLoginClick) {
                    Text("去登录")
                }
                // 阻止显示其他发帖表单元素
                return@Column
            }

            // 标题输入框
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isPreparingForm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 内容输入框
            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.onContentChange(it) },
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth(),
                minLines = 5,
                enabled = !uiState.isLoading && !uiState.isPreparingForm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            // 论坛协议
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.agreePrivacy,
                    onCheckedChange = { viewModel.onAgreePrivacyChange(it) },
                    enabled = !uiState.isLoading && !uiState.isPreparingForm
                )
                Text(
                    text = "我已阅读并同意",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { viewModel.onAgreePrivacyChange(!uiState.agreePrivacy) }
                )
                Text(
                    text = "隐私协议与免责声明",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        val intent = Intent(context, WebViewActivity::class.java)
                        intent.putExtra("url", "https://jfminus.pythonanywhere.com/about")
                        context.startActivity(intent)
                    }
                )
            }

            // 验证码区域
            if (uiState.isPreparingForm) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在加载发帖表单...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                // 显示验证码问题
                if (uiState.captchaQuestion.isNotBlank()) {
                    Text(
                        text = uiState.captchaQuestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // 如果没有获取到验证码问题，显示错误或提示
                    Text(
                        text = "未能加载验证码问题，请尝试刷新或重新登录。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 手动输入验证码的输入框
                OutlinedTextField(
                    value = uiState.captchaManualAnswer,
                    onValueChange = { viewModel.onCaptchaManualAnswerChange(it) },
                    label = { Text("验证码 (请输入计算结果)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.captchaQuestion.isNotBlank()
                )
            }


            // 错误提示
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 发帖按钮
            Button(
                onClick = { viewModel.submitPost(onSuccess = onPostSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading && !uiState.isPreparingForm && uiState.agreePrivacy && uiState.isLoggedIn && uiState.captchaQuestion.isNotBlank() && uiState.captchaManualAnswer.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发布中...")
                } else {
                    Icon(Icons.Default.Create, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发布")
                }
            }
        }
    }
}