package com.example.toolbox.mine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.ui.theme.ToolBoxTheme
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val client = OkHttpClient()

// 第一步：发送验证码
fun sendCode(email: String, onResult: (Boolean, String?) -> Unit) {
    val json = AppJson.json.encodeToString(mapOf("email" to email))
    val body = json.toRequestBody("application/json;charset=UTF-8".toMediaType())
    val request = Request.Builder().url(ApiAddress + "forgot_password").post(body).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { onResult(false, "网络错误") }
        }
        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body.string()
            val message = try {
                val json = AppJson.json.parseToJsonElement(bodyStr).jsonObject
                json["message"]?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, message ?: "发送失败(${response.code})")
            }
        }
    })
}

// 第二步：重置密码
fun resetPassword(email: String, code: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
    val data = mapOf("email" to email, "new_password" to newPass, "code" to code.toIntOrNull())
    val body = AppJson.json.encodeToString(data).toRequestBody("application/json;charset=UTF-8".toMediaType())
    val request = Request.Builder().url(ApiAddress + "reset_password").post(body).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post { onResult(false, "网络错误") }
        }
        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body.string()
            val message = try {
                val json = AppJson.json.parseToJsonElement(bodyStr).jsonObject
                json["message"]?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post {
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, message ?: "修改失败(${response.code})")
            }
        }
    })
}

class ForgetPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                // 状态变量
                var email by remember { mutableStateOf("") }
                var code by remember { mutableStateOf("") }
                var newPassword by remember { mutableStateOf("") }
                var isStepTwo by remember { mutableStateOf(false) } // 是否进入第二步
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("忘记密码") },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 输入框：邮箱 (第一步显示，第二步禁用)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("邮箱") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isStepTwo && !isLoading
                        )

                        // 第二步显示的输入框
                        if (isStepTwo) {
                            Spacer(modifier = Modifier.height(15.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("验证码") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("新密码") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                        }

                        // 错误信息显示
                        errorMessage?.let {
                            Text(
                                text = "$it\n（如果无法使用，那么可能是API炸了）",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, start = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 按钮与加载动画
                        Box(contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                            } else {
                                Button(
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        if (!isStepTwo) {
                                            // 执行第一步
                                            sendCode(email) { success, msg ->
                                                isLoading = false
                                                if (success) {
                                                    isStepTwo = true
                                                    Toast.makeText(this@ForgetPasswordActivity, "发送成功", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    errorMessage = msg
                                                }
                                            }
                                        } else {
                                            // 执行第二步
                                            resetPassword(email, code, newPassword) { success, msg ->
                                                isLoading = false
                                                if (success) {
                                                    Toast.makeText(this@ForgetPasswordActivity, "修改成功", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                } else {
                                                    errorMessage = msg
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isStepTwo) "修改" else "发送验证码")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}