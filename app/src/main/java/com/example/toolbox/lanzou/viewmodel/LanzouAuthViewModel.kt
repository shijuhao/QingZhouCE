package com.example.toolbox.lanzou.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.toolbox.lanzou.LanzouRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LanzouAuthViewModel(
    private val repository: LanzouRepository = LanzouRepository()
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun refresh(context: Context) {
        _isLoggedIn.value = repository.isLoggedIn(context)
    }

    fun logout(context: Context) {
        repository.logout(context)
        _isLoggedIn.value = false
    }
}
