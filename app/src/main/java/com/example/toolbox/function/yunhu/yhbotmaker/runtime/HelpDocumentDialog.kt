package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDocumentDialog(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏带关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "帮助文档",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    item {
                        Text(
                            text = helpContent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private val helpContent = """
YHBotMaker 帮助文档

一、基础操作
-----------
• 启动机器人：点击右下角播放按钮
• 停止机器人：再次点击播放按钮
• 发送消息：点击底部发送按钮，输入内容并选择类型
• 清空日志：点击底部清空按钮
• 打开侧边栏：点击顶部菜单图标

二、代码编辑
-----------
侧边栏 → 编辑代码

• 功能代码：机器人启动时执行一次，用于初始化
  - 示例：print("机器人已启动")

• 循环监听代码：每次收到新消息时执行
  - callback 变量包含消息内容：
    - callback.contentType: 消息类型 (text/markdown/html)
    - callback.content: 消息内容
    - callback.senderId: 发送者ID
    - callback.senderNickname: 发送者昵称
    - callback.msgId: 消息ID

三、内置函数
-----------
• print(消息内容, 类型)
  显示日志消息，类型：0=普通 1=成功 2=错误 3=警告 4=系统 5=进行中

• sendText(内容)
  发送文本消息

• sendMarkdown(内容)
  发送 Markdown 格式消息

• sendHTML(内容)
  发送 HTML 格式消息

• recallMessage(消息ID)
  撤回指定消息

• http.get(url, headers)
  发起 GET 请求，返回响应内容
  - headers: 可选，table 格式如 {Authorization = "Bearer token"}

• http.post(url, data, headers, contentType)
  发起 POST 请求
  - data: 请求体字符串
  - headers: 可选
  - contentType: 可选，默认 "application/json"

• http.put(url, data, headers, contentType)
• http.delete(url, headers)

四、快捷指令
-----------
侧边栏 → 快捷指令

• 自动回复：设置关键词和回复内容
  当消息包含关键词时自动回复

• 快捷命令：设置命令ID和执行代码
  当收到带 commandId 的消息时执行对应代码

五、代码示例
-----------
-- 文本消息自动回复
if callback.contentType == "text" then
    print("收到消息: " .. callback.content.text, 0)
    sendText("已收到你的消息")
end

-- 关键词匹配
if callback.contentType == "text" then
    local msg = callback.content.text
    if msg:match("帮助") then
        sendText("发送【功能】查看可用命令")
    end
end

-- 使用 HTTP 请求
local res = http.get("https://api.example.com/data")
print("API返回: " .. res)

-- 快捷命令示例（设置 commandId=1）
if callback.commandId == 1 then
    sendText("执行命令成功")
end

六、注意事项
-----------
• 代码修改后需停止机器人再启动才能生效
• 请求间隔建议 2000ms 以上，过快可能被封
• 循环代码中注意避免死循环
• 使用 pcall 包裹可能出错的代码
""".trimIndent()