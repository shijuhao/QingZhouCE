package com.example.toolbox.function.daily

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RandomItem(
    val id: Int,
    val name: String,
    val weight: Int,
    var isSelected: Boolean = false
)

class RandomChooseViewModel : ViewModel() {
    var items by mutableStateOf<List<RandomItem>>(emptyList())
        private set

    var newItemName by mutableStateOf("")
        private set

    var newItemWeight by mutableStateOf("1")
        private set

    var selectedResult by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf("")
        private set

    var isSelecting by mutableStateOf(false)
        private set

    var showResult by mutableStateOf(false)
        private set

    // 用于闪烁动画的高亮索引
    var highlightedIndex by mutableIntStateOf(-1)
        private set

    private var nextId = 1

    val totalWeight: Int
        get() = items.sumOf { it.weight }

    fun updateNewItemName(name: String) {
        newItemName = name
    }

    fun updateNewItemWeight(weight: String) {
        newItemWeight = weight
    }

    fun addItem() {
        errorMessage = ""
        try {
            val name = newItemName.trim()
            val weightText = newItemWeight.trim()

            if (name.isEmpty()) {
                errorMessage = "请输入项目名称"
                return
            }
            if (weightText.isEmpty()) {
                errorMessage = "请输入权重值"
                return
            }
            val weight = weightText.toIntOrNull()
            if (weight == null || weight <= 0) {
                errorMessage = "权重值必须是大于0的整数"
                return
            }

            items = items + RandomItem(nextId++, name, weight)
            newItemName = ""
            newItemWeight = "1"
            errorMessage = ""
            selectedResult = ""
            showResult = false
        } catch (e: Exception) {
            errorMessage = "添加失败: ${e.message}"
        }
    }

    fun removeItem(id: Int) {
        items = items.filter { it.id != id }
        selectedResult = ""
        showResult = false
    }

    fun clearAllItems() {
        items = emptyList()
        selectedResult = ""
        showResult = false
        isSelecting = false
    }

    fun performSelection(onComplete: () -> Unit = {}) {
        if (items.isEmpty() || isSelecting) return

        viewModelScope.launch {
            isSelecting = true
            showResult = false
            highlightedIndex = -1

            // 清除之前的选中状态
            items = items.map { it.copy(isSelected = false) }

            // 随机闪烁动画
            repeat(10) {
                if (items.isNotEmpty()) {
                    val randomIndex = Random.nextInt(items.size)
                    highlightedIndex = randomIndex
                }
                delay(150) // 150ms 每次闪烁
            }

            // 最终选择
            val totalWeight = items.sumOf { it.weight }
            if (totalWeight > 0 && items.isNotEmpty()) {
                val randomValue = Random.nextDouble(0.0, totalWeight.toDouble())
                var cumulativeWeight = 0.0
                var selectedItem: RandomItem? = null

                for (item in items) {
                    cumulativeWeight += item.weight
                    if (randomValue < cumulativeWeight) {
                        selectedItem = item
                        break
                    }
                }
                selectedItem = selectedItem ?: items.last()

                // 更新选中状态
                items = items.map { item ->
                    if (item.id == selectedItem.id) {
                        item.copy(isSelected = true)
                    } else {
                        item.copy(isSelected = false)
                    }
                }
                highlightedIndex = items.indexOfFirst { it.id == selectedItem.id }
                selectedResult = selectedItem.name
                showResult = true
            }

            isSelecting = false
            onComplete()
        }
    }
}