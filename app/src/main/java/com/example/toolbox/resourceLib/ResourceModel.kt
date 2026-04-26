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
import java.net.URLEncoder

class ResourceViewModel : ViewModel() {
    var resourceList by mutableStateOf<List<ResourceItem>>(emptyList())
    var searchResultList by mutableStateOf<List<ResourceItem>>(emptyList())
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
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun searchResources(keyword: String, categoryId: Int? = null, page: Int = 1, perPage: Int = 20) {
        if (keyword.isBlank()) {
            errorMessage = "请输入搜索关键词"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            errorMessage = null
            try {
                val encodedKeyword = URLEncoder.encode(keyword.trim(), "UTF-8")
                val urlBuilder = StringBuilder("${ApiAddress}search_resources?keyword=$encodedKeyword&page=$page&per_page=$perPage")
                
                if (categoryId != null) {
                    urlBuilder.append("&category_id=$categoryId")
                }

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body.string()
                        val responseData = AppJson.json.decodeFromString<SearchResourceResponse>(json)
                        
                        if (responseData.success) {
                            searchResultList = responseData.resources
                        } else {
                            errorMessage = responseData.message ?: "搜索失败"
                        }
                    } else {
                        errorMessage = "搜索失败: ${response.code}"
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

@kotlinx.serialization.Serializable
data class SearchResourceResponse(
    val success: Boolean,
    val message: String? = null,
    val keyword: String? = null,
    val category_id: String? = null,
    val resources: List<ResourceItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 0,
    val per_page: Int = 0,
    val pages: Int = 0,
    val has_next: Boolean = false,
    val has_prev: Boolean = false
)