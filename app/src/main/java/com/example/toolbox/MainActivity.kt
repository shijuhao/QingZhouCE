package com.example.toolbox

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.toolbox.utils.UpdateInfo
import com.example.toolbox.utils.checkForUpdateWithDetails
import com.example.toolbox.settings.UpdateDialog
import com.example.toolbox.utils.getAppVersionInfo
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.toolbox.function.yunhu.yhbotmaker.BotManagerScreen
import com.example.toolbox.functionPage.HomeScreen
import com.example.toolbox.guide.GuideActivity
import com.example.toolbox.liFangCommunity.AuthManager
import com.example.toolbox.liFangCommunity.CubeNetworkManager
import com.example.toolbox.liFangCommunity.ProfileScreen_LF
import com.example.toolbox.message.MessageScreen
import com.example.toolbox.mine.ProfileScreen
import com.example.toolbox.mine.UserBottomSheet
import com.example.toolbox.resourceLib.ResourceLibScreen
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = this.getSharedPreferences("app_preferences", MODE_PRIVATE)
        val isFinishGuide = prefs.getBoolean("guideFinished", false)
        if (!isFinishGuide) {
            val intent = Intent(this, GuideActivity::class.java)
            this.startActivity(intent)
            finish()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                MyApplicationApp()
            }
        }
    }
}

