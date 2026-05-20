package com.example.toolbox.function.yunhu.yhbotmaker.runtime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.toolbox.utils.MarkdownRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDocumentDialog(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
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

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp)
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    MarkdownRenderer.Render(
                        content = helpContent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val helpContent = """
# YHBotMaker 帮助文档

## 一、基础操作

- **连接 WebSocket**：点击右下角连接按钮（Sync图标）
- **断开连接**：点击右下角断开按钮（Close图标）
- **发送消息**：点击底部发送按钮，输入接收者ID、接收类型和内容
- **清空日志**：点击底部清空按钮
- **打开侧边栏**：点击顶部菜单图标
- **连接状态**：顶部显示 ●在线 或 ○离线
- **黑屏模式**：点击底部灯泡图标，再点击灯泡关闭

## 二、消息图标说明

消息列表左侧图标代表不同类型：

- 📥 **收到消息 (青色)** - 收到 WebSocket 事件
- ✅ **操作成功 (绿色)** - 发送成功、自动回复成功、快捷命令成功
- ℹ️ **系统消息 (白色)** - 连接状态、启动信息等
- ❌ **报错 (红色)** - 发送失败、连接错误等
- 🤖 **其他 (主题色)** - Lua print 输出等

## 三、代码编辑

侧边栏 → 编辑代码

- **功能代码（启动代码）**：WebSocket连接建立后执行一次，用于初始化
  - 示例：`print("机器人已启动", 4)`

- **事件处理代码**：每次收到WebSocket事件时执行
  - `event` 变量包含完整的事件数据

## 四、事件订阅说明

WebSocket 推送的事件格式请参阅官方文档：

👉 [云湖开放平台 - 事件订阅文档](https://www.yhchat.com/document/300-310)

主要事件类型：
- `message.receive.normal` - 普通消息
- `message.receive.instruction` - 指令消息
- `bot.followed` - 关注机器人
- `bot.unfollowed` - 取消关注
- `group.join` - 加入群
- `group.leave` - 退出群
- `button.report.inline` - 按钮点击
- `bot.shortcut.menu` - 快捷菜单
- `bot.setting` - 机器人设置

## 五、内置函数

### 日志输出
```

print(消息内容, 类型)

```
类型：0=普通 1=成功 2=错误 3=警告 4=系统 5=进行中

### 发送消息
```

sendText(接收者ID, 接收类型, 内容)
sendMarkdown(接收者ID, 接收类型, 内容)
sendHTML(接收者ID, 接收类型, 内容)

```
- 接收类型: `"user"` 或 `"group"`

### 撤回消息
```

recallMessage(聊天ID, 聊天类型, 消息ID)

```

### HTTP 请求
```

http.get(url, headers)
http.post(url, data, headers, contentType)
http.put(url, data, headers, contentType)
http.delete(url, headers)

```

### 本地存储（SharedData）
```

sharedDataSet(key, value)      -- 保存数据
sharedDataGet(key, defaultValue) -- 读取数据
sharedDataGetAll()             -- 获取所有数据
sharedDataRemove(key)          -- 删除数据
sharedDataClear()              -- 清空所有数据

```

## 六、代码示例

### 处理消息并回复
```lua
if event.header.eventType == "message.receive.normal" then
    local senderId = event.event.sender.senderId
    local text = event.event.message.content.text
    
    if text == "你好" then
        sendText(senderId, "user", "你好，我是机器人！")
    elseif text == "时间" then
        sendText(senderId, "user", os.date("%Y-%m-%d %H:%M:%S"))
    end
end
```

### 撤回消息

```lua
if event.header.eventType == "message.receive.normal" then
    local chatId = event.event.chat.chatId
    local chatType = event.event.chat.chatType
    local msgId = event.event.message.msgId
    local text = event.event.message.content.text
    
    if text == "撤回" then
        recallMessage(chatId, chatType, msgId)
        sendText(chatId, chatType, "已撤回上一条消息")
    end
end
```

### 使用 SharedData 记录状态

```lua
if event.header.eventType == "message.receive.normal" then
    local senderId = event.event.sender.senderId
    local count = sharedDataGet(senderId, "0")
    local num = tonumber(count)
    
    num = num + 1
    sharedDataSet(senderId, tostring(num))
    sendText(senderId, "user", "你已经说了 " .. num .. " 次")
end
```

### HTTP 请求示例

```lua
if event.header.eventType == "message.receive.normal" then
    local text = event.event.message.content.text
    if text:match("^天气") then
        local city = text:gsub("天气", "")
        local res = http.get("https://api.weather.com/" .. city)
        local senderId = event.event.sender.senderId
        sendText(senderId, "user", "天气查询结果：" .. res)
    end
end
```

## 七、注意事项

- 代码修改后点击保存即可生效，无需重启机器人
- WebSocket 连接需要手动点击右下角按钮
- sendText/sendMarkdown/sendHTML 需要3个参数：接收者ID、接收类型、内容
- recallMessage 需要3个参数：聊天ID、聊天类型、消息ID
- 接收类型：user(用户) 或 group(群)
- sharedData 存储的数据每个机器人独立，重启后保留
- 事件代码中注意避免死循环
- 使用 pcall 包裹可能出错的代码
""".trimIndent()