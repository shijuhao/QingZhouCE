package com.example.toolbox.mine

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.settings.SettingsItemCell
import com.example.toolbox.settings.UserSettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserBottomSheet(
    userName: String,
    userId: Int,
    userAvatar: String,
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            sheetState = sheetState
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    ItemGroup(
                        items = buildList<@Composable () -> Unit> {
                            // 用户信息头部
                            add {
                                CustomItem {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(userAvatar),
                                            contentDescription = "头像",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape),
                                        )
                                        Column(
                                            modifier = Modifier.padding(start = 15.dp),
                                        ) {
                                            Text(
                                                text = userName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            add {
                                SettingsItemCell(
                                    title = "查看主页",
                                    icon = Icons.Default.Person,
                                    subtitle = "查看我的主页",
                                    onClick = {
                                        onDismiss()
                                        val intent = Intent(context, UserInfoActivity::class.java)
                                        intent.putExtra("userId", userId)
                                        context.startActivity(intent)
                                    }
                                )
                            }

                            // 账号设置
                            add {
                                SettingsItemCell(
                                    title = "转到账号设置",
                                    icon = Icons.Default.SettingsSuggest,
                                    subtitle = "编辑头像、名称、简介，退出登录等",
                                    onClick = {
                                        onDismiss() // 点击后通常需要关闭 Sheet
                                        val intent = Intent(context, UserSettingsActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    )
                }

                item {
                    Spacer(
                        modifier = Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}