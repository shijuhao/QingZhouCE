package com.example.toolbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.mine.ForgetPasswordActivity
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.AppIconViewer
import com.example.toolbox.utils.MarkdownRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                LoginScreen(
                    onBackPressed = { finish() },
                    onLoginSuccess = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackPressed: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val viewModel: LoginViewModel = viewModel()
    val uiState by viewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (uiState.showUserAgreementDialog) {
        UserAgreementDialog(
            onDismiss = { viewModel.dismissUserAgreementDialog() },
            onAccept = { viewModel.acceptUserAgreement() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isLoginScreen) "登录" else "注册") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconViewer()

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoginScreen) {
                    LoginForm(
                        isLoading = uiState.isLoading,
                        onLoginClick = { username, password, onePointLogin ->
                            viewModel.login(
                                context = context,
                                onePointLogin = onePointLogin,
                                username = username,
                                password = password,
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("登录成功") }
                                    onLoginSuccess()
                                },
                                onError = { error ->
                                    scope.launch { snackbarHostState.showSnackbar("登录失败: $error") }
                                }
                            )
                        },
                        onSwitchToRegister = { viewModel.switchToRegister() }
                    )
                } else {
                    RegisterForm(
                        isLoading = uiState.isLoading,
                        snackbarHostState = snackbarHostState,
                        scope = scope,
                        userAgreementAccepted = uiState.userAgreementAccepted,
                        onUserAgreementClick = { viewModel.showUserAgreementDialog() },
                        onUserAgreementAcceptedChange = { viewModel.setUserAgreementAccepted(it) },
                        onRegisterClick = { username, password, email ->
                            viewModel.register(
                                username = username,
                                password = password,
                                email = email,
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("注册成功，请登录") }
                                    viewModel.switchToLogin()
                                    viewModel.setUserAgreementAccepted(false)
                                },
                                onError = { error ->
                                    scope.launch { snackbarHostState.showSnackbar("注册失败: $error") }
                                }
                            )
                        },
                        onSwitchToLogin = { viewModel.switchToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    isLoading: Boolean,
    onLoginClick: (String, String, Boolean) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "登录到轻昼",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "输入你的用户信息",
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("邮箱/用户名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onLoginClick(username, password, false)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading && (username.isNotBlank() || password.isNotBlank()),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("登录")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = {
                onLoginClick(username, password, true)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading && username.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("一键登录")
            }
        }

        Text(
            text = "或",
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onSwitchToRegister,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("注册")
        }

        TextButton(
            onClick = {
                val intent =
                    Intent(context, ForgetPasswordActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("忘记密码？")
        }
    }
}

@Composable
fun RegisterForm(
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    userAgreementAccepted: Boolean,
    onUserAgreementClick: () -> Unit,
    onUserAgreementAcceptedChange: (Boolean) -> Unit,
    onRegisterClick: (String, String, String) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var userIsLegal by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "注册轻昼账号",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("账户") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码（至少6位）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, contentDescription = null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("邮箱") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 用户协议同意
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = userAgreementAccepted,
                onCheckedChange = { if (it) onUserAgreementClick() else onUserAgreementAcceptedChange(false) }
            )
            Text("已阅读并同意")
            TextButton(
                onClick = onUserAgreementClick,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Text("《隐私政策》", color = MaterialTheme.colorScheme.primary)
            }
        }

        // 年龄确认
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = userIsLegal,
                onCheckedChange = { userIsLegal = it }
            )
            Text("我已满14周岁或有监护人陪同")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    username.isBlank() -> scope.launch { snackbarHostState.showSnackbar("请输入用户名") }
                    password.length < 6 -> scope.launch { snackbarHostState.showSnackbar("密码至少6位") }
                    email.isBlank() -> scope.launch { snackbarHostState.showSnackbar("请输入邮箱") }
                    !userAgreementAccepted -> scope.launch { snackbarHostState.showSnackbar("请先同意用户协议") }
                    !userIsLegal -> scope.launch { snackbarHostState.showSnackbar("请确认年龄") }
                    else -> onRegisterClick(username, password, email)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("注册")
            }
        }

        TextButton(
            onClick = onSwitchToLogin,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("返回登录")
        }
    }
}

@Composable
fun UserAgreementDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val userRules = stringResource(R.string.user_rules)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                MarkdownRenderer.Render(
                    content = userRules
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAccept()
                    onDismiss()
                }
            ) {
                Text("同意")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}