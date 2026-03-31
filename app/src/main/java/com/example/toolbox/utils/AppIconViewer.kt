package com.example.toolbox.utils

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppIconViewer() {
    Surface(
        modifier = Modifier.size(80.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(
            Icons.Rounded.Terrain,
            modifier = Modifier.requiredSize(55.dp),
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun AppIconViewerPreView () {
    AppIconViewer()
}