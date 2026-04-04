package com.example.toolbox.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import androidx.core.net.toUri

class MoreInfoActivity : ComponentActivity() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Greeting() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("更多信息") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsGroup(
                    title = "许可证",
                    items = listOf(
                        {
                            SettingsItemCell(
                                icon = Icons.AutoMirrored.Filled.TextSnippet,
                                title = "Apache License 2.0",
                                subtitle = "点击查看官方网站",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://www.apache.org/licenses/LICENSE-2.0".toUri())
                                    context.startActivity(intent)
                                }
                            )
                        },
                        {
                            SettingsItemCell(
                                icon = Icons.AutoMirrored.Filled.TextSnippet,
                                title = "GNU General Public License v3.0",
                                subtitle = "点击查看官方网站",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/gpl-3.0.html".toUri())
                                    context.startActivity(intent)
                                }
                            )
                        }
                    )
                )
            }
        }
    }
}