@Composable
fun MyApplicationApp() {
    val mainViewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }
    
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    var autoUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    var token by remember { mutableStateOf(TokenManager.get(context)) }

    LaunchedEffect(Unit) {
        token?.let { mainViewModel.refreshUserInfo(it) }
    }
    
    LaunchedEffect(Unit) {
        val autoCheckEnabled = prefs.getBoolean("autoCheckUpdate", true)
        if (autoCheckEnabled) {
            val updateChannel = prefs.getString("update_channel", "stable") ?: "stable"
            val includePreRelease = updateChannel == "prerelease"
            
            val info = checkForUpdateWithDetails(
                context = context,
                includePreRelease = includePreRelease
            )
            if (info != null) {
                autoUpdateInfo = info
                showAutoUpdateDialog = true
            }
        }
    }

    val tokenListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "safeToken") {
                token = TokenManager.get(context)
                if (token != null) {
                    mainViewModel.refreshUserInfo(token!!)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        prefs.registerOnSharedPreferenceChangeListener(tokenListener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(tokenListener)
        }
    }

    val showChat = token != null

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val mainRoutes = remember(showChat) {
        AppDestinations.entries
            .filter { item ->
                when (item) {
                    AppDestinations.CHAT -> showChat
                    else -> true
                }
            }
            .map { it.route }
    }
    val isMainPage by remember(currentRoute, mainRoutes) {
        derivedStateOf { currentRoute in mainRoutes }
    }

    var isBottomBarVisible by remember { mutableStateOf(true) }

    val userInfo by mainViewModel.userInfo.collectAsState()
    val uiStatus by mainViewModel.uiStatus.collectAsState()
    val showDialog = uiStatus.showUserDialog
    val userName = userInfo.name
    val userId = userInfo.id
    val userAvatar = userInfo.avatar

    val showSidebar by mainViewModel.showSidebar.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta < -15f && isBottomBarVisible) isBottomBarVisible = false
                if (delta > 15f && !isBottomBarVisible) isBottomBarVisible = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val visibleAppDestinations = remember(showChat) {
        AppDestinations.entries.filter { item ->
            when (item) {
                AppDestinations.CHAT -> showChat
                else -> true
            }
        }
    }
    
    val selectedRoute by remember(currentDestination, visibleAppDestinations) {
        derivedStateOf {
            val allDestinations: List<NavDestination> = 
                visibleAppDestinations.map { it as NavDestination } + 
                TopLevelDestinations.entries.map { it as NavDestination }
            allDestinations.find { item ->
                currentDestination?.hierarchy?.any { it.route == item.route } == true
            }?.route
        }
    }
    
    if (showAutoUpdateDialog && autoUpdateInfo != null) {
        UpdateDialog(
            updateInfo = autoUpdateInfo!!,
            currentVersion = context.getAppVersionInfo().versionName,
            onDismiss = { showAutoUpdateDialog = false },
            onConfirm = {
                val intent = Intent(Intent.ACTION_VIEW, autoUpdateInfo?.releaseUrl?.toUri())
                context.startActivity(intent)
                showAutoUpdateDialog = false
            }
        )
    }
    
    val mainContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(nestedScrollConnection)
        ) {
            MainContentNavHost(
                navController = navController,
                mainViewModel = mainViewModel,
                drawerState = drawerState,
                scope = scope,
                modifier = Modifier.fillMaxSize()
            )

            if (isMainPage) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                            NavigationBar {
                                visibleAppDestinations.forEach { item ->
                                    val isSelected = item.route == selectedRoute
                            
                                    NavigationBarItem(
                                        icon = {
                                            Crossfade(targetState = isSelected) { selected ->
                                                Icon(
                                                    imageVector = if (selected) item.icon else item.iconOutlined,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        label = {
                                            AnimatedVisibility(
                                                visible = isSelected,
                                                enter = fadeIn(animationSpec = tween(200)) +
                                                        scaleIn(initialScale = 0.5f, animationSpec = tween(200)),
                                                exit = fadeOut(animationSpec = tween(150)) +
                                                        scaleOut(targetScale = 0.5f, animationSpec = tween(150))
                                            ) {
                                                Text(item.label)
                                            }
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            UserBottomSheet(
                show = showDialog,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                onDismiss = { mainViewModel.changeUserDialogStatus(false) }
            )
        }
    }

    if (showSidebar) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(visibleAppDestinations) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.route == selectedRoute,
                                icon = { Icon(item.icon, contentDescription = null) },
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        items(TopLevelDestinations.entries) { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                selected = item.route == selectedRoute,
                                icon = { Icon(item.icon, contentDescription = null) },
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            },
            gesturesEnabled = true,
            content = { mainContent() }
        )
    } else {
        mainContent()
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = showDialog) {
        mainViewModel.changeUserDialogStatus(false)
    }
    BackHandler(enabled = !showDialog && !drawerState.isOpen) {
        if (currentRoute != AppDestinations.HOME.route && currentRoute != null) {
            navController.popBackStack()
        } else {
            val exitConfirmationEnabled = prefs.getBoolean("exit_confirmation", false)
            if (!exitConfirmationEnabled) {
                (context as? Activity)?.finish()
            } else {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedTime = now
                    Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun MainContentNavHost(
    navController: androidx.navigation.NavHostController,
    mainViewModel: MainViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AuthManager.initialize(context.applicationContext)
        CubeNetworkManager.initialize(context.applicationContext)
    }

    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(AppDestinations.HOME.route) {
            HomeScreen {
                scope.launch { drawerState.open() }
            }
        }
        composable(AppDestinations.CHAT.route) {
            MessageScreen {
                scope.launch { drawerState.open() }
            }
        }
        composable(AppDestinations.RESOURCE.route) {
            ResourceLibScreen(
                {
                    scope.launch { drawerState.open() }
                }
            )
        }
        composable(AppDestinations.PROFILE.route) {
            ProfileScreen(
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                mainViewModel = mainViewModel
            )
        }
        composable(TopLevelDestinations.LFCommunity.route) {
            ProfileScreen_LF(
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }
        composable(TopLevelDestinations.YHBotMaker.route) {
            BotManagerScreen(
                isMain = true,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }
    }
}

interface NavDestination {
    val route: String
    val label: String
    val icon: ImageVector
}

enum class TopLevelDestinations(
    override val route: String,
    override val label: String,
    override val icon: ImageVector
) : NavDestination {
    LFCommunity("lfcommunity", "立方论坛", Icons.Default.ChatBubbleOutline),
    YHBotMaker("yhbotmaker", "YHBotMaker", Icons.Default.Android)
}

enum class AppDestinations(
    override val route: String,
    override val label: String,
    override val icon: ImageVector,
    val iconOutlined: ImageVector
) : NavDestination {
    HOME("home", "主页", Icons.Filled.Home, Icons.Outlined.Home),
    CHAT("chat", "会话", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    RESOURCE("resource", "资源库", Icons.Filled.Inbox, Icons.Outlined.Inbox),
    PROFILE("profile", "我", Icons.Filled.Person, Icons.Outlined.Person);
}