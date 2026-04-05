package com.example.toolbox.utils

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

object MarkdownRenderer {
    private const val TAG = "MarkdownRenderer"

    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        content: String,
        onLinkClick: ((String) -> Unit)? = null,
        onImageClick: ((String, String) -> Unit)? = null
    ) {
        val context = LocalContext.current
        val defaultUriHandler = LocalUriHandler.current

        val customUriHandler = remember(onLinkClick) {
            object : UriHandler {
                override fun openUri(uri: String) {
                    if (onLinkClick != null) {
                        onLinkClick(uri)
                    } else {
                        try {
                            defaultUriHandler.openUri(uri)
                        } catch (_: Exception) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri.toUri())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                }
            }
        }

        val components = markdownComponents(
            image = { componentData ->
                val imageUrl = componentData.content
                val altText = componentData.node.getAltTextFromNode(componentData.content)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onImageClick?.invoke(imageUrl, altText)
                        }
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = altText.ifEmpty { null },
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        )

        CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
            Markdown(
                content = content,
                modifier = modifier,
                components = components,
                imageTransformer = Coil3ImageTransformerImpl,  // 图片加载器
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.displayLarge,
                    h2 = MaterialTheme.typography.displayMedium,
                    h3 = MaterialTheme.typography.displaySmall,
                    h4 = MaterialTheme.typography.headlineMedium,
                    h5 = MaterialTheme.typography.headlineSmall,
                    h6 = MaterialTheme.typography.titleLarge,
                    text = MaterialTheme.typography.bodyMedium,
                    paragraph = MaterialTheme.typography.bodyMedium,
                    code = MaterialTheme.typography.bodyMedium,
                    inlineCode = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        background = androidx.compose.ui.graphics.Color.LightGray
                    ),
                    quote = MaterialTheme.typography.bodyMedium,
                    ordered = MaterialTheme.typography.bodyLarge,
                    bullet = MaterialTheme.typography.bodyLarge,
                    list = MaterialTheme.typography.bodyMedium,
                    textLink = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    table = MaterialTheme.typography.bodyMedium
                )
            )
        }
    }

    private fun org.intellij.markdown.ast.ASTNode.getAltTextFromNode(content: String): String {
        val linkTextNode = findChildOfType(org.intellij.markdown.MarkdownElementTypes.LINK_TEXT)
        if (linkTextNode != null) {
            val text = linkTextNode.getTextInNode(content).toString()
            return text.removeSurrounding("[", "]")
        }
        return ""
    }
}