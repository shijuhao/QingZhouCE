package com.example.toolbox.mine

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.toolbox.ui.theme.ToolBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResourceSourceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ToolBoxTheme {
                MyResourceSourceScreen(onBack = { finish() })
            }
        }
    }
}

data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val icon: Any?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyResourceSourceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedMode by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val installedApps = remember { mutableStateListOf<InstalledAppItem>() }
    var searchQuery by remember { mutableStateOf("") }
    var loadingApps by remember { mutableStateOf(false) }

    val filteredApps = run {
        val keyword = searchQuery.trim().lowercase()
        if (keyword.isEmpty()) {
            installedApps.toList()
        } else {
            installedApps.filter {
                it.name.lowercase().contains(keyword) || it.packageName.lowercase().contains(keyword)
            }
        }
    }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        context.startActivity(ResourceUploadActivity.createApkIntent(context, uri))
    }

    LaunchedEffect(selectedMode) {
        if (selectedMode == 1 && installedApps.isEmpty() && !loadingApps) {
            loadingApps = true
            scope.launch {
                val apps = withContext(Dispatchers.IO) {
                    loadInstalledApps(context.packageManager)
                }
                installedApps.clear()
                installedApps.addAll(apps)
                loadingApps = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择资源来源") },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("本地 APK", "已安装应用").forEachIndexed { index, title ->
                        OutlinedButton(
                            onClick = { selectedMode = index },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (selectedMode == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                apkPicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AppShortcut,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("选取本地 APK 文件", style = MaterialTheme.typography.titleMedium)
                            Text("点击后选择一个 .apk 文件并自动带入投稿页")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜索应用名或包名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loadingApps) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredApps) { app ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            context.startActivity(
                                                ResourceUploadActivity.createInstalledAppIntent(context, app.packageName)
                                            )
                                        },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = app.icon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.padding(start = 12.dp)) {
                                            Text(app.name, style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            if (filteredApps.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("没有匹配的应用")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadInstalledApps(packageManager: PackageManager): List<InstalledAppItem> {
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .asSequence()
        .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
        .map {
            InstalledAppItem(
                name = packageManager.getApplicationLabel(it).toString(),
                packageName = it.packageName,
                icon = packageManager.getApplicationIcon(it)
            )
        }
        .sortedBy { it.name.lowercase() }
        .toList()
}
