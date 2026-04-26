@file:Suppress("DEPRECATION")

package com.example.toolbox.resourceLib

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.toolbox.data.community.ResourceItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ResourceLibScreen(
    onMenuClick: () -> Unit = {},
    viewModel: ResourceViewModel = viewModel()
) {
    val context = LocalContext.current

    val tabs = listOf(
        "开源软件",
        "实用工具",
        "生活便利",
        "影音娱乐",
        "玩机工具",
        "社交",
        "金融理财",
        "网页",
        "其他"
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) } // 标记是否已执行过搜索

    // 只有执行过搜索后才显示搜索结果
    val displayList = if (isSearchActive && hasSearched) {
        viewModel.searchResultList
    } else {
        emptyList()
    }

    // 监听加载状态，加载完成后重置 isSearching
    LaunchedEffect(viewModel.isLoading) {
        if (!viewModel.isLoading && isSearching) {
            isSearching = false
        }
    }

    LaunchedEffect(selectedTabIndex) {
        val categoryId = when (selectedTabIndex) {
            0 -> 2
            8 -> 1
            else -> selectedTabIndex + 2
        }
        viewModel.fetchResources(categoryId)
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    } else {
                        slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "topBarTransition"
            ) { targetIsSearching ->
                if (targetIsSearching) {
                    SearchBar(
                        modifier = Modifier.fillMaxWidth(),
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { 
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                hasSearched = true
                                viewModel.searchResources(searchQuery, categoryId = null)
                            }
                        },
                        active = true,
                        onActiveChange = { active ->
                            if (!active) {
                                isSearchActive = false
                                isSearching = false
                                hasSearched = false
                                searchQuery = ""
                                val categoryId = when (selectedTabIndex) {
                                    0 -> 2
                                    8 -> 1
                                    else -> selectedTabIndex + 2
                                }
                                viewModel.fetchResources(categoryId)
                            }
                        },
                        placeholder = { Text("搜索该区资源") },
                        leadingIcon = {
                            IconButton(onClick = { isSearchActive = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (searchQuery.isNotBlank()) {
                                    isSearching = true
                                    hasSearched = true
                                    viewModel.searchResources(searchQuery, categoryId = null)
                                }
                            }) {
                                Icon(Icons.Default.Search, "搜索")
                            }
                        }
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 0.5.dp
                            )
                            if (isSearching && viewModel.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator()
                                }
                            } else if (hasSearched && displayList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        "未找到相关资源",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (!hasSearched) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 32.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        "输入关键词搜索资源",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(displayList) { item ->
                                        ResourceCard(item) {
                                            val intent =
                                                Intent(context, ResourceDetailActivity::class.java).apply {
                                                    putExtra("item", item)
                                                }
                                            context.startActivity(intent)
                                        }
                                    }
                                    item {
                                        Spacer(
                                            modifier = Modifier.height(
                                                WindowInsets.navigationBars.asPaddingValues()
                                                    .calculateBottomPadding()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    TopAppBar(
                        title = { Text("资源库") },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                 onMenuClick()
                            }) {
                                Icon(Icons.Default.Menu, null)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = Color.Transparent
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isLoading) {
                    ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.resourceList) { item ->
                            ResourceCard(item) {
                                val intent =
                                    Intent(context, ResourceDetailActivity::class.java).apply {
                                        putExtra("item", item)
                                    }
                                context.startActivity(intent)
                            }
                        }
                        item {
                            Spacer(
                                modifier = Modifier.height(
                                    WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceCard(item: ResourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0x157d7d7d)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.icon_url,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(item.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("版本号：${item.version}", fontSize = 13.sp)
                Text("上传时间：${item.release_date}", fontSize = 13.sp)
            }
        }
    }
}