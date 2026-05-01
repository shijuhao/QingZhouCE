package com.example.toolbox.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.toolbox.AppDestinations
import com.example.toolbox.TopLevelDestinations
import com.example.toolbox.ui.theme.ToolBoxTheme

class DefaultStartPageActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("app_preferences", MODE_PRIVATE)
            
            var currentStartRoute by remember {
                mutableStateOf(prefs.getString("default_start_page", "主页") ?: "主页")
            }
            
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            
            ToolBoxTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                Text("默认启动页")
                            },
                            navigationIcon = {
                                FilledTonalIconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    DefaultStartPageScreen(
                        currentStartRoute = currentStartRoute,
                        innerPadding = innerPadding,
                        onStartRouteChange = { _, pageName ->
                            currentStartRoute = pageName
                            prefs.edit().apply {
                                putString("default_start_page", pageName)
                                apply()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultStartPageScreen(
    currentStartRoute: String,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onStartRouteChange: (String, String) -> Unit
) {
    val startPages = listOf(
        AppDestinations.HOME,
        TopLevelDestinations.LFCommunity,
        TopLevelDestinations.YHBotMaker
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            SettingsGroup(
                title = "选择默认启动页",
                items = startPages.map { destination ->
                    {
                        DefaultStartPageRadioItem(
                            destination = destination,
                            isSelected = currentStartRoute == destination.label,
                            onStartRouteChange = onStartRouteChange
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun DefaultStartPageRadioItem(
    destination: com.example.toolbox.NavDestination,
    isSelected: Boolean,
    onStartRouteChange: (String, String) -> Unit
) {
    SettingsCustomItem(onClick = { onStartRouteChange(destination.route, destination.label) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .selectable(
                    selected = isSelected,
                    onClick = { onStartRouteChange(destination.route, destination.label) },
                    role = Role.RadioButton
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (destination.description.isNotEmpty()) {
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}
