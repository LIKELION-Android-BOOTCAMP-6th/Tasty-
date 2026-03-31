package com.tasty.android.feature.feed

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.util.toFormattedDate
import com.tasty.android.feature.feed.model.Comment
import com.tasty.android.feature.feed.model.FeedComment
import com.tasty.android.feature.feed.model.FeedLike
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FeedDetailPostUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val placeName: String,
    val address: String,
    val rating: Int,
    val shortReview: String,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val likeCount: Int,
    val commentCount: Int,
    val dateText: String,
    val isLiked: Boolean = false
)

data class FeedDetailUiState(
    val isLoading: Boolean = false,
    val isCommentSubmitting: Boolean = false,
    val post: FeedDetailPostUiModel? = null,
    val comments: List<FeedComment> = emptyList(),
    val commentInput: String = "",
    val errorMessage: String? = null,
    val isLoadingMoreComments: Boolean = false,
    val hasMoreComments: Boolean = true
) {
    val canSubmitComment: Boolean
        get() = commentInput.trim().isNotBlank() && !isCommentSubmitting
}

class FeedDetailViewModel(
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {

    private val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(FeedDetailUiState())
    val uiState: StateFlow<FeedDetailUiState> = _uiState.asStateFlow()

    private var lastCommentId: String? = null

    // 피드 상세 로딩
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadFeedDetail(feedId: String) {
        viewModelScope.launch {
            lastCommentId = null
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasMoreComments = true) }


            val feedResultDeferred = async { feedStoreManager.getFeedDetail(feedId) }

            val commentsResultDeferred = async { feedStoreManager.getComments(feedId) }

            val isLikedDeferred = async {
                feedStoreManager.isLiked(FeedLike(feedId = feedId, userId = currentUserId)).getOrDefault(false)
            }

            val feedResult = feedResultDeferred.await()
            val commentsResult = commentsResultDeferred.await()
            val isLiked = isLikedDeferred.await()

            if (feedResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "피드를 불러오지 못했습니다.") }
                return@launch
            }

            val feed = feedResult.getOrThrow()

            // Domain -> UI Model 맵핑
            val postUiModel = FeedDetailPostUiModel(
                id = feed?.feedId ?: "",
                authorId = feed?.authorId ?: "",
                authorName = "작성자", //
                placeName = feed?.restaurantName ?: "",
                address = feed?.addressInfo?.roadAddress ?: "" ,
                rating = feed?.rating ?: 0,
                shortReview = feed?.shortReview ?: "",
                content = feed?.content ?: "",
                imageUrls = feed?.feedImageUrls ?: emptyList() ,
                likeCount = feed?.likeCount ?: 0,
                commentCount = feed?.commentCount ?: 0,
                dateText = feed?.createdAt?.toFormattedDate() ?: LocalDate.now().toString(),
                isLiked = isLiked
            )


            val commentUiModels = commentsResult.getOrNull()?.map { comment ->
                FeedComment(
                    commentId = comment.commentId,
                    feedId = comment.feedId,
                    authorId = comment.authorId,
                    content = comment.content,
                    createdAt = comment.createdAt
                )
            } ?: emptyList()
            lastCommentId = commentUiModels.lastOrNull()?.commentId

            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    post = postUiModel,
                    comments = commentUiModels,
                    hasMoreComments = commentUiModels.size >= 20
                )
            }
        }
    }

    // 댓글 추가 로딩 (페이지네이션)
    fun loadMoreComments(feedId: String) {
        if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreComments = true) }
            val result = feedStoreManager.getComments(feedId = feedId, lastCommentId = lastCommentId)

            val newComments = result.getOrNull()?.map { comment ->
                FeedComment(
                    commentId = comment.commentId,
                    feedId = comment.feedId,
                    authorId = comment.authorId,
                    content = comment.content,
                    createdAt = comment.createdAt
                )
            } ?: emptyList()

            if (newComments.isNotEmpty()) {
                lastCommentId = newComments.last().commentId
            }

            _uiState.update { currentState ->
                currentState.copy(
                    isLoadingMoreComments = false,
                    hasMoreComments = newComments.size >= 20,
                    comments = currentState.comments + newComments
                )
            }
        }
    }

    fun updateCommentInput(input: String) {
        _uiState.update { it.copy(commentInput = input) }
    }

    // 댓글 추가
    @RequiresApi(Build.VERSION_CODES.O)
    fun submitComment(feedId: String) {
        val input = _uiState.value.commentInput.trim()
        if (input.isBlank()) return

        val safeUserId = if (currentUserId.isBlank()) "anonymous_user" else currentUserId

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSubmitting = true) }

            val newComment = Comment(
                feedId = feedId,
                authorId = safeUserId,
                content = input,
                createdAt = com.google.firebase.Timestamp.now() // orderBy 누락 방지
            )

            feedStoreManager.addComment(newComment)
                .onSuccess {
                    _uiState.update { currentState ->
                        val updatedComments = listOf(
                            FeedComment(
                                feedId = feedId,
                                authorId = safeUserId,
                                content = input,
                                createdAt = newComment.createdAt
                            )
                        ) + currentState.comments

                        val updatedPost = currentState.post?.copy(
                            commentCount = currentState.post.commentCount + 1
                        )

                        currentState.copy(
                            commentInput = "",
                            isCommentSubmitting = false,
                            comments = updatedComments,
                            post = updatedPost
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isCommentSubmitting = false, errorMessage = "댓글 작성에 실패했습니다.") }
                }
        }
    }
    // 좋아요 토글
    fun toggleLike(feedId: String) {
        val safeUserId = if (currentUserId.isBlank()) "anonymous_user" else currentUserId

        val currentPost = _uiState.value.post ?: return
        val isCurrentlyLiked = currentPost.isLiked

        _uiState.update { state ->
            state.copy(
                post = currentPost.copy(
                    isLiked = !isCurrentlyLiked,
                    likeCount = if (isCurrentlyLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1
                )
            )
        }

        val feedLike = FeedLike(feedId = feedId, userId = safeUserId) // currentUserId 대신 safeUserId 사용

        viewModelScope.launch {
            val result = if (isCurrentlyLiked) {
                feedStoreManager.unlikeFeed(feedLike)
            } else {
                feedStoreManager.likeFeed(feedLike)
            }

            result.onFailure {
                _uiState.update { state ->
                    state.copy(
                        post = currentPost.copy(
                            isLiked = isCurrentlyLiked,
                            likeCount = currentPost.likeCount
                        ),
                        errorMessage = "좋아요 처리에 실패했습니다."
                    )
                }
            }
        }
    }
}