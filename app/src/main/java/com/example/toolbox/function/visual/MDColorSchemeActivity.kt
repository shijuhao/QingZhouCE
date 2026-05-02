package com.example.toolbox.function.visual

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.toolbox.ui.theme.ToolBoxTheme
import androidx.compose.ui.graphics.Color as ComposeColor

class MDColorSchemeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    println(innerPadding)
                    MDColorSchemeScreen()
                }
            }
        }
    }
}

data class ColorItem(val name: String, val color: ComposeColor, val contentColor: ComposeColor)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MDColorSchemeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val currentColorScheme = MaterialTheme.colorScheme

    val colorPalette = remember(currentColorScheme) {
        listOf(
            "Primary" to currentColorScheme.primary,
            "Primary Container" to currentColorScheme.primaryContainer,
            "On Primary" to currentColorScheme.onPrimary,
            "On Primary Container" to currentColorScheme.onPrimaryContainer,

            "Secondary" to currentColorScheme.secondary,
            "Secondary Container" to currentColorScheme.secondaryContainer,
            "On Secondary" to currentColorScheme.onSecondary,
            "On Secondary Container" to currentColorScheme.onSecondaryContainer,

            "Tertiary" to currentColorScheme.tertiary,
            "Tertiary Container" to currentColorScheme.tertiaryContainer,
            "On Tertiary" to currentColorScheme.onTertiary,
            "On Tertiary Container" to currentColorScheme.onTertiaryContainer,

            "Error" to currentColorScheme.error,
            "Error Container" to currentColorScheme.errorContainer,
            "On Error" to currentColorScheme.onError,
            "On Error Container" to currentColorScheme.onErrorContainer,

            "Background" to currentColorScheme.background,
            "On Background" to currentColorScheme.onBackground,

            "Surface" to currentColorScheme.surface,
            "Surface Variant" to currentColorScheme.surfaceVariant,
            "On Surface" to currentColorScheme.onSurface,
            "On Surface Variant" to currentColorScheme.onSurfaceVariant,

            "Outline" to currentColorScheme.outline,
            "Outline Variant" to currentColorScheme.outlineVariant,

            "Inverse Primary" to currentColorScheme.inversePrimary,
            "Inverse Surface" to currentColorScheme.inverseSurface,
            "Inverse On Surface" to currentColorScheme.inverseOnSurface,

            "Scrim" to currentColorScheme.scrim,
            "Surface Bright" to currentColorScheme.surfaceBright,
            "Surface Dim" to currentColorScheme.surfaceDim,
            "Surface Container" to currentColorScheme.surfaceContainer,
            "Surface Container Lowest" to currentColorScheme.surfaceContainerLowest,
            "Surface Container Low" to currentColorScheme.surfaceContainerLow,
            "Surface Container High" to currentColorScheme.surfaceContainerHigh,
            "Surface Container Highest" to currentColorScheme.surfaceContainerHighest,
        ).map { (name, color) ->
            ColorItem(name, color,
                // 根据颜色亮度自动选择内容颜色，以确保对比度
                if (color.luminance() > 0.5f) ComposeColor.Black else ComposeColor.White
            )
        }
    }

    fun copyToClipboard(label: String, colorHex: String) {
        val clipboard = context.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MD Color $label", colorHex)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制 $label: $colorHex", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text("MD3 配色参考") },
            navigationIcon = {
                FilledTonalIconButton(onClick = { (context as Activity).finish() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Material Design 3 配色",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击每个颜色块可复制其十六进制值。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(colorPalette) { colorItem ->
                ColorCard(colorItem = colorItem) { label, hex ->
                    copyToClipboard(label, hex)
                }
            }

            item {
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }
}

@Composable
fun ColorCard(colorItem: ColorItem, onCopy: (String, String) -> Unit) {
    val hexColor = String.format("#%06X", (0xFFFFFF and colorItem.color.toArgb()))

    Surface(
        color = colorItem.color,
        contentColor = colorItem.contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(colorItem.name, hexColor) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = colorItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current
                )
                Text(
                    text = hexColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "复制 ${colorItem.name} 颜色",
                modifier = Modifier.size(24.dp),
                tint = LocalContentColor.current
            )
        }
    }
}

fun ComposeColor.toArgb(): Int {
    // 将 Float 颜色通道值 (0.0f-1.0f) 转换为 Int (0-255)
    return Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

fun ComposeColor.luminance(): Float {
    return (0.2126f * red) + (0.7152f * green) + (0.0722f * blue)
}

@Preview(showBackground = true)
@Composable
fun MDColorSchemeScreenPreview() {
    ToolBoxTheme {
        MDColorSchemeScreen()
    }
}