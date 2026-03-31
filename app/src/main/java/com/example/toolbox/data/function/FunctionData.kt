package com.example.toolbox.data.function

import androidx.compose.ui.graphics.vector.ImageVector

data class FunctionItem(
    val name: String,
    val activity: String,
    val icon: ImageVector? = null
)

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class Resource(val resId: Int) : IconSource()
}

data class FunctionCategory(
    val name: String,
    val functions: List<FunctionItem>,
    val icon: IconSource? = null
)

data class SearchFunctionModel(
    val function: FunctionItem,
    val categoryName: String,
    val categoryIcon: IconSource?
)