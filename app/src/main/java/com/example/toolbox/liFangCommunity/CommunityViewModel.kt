package com.example.toolbox.liFangCommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolbox.data.liFangCommunity.Comment
import com.example.toolbox.data.liFangCommunity.CommunityUiState
import com.example.toolbox.data.liFangCommunity.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup

class CommunityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/")
                    .get()
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                if (response.isSuccessful) {
                    val posts = parsePostsFromHtml(html)
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载失败: ${response.code}"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网络错误: ${e.message}"
                )
            }
        }
    }

    private fun parsePostsFromHtml(html: String): List<Post> {
        val doc = Jsoup.parse(html)
        val postElements = doc.select(".post-card")
        val postList = mutableListOf<Post>()

        for (element in postElements) {
            try {
                val title = element.select(".post-title").text()
                val author = element.select(".post-meta strong").text()
                val time = element.select(".post-time").text()

                // 提取帖子ID：从删除链接或评论表单中获取
                val deleteLink = element.select("a[href*='/delete/']").attr("href")
                val postIdFromDelete = if (deleteLink.isNotEmpty()) deleteLink.substringAfterLast("/") else null

                val commentForm = element.select(".comment-form input[name=post_id]").firstOrNull()
                val postIdFromCommentForm = commentForm?.attr("value")

                val id = postIdFromDelete ?: postIdFromCommentForm ?: title.hashCode().toString() // 确保有ID

                val contentHtml = element.select(".post-content").html()

                // --- 新增：解析评论 ---
                val commentsList = mutableListOf<Comment>()
                val commentElements = element.select(".comment-item")
                for (commentEl in commentElements) {
                    val commentAuthor = commentEl.select(".comment-author").text()
                    val commentTime = commentEl.select(".comment-time").text()
                    val commentContentHtml = commentEl.select(".comment-content").html()
                    commentsList.add(Comment(commentAuthor, commentTime, commentContentHtml))
                }

                postList.add(Post(id, title, author, time, contentHtml, commentsList))
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
        }
        return postList
    }
    
    fun postComment(postId: String, commentContent: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("post_id", postId)
                    .add("content", commentContent)
                    .build()

                val request = Request.Builder()
                    .url("${CubeNetworkManager.BASE_URL}/comment")
                    .post(formBody)
                    .build()

                val response = CubeNetworkManager.client.newCall(request).execute()
                val html = response.body.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val doc = Jsoup.parse(html)
                        val errorAlert = doc.select(".alert-danger").firstOrNull()

                        if (errorAlert == null) {
                            onSuccess()
                            fetchPosts()
                        } else {
                            onError(errorAlert.text())
                        }
                    } else {
                        onError("评论失败: 服务器响应错误 ${response.code}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("评论失败: 网络错误 ${e.message}")
                }
            }
        }
    }
}