@file:Suppress("PropertyName")

package com.example.toolbox.resourceLib

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.ApiAddress
import com.example.toolbox.AppJson
import com.example.toolbox.data.community.ResourceItem
import com.example.toolbox.data.community.ResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class ResourceViewModel : ViewModel() {
    var resourceList by mutableStateOf<List<ResourceItem>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private val client = OkHttpClient()

    fun fetchResources(categoryId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            try {
                val request = Request.Builder()
                    .url("${ApiAddress}get_resources?category_id=$categoryId")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body.string()
                        val data = AppJson.json.decodeFromString<ResourceResponse>(json)
                        resourceList = data.resources
                    } else {
                        errorMessage = "加载失败: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "网络错误: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}