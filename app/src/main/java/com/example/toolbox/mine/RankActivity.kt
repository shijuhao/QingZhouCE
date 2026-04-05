package com.example.toolbox.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.toolbox.R
import com.example.toolbox.community.UserInfoActivity
import com.example.toolbox.data.mine.RankResponse
import com.example.toolbox.data.mine.RankUser

class RankActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                RankScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreen() {
    val context = LocalContext.current
    val tabs = listOf("等级榜", "财富榜", "活跃榜", "投稿榜")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("排行榜") },
                    navigationIcon = {
                        FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                )
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            icon = {
                                val icon = when(index) {
                                    0 -> Icons.Default.MilitaryTech
                                    1 -> Icons.Default.MonetizationOn
                                    2 -> Icons.Default.LocalFireDepartment
                                    else -> Icons.Default.CloudUpload
                                }
                                Icon(icon, contentDescription = null)
                            },
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) { pageIndex ->
            RankList(type = pageIndex)
        }
    }
}

@Composable
fun RankList(type: Int) {
    var userList by remember { mutableStateOf(listOf<RankUser>()) }
    var isRefreshing by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val apiUrl = when (type) {
        0 -> "${ApiAddress}level_rank"
        1 -> "${ApiAddress}wealth_rank"
        2 -> "${ApiAddress}activity_rank"
        else -> "${ApiAddress}resource_rank"
    }

    LaunchedEffect(apiUrl) {
        isRefreshing = true
        scope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder().build()
            val request = if (type != 3)
                Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build()
            else
                Request.Builder()
                    .url(apiUrl)
                    .build()

            try {
                val response = client.newCall(request).execute()
                val json = response.body.string()
                val responseData = AppJson.json.decodeFromString<RankResponse>(json)
                withContext(Dispatchers.Main) {
                    userList = responseData.rankList
                    isRefreshing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isRefreshing = false }
            }
        }
    }

    if (isRefreshing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(userList) { index, user ->
                RankItem(index + 1, user, type)
            }
            item {
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }
}

fun getLevelIconRes(levelStr: String): Int {
    val lv = levelStr.toIntOrNull() ?: 0
    return when {
        lv >= 50 -> R.drawable.level6
        lv >= 40 -> R.drawable.level5
        lv >= 30 -> R.drawable.level4
        lv >= 20 -> R.drawable.level3
        lv >= 10 -> R.drawable.level2
        else -> R.drawable.level1
    }
}

@Composable
fun RankItem(rank: Int, user: RankUser, type: Int) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", user.userId)
                context.startActivity(intent)
            },
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rankColor = when (rank) {
                1 -> Color(0xFFFFD700)
                2 -> MaterialTheme.colorScheme.onSurface
                3 -> Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = rank.toString(),
                modifier = Modifier.width(35.dp),
                style = if (rank <= 3) {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = rankColor
            )

            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val detailText = when (type) {
                    0 -> "等级: LV${user.level} (EXP: ${user.exp})"
                    1 -> "金币: ${user.gold}"
                    2 -> "活跃度: ${user.activityPoint}"
                    else -> "投稿数: ${user.resourceCount}"
                }
                Text(detailText, fontSize = 13.sp, modifier = Modifier.alpha(0.7f))
            }

            // 等级图标逻辑
            if (type == 0) {
                val iconRes = getLevelIconRes(user.level.toString())
                if (iconRes != 0) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Level Icon",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}