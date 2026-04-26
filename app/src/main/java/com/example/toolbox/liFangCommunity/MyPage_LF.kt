@file:Suppress("DEPRECATION")

package com.example.toolbox.liFangCommunity

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.R
import com.example.toolbox.mine.CustomItem
import com.example.toolbox.mine.ItemGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen_LF(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    viewModel: LiFangViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.isLoggedIn) {
                // 每次页面可见时，都重新检查 Cookie 状态和未读消息数
                viewModel.checkLoginStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("立方论坛") },
            navigationIcon = {
                FilledTonalIconButton(onClick = {
                    onMenuClick()
                }) {
                    Icon(Icons.Default.Menu, null)
                }
            }
        )
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.checkLoginStatus() }
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    ItemGroup(
                        items = buildList {
                            // 错误提示
                            if (uiState.isError) {
                                add {
                                    Column(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text(
                                            text = uiState.errorMessage ?: "发生未知错误",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            // 用户信息卡片
                            add {
                                CustomItem(
                                    onClick = {
                                        if (!uiState.isLoggedIn) {
                                            // 跳转到登录页
                                            val intent = Intent(context, LoginActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.user),
                                            contentDescription = "头像",
                                            modifier = Modifier
                                                .size(58.dp)
                                                .clip(CircleShape)
                                        )
                                        Column(
                                            modifier = Modifier.padding(start = 15.dp),
                                        ) {
                                            Text(
                                                text = uiState.username,
                                                style = TextStyle(fontWeight = FontWeight.Bold),
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = if (uiState.isLoggedIn) "欢迎回到字符立方" else "点击登录",
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // 功能列表
                item {
                    ItemGroup(
                        items = buildList {
                            if (uiState.isLoggedIn) {
                                add {
                                    ListItem(
                                        headlineContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("信箱",modifier = Modifier.weight(1f))
                                                if (uiState.unreadNotificationsCount > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Badge {
                                                        Text(uiState.unreadNotificationsCount.toString())
                                                    }
                                                }
                                            }
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.Default.Mail,
                                                contentDescription = "提醒",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = if (uiState.unreadNotificationsCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val intent = Intent(context, MailboxActivity::class.java)
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                            }
                            add {
                                ListItem(
                                    headlineContent = { Text("进入论坛") },
                                    supportingContent = { Text("浏览帖子列表") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Comment,
                                            contentDescription = "论坛",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(context, CommunityActivity::class.java)
                                            context.startActivity(intent)
                                        }
                                )
                            }
                        }
                    )
                }

                // 退出登录按钮
                if (uiState.isLoggedIn) {
                    item {
                        ItemGroup(
                            items = buildList {
                                add {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                "退出登录",
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = "退出",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.logout()
                                                Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
                }
            }
        }
    }
}