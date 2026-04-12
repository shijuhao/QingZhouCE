package com.example.toolbox.functionPage

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.toolbox.data.function.FunctionItem
import com.example.toolbox.data.function.SearchFunctionModel


@Composable
fun SearchFunctionItem(
    function: SearchFunctionModel,
    modifier: Modifier = Modifier,
    onLongPress: ((SearchFunctionModel) -> Unit)? = null,
    isFavorite: Boolean = false
) {
    val context = LocalContext.current

    val cornerRadius = 24.dp

    Card(
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius, bottomStart = cornerRadius, bottomEnd = cornerRadius),
        modifier = modifier.combinedClickable(
            enabled = true,
            onClick = {
                try {
                    val intent = Intent(context, Class.forName(function.function.activity))
                    context.startActivity(intent)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(context, "功能暂不可用", Toast.LENGTH_SHORT).show()
                }
            },
            onLongClick = {
                onLongPress?.invoke(function)
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = function.function.icon ?: Icons.Default.Person,
                    contentDescription = "${function.function.name}图标",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = function.function.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // 收藏图标指示
                    if (isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "已收藏",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = function.categoryIcon?.toPainter() ?: rememberVectorPainter(Icons.Default.Functions),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = function.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GridFunctionItem(
    function: FunctionItem,
    modifier: Modifier = Modifier,
    onLongPress: ((FunctionItem) -> Unit)? = null,
    isFavorite: Boolean = false
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .padding(2.dp)
            .combinedClickable(
                enabled = true,
                onClick = {
                    try {
                        val intent = Intent(context, Class.forName(function.activity))
                        context.startActivity(intent)
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                        Toast.makeText(context, "功能暂不可用", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    onLongPress?.invoke(function)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFavorite) {
                    Box(
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Icon(
                            imageVector = function.icon ?: Icons.Default.Functions,
                            contentDescription = "${function.name}图标",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "已收藏",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = function.icon ?: Icons.Default.Functions,
                        contentDescription = "${function.name}图标",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = function.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                maxLines = 2
            )
        }
    }
}