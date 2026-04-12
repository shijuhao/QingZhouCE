package com.example.toolbox.function.visual.painter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.toolbox.ui.theme.ToolBoxTheme

class PainterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolBoxTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    PainterScreen()
                }
            }
        }
    }
}