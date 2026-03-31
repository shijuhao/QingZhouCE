package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.MailboxUiState
import com.example.toolbox.data.liFangCommunity.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class MailboxViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MailboxUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkLoginStatusAndFetchNotifications()
    }

    private fun checkLoginStatusAndFetchNotifications() {
        val isLoggedIn = AuthManager.getIsLoggedIn()
        _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)

        if (!isLoggedIn) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "请先登录才能查看提醒。"
            )
            return
        }
        fetchNotifications()
    }

    fun fetchNotifications() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/notifications") // --- 核心修改：修正 URL ---
                    .get()
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val notifications = parseNotificationsFromHtml(html)
                        _uiState.value = _uiState.value.copy(
                            notifications = notifications,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "加载提醒失败: ${response.code}。请确认已登录或稍后重试。"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "网络连接失败: ${e.message}。请检查网络或稍后重试。"
                    )
                }
            }
        }
    }

    private fun parseNotificationsFromHtml(html: String): List<Notification> {
        val doc = Jsoup.parse(html)
        val notificationElements = doc.select(".notification-card")
        val notificationList = mutableListOf<Notification>()

        if (notificationElements.isEmpty()) {
            val loginPrompt = doc.select("a[href*='/login']").firstOrNull()
            if (loginPrompt != null) {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = false,
                    error = "会话已失效，请重新登录。"
                )
                AuthManager.clearLoginState() // 清除本地登录状态
                return emptyList()
            }
            // 否则可能是没有消息
            return emptyList()
        }

        for (element in notificationElements) {
            try {
                val isRead = element.hasClass("read") // 根据 class 判断是否已读
                val senderInfo = element.select("div[style*='font-weight: bold']").text() // 提取发送者信息
                val contentDiv = element.select("div[style*='margin: 8px 0']").firstOrNull()
                val contentHtml = contentDiv?.html() ?: "" // 提醒内容 HTML
                val time = element.select(".notification-time").text() // 提醒时间

                val postLinkElement = element.select("a.text-btn[href*='#post-']").firstOrNull()
                val postLink = postLinkElement?.attr("href") ?: ""
                val id = postLink.substringAfter("#post-").ifEmpty { element.hashCode().toString() } // 从链接中提取帖子ID作为提醒ID

                notificationList.add(Notification(id, senderInfo, contentHtml, time, isRead, postLink))
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }
        return notificationList
    }

    fun checkLoginState() {
        val isLoggedIn = AuthManager.getIsLoggedIn()
        if (_uiState.value.isLoggedIn != isLoggedIn) {
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
            if (isLoggedIn) {
                fetchNotifications() // 如果登录状态改变为已登录，重新获取提醒
            } else {
                _uiState.value = _uiState.value.copy(
                    notifications = emptyList(),
                    error = "请先登录才能查看提醒。"
                )
            }
        }
    }
}