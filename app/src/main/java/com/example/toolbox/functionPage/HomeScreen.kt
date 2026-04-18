@file:Suppress("AssignedValueIsNeverRead")

package com.example.toolbox.functionPage

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.R
import com.example.toolbox.YiYanViewModel
import com.example.toolbox.data.function.FunctionCategory
import com.example.toolbox.data.function.FunctionItem
import com.example.toolbox.data.function.IconSource
import com.example.toolbox.data.function.SearchFunctionModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.edit

object ExpandedStatePrefs {
    private const val PREFS_NAME = "functionCategory_expanded"
    private const val KEY_PREFIX = "expanded_"

    fun saveExpanded(context: Context, categoryName: String, isExpanded: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_PREFIX + categoryName, isExpanded)
            }
    }

    fun getExpanded(context: Context, categoryName: String, default: Boolean): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFIX + categoryName, default)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var functionToFavorite by remember { mutableStateOf<FunctionItem?>(null) }
    
    var favoriteRefreshTrigger by remember { mutableIntStateOf(0) }
    
    val viewModel: YiYanViewModel = viewModel()
    val aWordText by viewModel.hitokoto.collectAsState()
    
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val yearWeekFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }
    val currentDate = remember { Date() }

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        if (expandedState.isEmpty()) {
            val loadedState = functionData.associate { category ->
                category.name to ExpandedStatePrefs.getExpanded(context, category.name, true)
            }.toMutableMap()
            loadedState["我的收藏"] = ExpandedStatePrefs.getExpanded(context, "我的收藏", false)
            expandedState.putAll(loadedState)
        }
    }

    val allFunctions = remember {
        functionData.flatMap { category ->
            category.functions.map { function ->
                SearchFunctionModel(function, category.name, category.icon)
            }
        }
    }

    val favoriteFunctions by remember(favoriteRefreshTrigger) {
        derivedStateOf { FavoriteManager.getFavoriteFunctions(context, allFunctions) }
    }

    val filteredFunctions = remember(searchText) {
        if (searchText.isBlank()) allFunctions
        else allFunctions.filter {
            it.function.name.contains(searchText, ignoreCase = true) ||
                    it.categoryName.contains(searchText, ignoreCase = true)
        }
    }

    BackHandler(enabled = isSearching) {
        isSearching = false
        searchText = ""
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSearching) 0.dp else 16.dp,
        label = "horizontalPadding"
    )
    val verticalPadding by animateDpAsState(
        targetValue = if (isSearching) 0.dp else 8.dp,
        label = "verticalPadding"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                if (!isSearching) {
                    LargeTopAppBar(
                        title = { Text(stringResource(id = R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { onMenuClick() }) {
                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                } else {
                    SearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        query = searchText,
                        onQueryChange = { searchText = it },
                        onSearch = { },
                        active = isSearching,
                        onActiveChange = {
                            isSearching = it
                            if (!it) searchText = ""
                        },
                        placeholder = { Text("搜索功能...") },
                        leadingIcon = {
                            if (isSearching) {
                                IconButton(onClick = { isSearching = false; searchText = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                }
                            } else {
                                Icon(Icons.Default.Search, null)
                            }
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Close, "清除")
                                }
                            }
                        }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredFunctions) { searchModel ->
                                SearchFunctionItem(
                                    function = searchModel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    onLongPress = {
                                        functionToFavorite = it.function
                                        showFavoriteDialog = true
                                    },
                                    isFavorite = FavoriteManager.isFavorite(
                                        context,
                                        searchModel.function.activity
                                    )
                                )
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dayOfMonth = dayFormat.format(currentDate)
                    val yearAndWeek = yearWeekFormat.format(currentDate)

                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "日月如梭",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 15.dp)
                            )

                            Row {
                                Spacer(modifier = Modifier.width(15.dp))
                                Text(
                                    text = aWordText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee()
                                        .padding(top = 8.dp, bottom = 12.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(start = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayOfMonth,
                                style = MaterialTheme.typography.headlineLarge,
                                modifier = Modifier.padding(top = 12.dp, end = 15.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = yearAndWeek,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 12.dp, end = 15.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                FavoriteCategoryCard(
                    favoritesExpanded = expandedState["我的收藏"] == true,
                    favoriteFunctions = favoriteFunctions,
                    onToggle = {
                        val newState = !(expandedState["我的收藏"] ?: false)
                        expandedState["我的收藏"] = newState
                        ExpandedStatePrefs.saveExpanded(context, "我的收藏", newState)
                    },
                    onLongPress = { function ->
                        functionToFavorite = function.function
                        showFavoriteDialog = true
                    }
                )
            }

            itemsIndexed(functionData, key = { _, category -> category.name }) { index, category ->
                val isExpanded = expandedState[category.name] == true
                CategoryCard(
                    category = category,
                    isExpanded = isExpanded,
                    isLastCategory = index == functionData.size - 1,
                    onToggle = {
                        val newState = !isExpanded
                        expandedState[category.name] = newState
                        ExpandedStatePrefs.saveExpanded(context, category.name, newState)
                    },
                    context = context,
                    onLongPress = {
                        functionToFavorite = it
                        showFavoriteDialog = true
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

    if (showFavoriteDialog && functionToFavorite != null) {
        val isFavorite = FavoriteManager.isFavorite(context, functionToFavorite!!.activity)
        AlertDialog(
            onDismissRequest = { showFavoriteDialog = false },
            title = { Text(text = if (isFavorite) "取消收藏" else "添加到收藏") },
            text = { Text(text = "功能：${functionToFavorite!!.name}") },
            confirmButton = {
                Text(
                    text = if (isFavorite) "取消收藏" else "收藏",
                    modifier = Modifier
                        .clickable {
                            if (isFavorite) {
                                FavoriteManager.removeFavorite(
                                    context,
                                    functionToFavorite!!.activity
                                )
                            } else {
                                FavoriteManager.addFavorite(context, functionToFavorite!!.activity)
                            }
                            favoriteRefreshTrigger++
                            showFavoriteDialog = false
                        }
                        .padding(16.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { showFavoriteDialog = false }
                        .padding(16.dp)
                )
            }
        )
    }
}

@Composable
private fun FavoriteCategoryCard(
    favoritesExpanded: Boolean,
    favoriteFunctions: List<SearchFunctionModel>,
    onToggle: () -> Unit,
    onLongPress: (SearchFunctionModel) -> Unit
) {
    val context = LocalContext.current
    val useCustomColor = IconColorPrefs.isEnabled(context)
    val iconTint = if (useCustomColor) {
        IconColorMap.getColor("red") ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }

    val cornerRadius = 24.dp
    val smallRadius = 4.dp

    Surface(
        shape = RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = smallRadius,
            bottomEnd = smallRadius
        ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconTint.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "我的收藏",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (favoriteFunctions.isNotEmpty()) {
                    Text(
                        "${favoriteFunctions.size}个收藏",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.6f)
                    )
                }

                val rotation by animateFloatAsState(if (favoritesExpanded) 0f else 90f, label = "")
                Icon(
                    Icons.Default.ExpandLess,
                    null,
                    modifier = Modifier
                        .rotate(rotation)
                        .alpha(0.6f)
                )
            }

            AnimatedVisibility(
                visible = favoritesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                    if (favoriteFunctions.isNotEmpty()) {
                        favoriteFunctions.map { it.function }.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { function ->
                                    GridFunctionItem(
                                        function = function,
                                        modifier = Modifier.weight(1f),
                                        onLongPress = { clickedFunction ->
                                            val model = favoriteFunctions.find { it.function == clickedFunction }
                                            model?.let { onLongPress(it) }
                                        },
                                        isFavorite = true
                                    )
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        Text(
                            "长按功能即可收藏",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .alpha(0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconSource.toPainter(): Painter = when (this) {
    is IconSource.Vector -> rememberVectorPainter(imageVector)
    is IconSource.Resource -> painterResource(id = resId)
}

@Composable
private fun CategoryCard(
    category: FunctionCategory,
    isExpanded: Boolean,
    isLastCategory: Boolean,
    onToggle: () -> Unit,
    context: Context,
    onLongPress: (FunctionItem) -> Unit
) {
    val cornerRadius = 24.dp
    val smallRadius = 4.dp
    val shape = if (isLastCategory) RoundedCornerShape(
        topStart = smallRadius,
        topEnd = smallRadius,
        bottomStart = cornerRadius,
        bottomEnd = cornerRadius
    )
    else RoundedCornerShape(smallRadius)

    val useCustomColor = IconColorPrefs.isEnabled(context)
    val iconTint = if (useCustomColor) {
        IconColorMap.getColor(category.iconColorName) ?: MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconTint.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = category.icon?.toPainter() ?: rememberVectorPainter(Icons.Default.Functions),
                        null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "共${category.functions.size}个",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val rotation by animateFloatAsState(if (isExpanded) 0f else 90f, label = "")
                Icon(
                    Icons.Default.ExpandLess,
                    null,
                    modifier = Modifier
                        .rotate(rotation)
                        .alpha(0.6f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                    category.functions.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                GridFunctionItem(
                                    function = item,
                                    modifier = Modifier.weight(1f),
                                    onLongPress = { onLongPress(it) },
                                    isFavorite = FavoriteManager.isFavorite(context, item.activity)
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}