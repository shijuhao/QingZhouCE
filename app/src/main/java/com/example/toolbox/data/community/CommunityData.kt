package com.example.toolbox.data.community

data class CommunityState(
    val categories: List<Category> = emptyList(),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val categoryId: Int = 1,
    val wsConnectState: Int = 1 //1断开 2连接
)