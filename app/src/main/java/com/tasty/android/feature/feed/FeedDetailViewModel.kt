package com.tasty.android.feature.feed

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.feature.feed.model.FeedComment
import com.tasty.android.feature.feed.model.FeedLike
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tasty.android.core.firebase.FeedUpdateEvent
import com.tasty.android.core.firebase.UserStoreManager
import com.tasty.android.feature.feed.mapper.toFeedDetailPostUiModel
import kotlin.collections.emptyList


data class FeedDetailPostUiModel(
    val id: String,
    val authorId: String,
    val authorNickname: String, // authorName에서 변경
    val userHandle: String,     // 추가
    val authorProfileUrl: String? = null, // 추가
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
    private val feedStoreManager: FeedStoreManager,
    private val userStoreManager: UserStoreManager
) : ViewModel() {

    private val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(FeedDetailUiState())
    val uiState: StateFlow<FeedDetailUiState> = _uiState.asStateFlow()

    private var lastCommentId: String? = null

    init {
        // 실시간 피드 업데이트 이벤트 구독 (좋아요 상태 등)
        viewModelScope.launch {
            feedStoreManager.feedUpdateEvents.collect { event ->
                when (event) {
                    is FeedUpdateEvent.LikeStatusChanged -> {
                        _uiState.update { state ->
                            if (state.post?.id == event.feedId) {
                                state.copy(
                                    post = state.post.copy(
                                        isLiked = event.isLiked,
                                        likeCount = event.likeCount
                                    )
                                )
                            } else state
                        }
                    }
                    is FeedUpdateEvent.CommentCountChanged -> {
                        _uiState.update { state ->
                            if (state.post?.id == event.feedId) {
                                state.copy(post = state.post.copy(commentCount = event.newCount))
                            } else state
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refresh(feedId: String) {
        loadFeedDetail(feedId)
    }

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

            // 작성자 정보 확인 (비어있을 경우 조회)
            val authorNickname = feed?.authorNickname?.ifBlank { "작성자" } ?: "작성자"
            val userHandle = feed?.authorHandle?.ifBlank { "abcd" } ?: "abcd"

            // Mapper 사용
            val postUiModel = feed?.toFeedDetailPostUiModel(
                authorNickname = authorNickname,
                userHandle = userHandle,
                authorProfileUrl = feed.authorProfileUrl, // 추가
                isLiked = isLiked
            )


            val commentUiModels = commentsResult.getOrNull()?.map { comment ->
                FeedComment(
                    commentId = comment.commentId,
                    feedId = comment.feedId,
                    authorId = comment.authorId,
                    authorNickname = comment.authorNickname,
                    authorHandle = comment.authorHandle,
                    authorProfileUrl = comment.authorProfileUrl,
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
                    authorNickname = comment.authorNickname.ifBlank { "익명" },
                    authorHandle = comment.authorHandle.ifBlank { "anonymous" },
                    authorProfileUrl = comment.authorProfileUrl, // 추가
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
            // 현재 로그인 유저 정보 가져오기 (역정규화용)
            val currentUserResult = userStoreManager.getUser(safeUserId)
            val userProfile = currentUserResult.getOrNull()

            val newComment = FeedComment(
                feedId = feedId,
                authorId = safeUserId,
                authorNickname = userProfile?.nickname ?: "익명",
                authorHandle = userProfile?.userHandle ?: "anonymous",
                authorProfileUrl = userProfile?.profileImageUrl, // 추가 (역정규화)
                content = input,
                createdAt = com.google.firebase.Timestamp.now()
            )

            feedStoreManager.addComment(newComment)
                .onSuccess {
                    _uiState.update { currentState ->
                        val updatedComments = listOf(
                            FeedComment(
                                commentId = newComment.commentId,
                                feedId = feedId,
                                authorId = safeUserId,
                                authorNickname = newComment.authorNickname,
                                authorHandle = newComment.authorHandle,
                                authorProfileUrl = newComment.authorProfileUrl, // 추가
                                content = input,
                                createdAt = newComment.createdAt
                            )
                        ) + currentState.comments

                        val newCount = (currentState.post?.commentCount ?: 0) + 1
                        val updatedPost = currentState.post?.copy(commentCount = newCount)

                        // 다른 화면에도 알림
                        viewModelScope.launch {
                            feedStoreManager.notifyFeedUpdated(
                                FeedUpdateEvent.CommentCountChanged(feedId, newCount)
                            )
                        }

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

            result.onSuccess {
                // 실시간 이벤트 전파
                val updatedCount = if (isCurrentlyLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1
                feedStoreManager.notifyFeedUpdated(
                    FeedUpdateEvent.LikeStatusChanged(feedId, !isCurrentlyLiked, updatedCount)
                )
            }
            .onFailure {
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