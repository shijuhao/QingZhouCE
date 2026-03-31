package com.example.toolbox.guide

import androidx.lifecycle.ViewModel
import com.example.toolbox.data.guide.GuideData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GuideViewModel : ViewModel() {
    private val _guide = MutableStateFlow(GuideData())
    val guide: StateFlow<GuideData> = _guide

    fun updatePage(value: Int) {
        _guide.update { it.copy(page = value) }
    }
}