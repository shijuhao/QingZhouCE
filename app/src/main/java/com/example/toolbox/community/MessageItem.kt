package com.example.toolbox.community

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.data.community.Category
import com.example.toolbox.data.community.Message
import com.example.toolbox.mine.getLevelIconRes
import com.example.toolbox.utils.MarkdownRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CategoryItem(
    select: Boolean,
    category: Category,
    onItemClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                onItemClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (select)
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else
                    Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = category.avatar_url,
                contentDescription = "${category.name}头像",
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                placeholder = painterResource(R.drawable.user),
                error = painterResource(R.drawable.user)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                category.latest_message?.let { latestMessage ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${latestMessage.sender_username}: ${latestMessage.content}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (category.stats.has_unread) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${category.stats.unread_count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onReply: () -> Unit,
    onHistory: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    var showMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current

    val userStatus = TokenManager.getTagStatus(context)
    val currentUserId = TokenManager.getUserID(context)

    val usernameColor = when (message.tag_status) {
        1 -> colorScheme.error
        2 -> colorScheme.tertiary
        3 -> colorScheme.secondary
        4 -> colorScheme.primary
        else -> colorScheme.onSurface
    }

    Surface(
        onClick = {
            context.startActivity(
                Intent(context, PostDetailActivity::class.java).apply {
                    putExtra("msgid", message.message_id)
                }
            )
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = message.avatar_url ?: R.drawable.user,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            context.startActivity(
                                Intent(context, UserInfoActivity::class.java).apply {
                                    putExtra("userId", message.userid)
                                }
                            )
                        },
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.user),
                    error = painterResource(R.drawable.user)
                )

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                        .clickable {
                            context.startActivity(
                                Intent(context, UserInfoActivity::class.java).apply {
                                    putExtra("userId", message.userid)
                                }
                            )
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = usernameColor
                        )

                        val levelTag = buildString {
                            if (message.user_level > 0) append("${message.user_level}")
                            if (message.tag.isNotEmpty()) {
                                append(" " + message.tag)
                            }
                        }.trim()
                        
                        if (levelTag.isNotEmpty()) {
                            Surface(
                                color = colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Image(
                                        modifier = Modifier.size(16.dp),
                                        contentDescription = null,
                                        painter = painterResource(getLevelIconRes(message.user_level.toString()))
                                    )
                                    Text(
                                        text = levelTag,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.timestamp_user,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline
                        )

                        if (message.is_private) {
                            val label = message.visible_to?.let { userList ->
                                if (userList.size > 2) {
                                    "悄悄发送（${userList.size} 人可见）"
                                } else if (message.userid == currentUserId && userList.size == 2) {
                                    val otherUserId = userList.find { it != currentUserId } ?: ""
                                    "悄悄发送给用户 $otherUserId"
                                } else {
                                    "仅你可见"
                                }
                            }
                            Text(
                                text = " • $label",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.outline
                            )
                        }
                    }
                }

                // 更多菜单
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                    
                    DropdownMenu(
                        expanded = showMenu, 
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("复制文本") },
                            onClick = {
                                clipboard.nativeClipboard.setPrimaryClip(
                                    ClipData.newPlainText("text", message.content.content ?: message.content.text ?: "")
                                )
                                showMenu = false
                                Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { 
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)) 
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("+1") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    try {
                                        val token = TokenManager.get(context)
                                        if (token != null) {
                                            addOne(messageId = message.message_id, token = token)
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "已+1", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "+1失败", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AddCircle, null, Modifier.size(18.dp))
                            }
                        )
                        
                        if (message.userid == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("编辑留言") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                                }
                            )
                        }

                        if (message.userid == currentUserId || userStatus == 1) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "删除留言", 
                                        color = MaterialTheme.colorScheme.error
                                    ) 
                                },
                                onClick = { 
                                    showMenu = false
                                    onDelete() 
                                },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Delete, 
                                        null, 
                                        Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    ) 
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!message.content.title.isNullOrEmpty()) {
                Text(
                    text = message.content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            val rawText = message.content.text ?: message.content.content ?: ""
            if (rawText.isNotEmpty()) {
                if (message.is_markdown) {
                    MarkdownRenderer.Render(
                        content = rawText,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = rawText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            message.content.images?.takeIf { it.isNotEmpty() }?.let { images ->
                FlowRow(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    images.take(9).forEachIndexed { idx, img ->
                        AsyncImage(
                            model = img,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.32f)
                                .aspectRatio(1f)
                                .clickable {
                                    onImageClick(images, idx)
                                }
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.user),
                            error = painterResource(R.drawable.user)
                        )
                    }
                }
            }

            if (message.is_referenced && message.referenced_message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, 
                        colorScheme.outlineVariant
                    ),
                    onClick = {
                        context.startActivity(
                            Intent(context, PostDetailActivity::class.java).apply {
                                putExtra("msgid", message.referenced_message.message_id)
                            }
                        )
                    }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "回复 @${message.referenced_message.sender_username}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = message.referenced_message.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部操作栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (message.is_edited == 1) {
                    IconButton(
                        onClick = onHistory, 
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            null,
                            tint = colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 评论按钮
                TextButton(onClick = onReply) {
                    Icon(
                        Icons.Default.ChatBubbleOutline, 
                        null, 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${message.reference_count}", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 点赞按钮
                TextButton(
                    onClick = onLike,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (message.is_liked) 
                            colorScheme.primary 
                        else 
                            colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = if (message.is_liked) 
                            Icons.Filled.ThumbUp 
                        else 
                            Icons.Default.ThumbUpOffAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${message.like_count}", fontSize = 12.sp)
                }
            }
        }
    }
}