package com.example.toolbox.utils

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.toolbox.LoginActivity
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.community.UserInfoActivity

@Composable
fun UserAvatar(
    avatarUrl: String,
    userId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val token = TokenManager.get(context)
    
    AsyncImage(
        model = if (avatarUrl.isNotEmpty() && avatarUrl != "null") avatarUrl else null,
        contentDescription = "用户头像",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .padding(start = 4.dp, end = 8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .clickable {
                if (token != null) {
                    val intent = Intent(context, UserInfoActivity::class.java).apply {
                        putExtra("userId", userId)
                    }
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                }
            },
        placeholder = painterResource(id = R.drawable.user),
        error = painterResource(id = R.drawable.user)
    )
}
