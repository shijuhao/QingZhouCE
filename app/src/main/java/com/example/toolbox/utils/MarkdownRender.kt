package com.example.toolbox.utils

import android.content.Intent
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
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

object MarkdownRenderer {
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
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    }
                }
            }
        }
        val customImageComponent: MarkdownComponent = { componentModel ->
            val node = componentModel.node
            val contentText = componentModel.content

            if (node.type == MarkdownElementTypes.IMAGE) {

                val imageUrl = node.getURLFromNode(contentText)
                val altText = node.getAltTextFromNode(contentText)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            when {
                                onImageClick != null -> onImageClick(imageUrl, altText)
                                imageUrl.startsWith("http") -> customUriHandler.openUri(imageUrl)
                            }
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
        }

        val components = markdownComponents(
            image = customImageComponent
        )

        CompositionLocalProvider(LocalUriHandler provides customUriHandler) {
            Markdown(
                content = content,
                modifier = modifier,
                components = components,
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

    private fun ASTNode.getURLFromNode(content: String): String {
        val linkDestinationNode = findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
        if (linkDestinationNode != null) {
            return linkDestinationNode.getTextInNode(content).toString()
        }
        return ""
    }

    private fun ASTNode.getAltTextFromNode(content: String): String {
        val linkTextNode = findChildOfType(MarkdownElementTypes.LINK_TEXT)
        if (linkTextNode != null) {
            val text = linkTextNode.getTextInNode(content).toString()
            return text.removeSurrounding("[", "]")
        }
        return ""
    }
}