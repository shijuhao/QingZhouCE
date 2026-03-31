package com.example.toolbox.data.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CircleNotifications
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 定义更新日志条目类型
enum class UpdateLogType(val icon: @Composable () -> Unit) {
    NEW(
        icon = {
            Icon(
                imageVector = Icons.Default.NewReleases,
                contentDescription = "新功能",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    ),
    FIX(
        icon = {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "问题修复",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    ),
    IMPROVE(
        icon = {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "改进优化",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    ),
    SECURITY(
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "安全更新",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    ),
    OTHER(
        icon = {
            Icon(
                imageVector = Icons.Default.CircleNotifications,
                contentDescription = "其他更新",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

data class UpdateLogItem(
    val text: String,
    val type: UpdateLogType
)

data class VersionUpdate(
    val version: String,
    val date: String,
    val logs: List<UpdateLogItem>
)

val updateLogs = listOf(
    VersionUpdate(
        version = "0.0.9",
        date = "2026/3/22",
        logs = listOf(
            UpdateLogItem("优化内置库", UpdateLogType.IMPROVE),
            UpdateLogItem("优化主页", UpdateLogType.IMPROVE),
            UpdateLogItem("优化帖子截图", UpdateLogType.IMPROVE)
        )
    ),
    VersionUpdate(
        version = "0.0.8",
        date = "2026/3/14",
        logs = listOf(
            UpdateLogItem("新增YHBotMaker", UpdateLogType.NEW),
            UpdateLogItem("支持发送私信、添加好友", UpdateLogType.IMPROVE),
            UpdateLogItem("优化主页", UpdateLogType.IMPROVE)
        )
    ),
    VersionUpdate(
        version = "0.0.7",
        date = "2026/3/3",
        logs = listOf(
            UpdateLogItem("新增随机颜色卡", UpdateLogType.NEW),
            UpdateLogItem("新增SHA256计算", UpdateLogType.NEW),
            UpdateLogItem("支持二次退出确认", UpdateLogType.NEW),
            UpdateLogItem("为安卓12以下提供启动画面", UpdateLogType.IMPROVE),
            UpdateLogItem("优化细节", UpdateLogType.IMPROVE),
            UpdateLogItem("修复帖子详情页问题", UpdateLogType.FIX),
            UpdateLogItem("修复私密帖子泄露问题", UpdateLogType.SECURITY),
            UpdateLogItem("加密存储Token", UpdateLogType.SECURITY),
            UpdateLogItem("更换新的应用图标，且此图标支持莫奈取色", UpdateLogType.OTHER),
        )
    ),
    VersionUpdate(
        version = "0.0.6",
        date = "2026/2/22",
        logs = listOf(
            UpdateLogItem("新增引导页", UpdateLogType.NEW),
            UpdateLogItem("新增图片取色器", UpdateLogType.NEW),
            UpdateLogItem("新增进制转换器", UpdateLogType.NEW),
            UpdateLogItem("轻昼社区支持Websocket连接", UpdateLogType.IMPROVE),
            UpdateLogItem("支持查看帖子详情", UpdateLogType.IMPROVE),
            UpdateLogItem("优化UI", UpdateLogType.IMPROVE),
        )
    ),
    VersionUpdate(
        version = "0.0.5",
        date = "2026/2/13",
        logs = listOf(
            UpdateLogItem("新增内置浏览器", UpdateLogType.NEW),
            UpdateLogItem("支持查看轻昼资源库", UpdateLogType.IMPROVE),
            UpdateLogItem("帖子支持发送多图", UpdateLogType.IMPROVE),
            UpdateLogItem("支持修改个人资料", UpdateLogType.IMPROVE),
            UpdateLogItem("支持举报用户", UpdateLogType.IMPROVE),
            UpdateLogItem("支持编辑帖子", UpdateLogType.IMPROVE),
            UpdateLogItem("优化系统功能/设备信息", UpdateLogType.IMPROVE),
            UpdateLogItem("优化数学功能/计算器", UpdateLogType.IMPROVE),
            UpdateLogItem("优化UI", UpdateLogType.IMPROVE),
            UpdateLogItem("修复点赞问题", UpdateLogType.FIX),
        )
    ),
    VersionUpdate(
        version = "0.0.4",
        date = "2026/2/1",
        logs = listOf(
            UpdateLogItem("新增对立方论坛的支持", UpdateLogType.NEW),
            UpdateLogItem("新增计分板", UpdateLogType.NEW),
            UpdateLogItem("支持签到", UpdateLogType.NEW),
            UpdateLogItem("支持发帖", UpdateLogType.NEW),
            UpdateLogItem("优化软件大小", UpdateLogType.IMPROVE),
            UpdateLogItem("优化主页UI", UpdateLogType.IMPROVE),
        )
    ),
    VersionUpdate(
        version = "0.0.3",
        date = "2025/12/25",
        logs = listOf(
            UpdateLogItem("新增选色器", UpdateLogType.NEW),
            UpdateLogItem("新增全屏时钟", UpdateLogType.NEW),
            UpdateLogItem("优化UI", UpdateLogType.IMPROVE),
            UpdateLogItem("优化计算器", UpdateLogType.IMPROVE),
            UpdateLogItem("修复已知问题", UpdateLogType.FIX)
        )
    )
)