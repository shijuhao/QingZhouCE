package com.example.toolbox.guide

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.toolbox.LoginActivity
import com.example.toolbox.MainActivity
import com.example.toolbox.R
import com.example.toolbox.TokenManager
import com.example.toolbox.mine.OnResumeScreen
import com.example.toolbox.settings.ThemeSwitchScreen
import com.example.toolbox.settings.ThemeViewModel
import com.example.toolbox.ui.theme.ToolBoxTheme
import com.example.toolbox.utils.MarkdownRenderer

class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Greeting()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Greeting() {
    val context = LocalContext.current

    val viewModel: GuideViewModel = viewModel()
    val guideData by viewModel.guide.collectAsState()

    val page by remember { derivedStateOf { guideData.page } }

    val targetProgress = page / 5f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 500),
        label = "ProgressAnimation"
    )

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .clipToBounds()) {
            Icon(
                imageVector = when (page) {
                    1 -> Icons.Default.Build
                    2 -> Icons.Default.Palette
                    3 -> Icons.AutoMirrored.Filled.TextSnippet
                    4 -> Icons.Default.Person
                    else -> Icons.Default.SettingsSuggest
                },
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-60).dp)
                    .size(320.dp)
                    .alpha(0.08f),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "PageTransition"
                    ) { targetPage ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            GuidePageContent(targetPage)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (page != 1) {
                            FilledTonalIconButton(
                                modifier = Modifier.size(60.dp),
                                onClick = {
                                    viewModel.updatePage(page - 1)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        FilledIconButton(
                            modifier = Modifier.size(60.dp),
                            onClick = {
                                if (page < 5) {
                                    viewModel.updatePage(page + 1)
                                } else {
                                    val prefs =
                                        context.getSharedPreferences(
                                            "app_preferences",
                                            MODE_PRIVATE
                                        )
                                    prefs.edit().apply {
                                        putBoolean("guideFinished", true)
                                        apply()
                                    }
                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                    (context as Activity).finish()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (page != 5) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                contentDescription = "Next"
                            )
                        }
                    }

                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(80.dp)
                            .align(Alignment.Center),
                        progress = { animatedProgress }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidePageContent(page: Int) {
    val context = LocalContext.current
    val viewModel: ThemeViewModel = viewModel()

    val userRules = stringResource(R.string.user_rules)

    viewModel.loadSavedTheme(context)

    val currentTheme by viewModel.currentTheme.collectAsState()
    val monetEnabled by viewModel.monetEnabled.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()
    var isLogin by remember { mutableStateOf(false) }

    isLogin = (TokenManager.get(context) ?: "null") != "null"

    OnResumeScreen(
        onResume = {
            val currentToken = TokenManager.get(context) ?: "null"
            isLogin = currentToken != "null"
        }
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (page) {
            1 -> {
                Icon(
                    imageVector = Icons.Rounded.Terrain,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "欢迎使用${stringResource(id = R.string.app_name)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "极简、快速、美观、实用",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .alpha(0.8f)
                )
            }
            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            modifier = Modifier.size(30.dp),
                            imageVector = Icons.Default.Palette,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "设置主题",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "确保初次使用时显示为您最喜爱的主题",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    ThemeSwitchScreen(
                        currentTheme = currentTheme,
                        monetEnabled = monetEnabled,
                        colorTheme = colorTheme,
                        innerPadding = PaddingValues(0.dp),
                        onThemeChange = { theme -> viewModel.changeTheme(theme, context) },
                        onMonetToggle = { viewModel.toggleMonetEnabled(context) },
                        onColorThemeChange = { theme -> viewModel.changeColorTheme(theme, context) }
                    )
                }
            }
            3 -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            modifier = Modifier.size(30.dp),
                            imageVector = Icons.Default.Person,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "隐私政策",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 5.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "请认真阅读隐私政策",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        MarkdownRenderer.Render(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            content = userRules
                        )
                    }
                }
            }
            4 -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "登录轻昼账号",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isLogin) "您已成功登录" else "登录以使用社区等功能（点击下一步可跳过登录）",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
                if (!isLogin) {
                    Button(
                        onClick = {
                            val intent =
                                Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                        }
                    ) {
                        Text("登录")
                    }
                }
            }
            5 -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "您已设置完成",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "感谢您选择${stringResource(id = R.string.app_name)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .alpha(0.8f)
                )
            }
        }
    }
}