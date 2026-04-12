package com.example.toolbox.lanzou.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.toolbox.lanzou.LanzouRepository
import com.example.toolbox.lanzou.service.LanzouShareInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LanzouUploadUiState(
    val isUploading: Boolean = false,
    val progress: Int = 0,
    val speedText: String = "0 KB/s"
)

class LanzouUploadViewModel(
    private val repository: LanzouRepository = LanzouRepository()
) : ViewModel() {
    private val _uploadState = MutableStateFlow(LanzouUploadUiState())
    val uploadState: StateFlow<LanzouUploadUiState> = _uploadState.asStateFlow()

    suspend fun uploadApkAndGetShareInfo(context: Context, apkPath: String): LanzouShareInfo? {
        if (_uploadState.value.isUploading) return null
        _uploadState.value = LanzouUploadUiState(isUploading = true, progress = 0, speedText = "0 KB/s")
        return try {
            repository.uploadApkAndGetShareInfo(context, apkPath) { progress ->
                _uploadState.value = LanzouUploadUiState(
                    isUploading = true,
                    progress = progress.progress,
                    speedText = progress.speedText
                )
            }
        } finally {
            _uploadState.value = LanzouUploadUiState()
        }
    }
}
