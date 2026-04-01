package com.tasty.android.feature.tasty

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasty.android.core.firebase.AuthManager
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.core.firebase.UserStoreManager
import com.tasty.android.core.model.UserSummary
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.mypage.tastylist.model.TastyListLike
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TastyAuthorUiModel(
    val profileImageUrl: String = "",
    val nickname: String = "",
    val username: String = "",
    val introduction: String = ""
)

data class TastyFeedItemUiModel(
    val feedId: String = "",
    val imageUrl: String = "",
    val restaurantName: String = "",
    val category: String = "",
    val address: String = "",
    val distanceText: String = "",
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val oneLineReview: String = ""
)

data class TastyDetailUiState(
    val isLoading: Boolean = false,
    val tastyId: String = "",
    val title: String = "",
    val author: TastyAuthorUiModel = TastyAuthorUiModel(),
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val isLiked: Boolean = false,
    val feedList: List<TastyFeedItemUiModel> = emptyList(),
    val errorMessage: String? = null
)

class TastyDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tastystoreManager = TastyStoreManager()

    private val userstoreManager = UserStoreManager()

    private val authManager = AuthManager()

    private val tastyId: String = savedStateHandle["tastyId"] ?: ""

    private val _uiState = MutableStateFlow(
        TastyDetailUiState(tastyId = tastyId)
    )
    val uiState: StateFlow<TastyDetailUiState> = _uiState.asStateFlow()

    init {
        loadTastyDetail(shouldIncrement = true)
    }

    fun refresh() {
        loadTastyDetail(shouldIncrement = false)
    }

    fun loadTastyDetail(shouldIncrement: Boolean = false) {
        if (tastyId.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "잘못된 접근입니다.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (shouldIncrement) {
                tastystoreManager.incrementTastyListViewCount(tastyId)
            }

            val tastyResult = tastystoreManager.getTastyList(tastyId)

            tastyResult
                .onSuccess { tastyList ->
                    if (tastyList == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "테이스티 정보를 찾을 수 없습니다."
                            )
                        }
                        return@launch
                    }

                    val currentUserId = authManager.getCurrentUser()?.uid

                    val authorDeferred = async {
                        userstoreManager.getUserSummary(tastyList.authorId)
                    }

                    val feedsDeferred = async {
                        tastystoreManager.getFeedsByIDs(tastyList.feedIds)
                    }

                    val likeDeferred = async {
                        if (currentUserId == null) {
                            Result.success(false)
                        } else {
                            tastystoreManager.isTastyListLiked(
                                TastyListLike(
                                    tastyListId = tastyId,
                                    userId = currentUserId
                                )
                            )
                        }
                    }

                    val authorResult = authorDeferred.await()
                    val feedsResult = feedsDeferred.await()
                    val likeResult = likeDeferred.await()

                    val author = authorResult.getOrNull()
                    val feeds = feedsResult.getOrNull().orEmpty()
                    val isLiked = likeResult.getOrDefault(false)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = tastyList.title,
                            author = author?.toUiModel() ?: TastyAuthorUiModel(),
                            likeCount = tastyList.likeCount,
                            viewCount = tastyList.viewCount,
                            isLiked = isLiked,
                            feedList = feeds.map { feed -> feed.toUiModel() }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "테이스티 상세를 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun toggleLike() {
        val currentUserId = authManager.getCurrentUser()?.uid ?: return
        val currentState = _uiState.value

        viewModelScope.launch {
            val like = createTastyListLike(
                tastyListId = currentState.tastyId,
                userId = currentUserId
            )

            val result = if (currentState.isLiked) {
                tastystoreManager.unlikeTastyList(like)
            } else {
                tastystoreManager.likeTastyList(like)
            }

            result.onSuccess {
                _uiState.update {
                    val nextLiked = !it.isLiked
                    it.copy(
                        isLiked = nextLiked,
                        likeCount = if (nextLiked) it.likeCount + 1 else (it.likeCount - 1).coerceAtLeast(0)
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "좋아요 처리에 실패했습니다.")
                }
            }
        }
    }
}

private fun createTastyListLike(
    tastyListId: String,
    userId: String
): TastyListLike {
    return TastyListLike(
        likeId = "",
        tastyListId = tastyListId,
        userId = userId
    )
}

private fun UserSummary.toUiModel(): TastyAuthorUiModel {
    return TastyAuthorUiModel(
        profileImageUrl = profileImageUrl ?: "",
        nickname = nickname,
        username = userHandle
    )
}

private fun Feed.toUiModel(): TastyFeedItemUiModel {
    return TastyFeedItemUiModel(
        feedId = feedId,
        imageUrl = feedImageUrls.firstOrNull() ?: "",
        restaurantName = restaurantName,
        category = "",
        address = "${addressInfo.mainRegion} ${addressInfo.subRegion}",
        distanceText = "",
        rating = rating.toFloat(),
        reviewCount = commentCount,
        oneLineReview = if (shortReview.isNotBlank()) shortReview else content
    )
}