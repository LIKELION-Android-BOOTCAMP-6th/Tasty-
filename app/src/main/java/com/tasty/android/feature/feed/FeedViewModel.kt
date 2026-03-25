package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeedUiState(
    val userRegion: String = "",
    val tastyLists: List<TastyListUiModel> = emptyList(),
    val feedPosts: List<FeedPostUiModel> = emptyList(),
    val isLoading: Boolean = false
)

data class TastyListUiModel(
    val id: String,
    val title: String,
    val subTitle: String
)

data class FeedPostUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val placeName: String,
    val address: String,
    val rating: Int,
    val description: String,
    val likeCount: Int,
    val commentCount: Int,
    val dateText: String
)

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        FeedUiState(
            userRegion = "성수동",
            tastyLists = listOf(
                TastyListUiModel("tasty_1", "성수 브런치", "브런치"),
                TastyListUiModel("tasty_2", "강남 파스타", "양식"),
                TastyListUiModel("tasty_3", "홍대 술집", "주점"),
                TastyListUiModel("tasty_4", "잠실 데이트", "데이트")
            ),
            feedPosts = listOf(
                FeedPostUiModel(
                    id = "feed_1",
                    authorId = "user_1",
                    authorName = "tasty_user",
                    placeName = "테이스티 파스타",
                    address = "서울 성동구 성수동 123-45",
                    rating = 4,
                    description = "분위기도 좋고 음식도 깔끔해서 재방문하고 싶은 곳이었어요.",
                    likeCount = 12,
                    commentCount = 3,
                    dateText = "03.25"
                ),
                FeedPostUiModel(
                    id = "feed_2",
                    authorId = "user_2",
                    authorName = "foodie",
                    placeName = "성수 브런치 하우스",
                    address = "서울 성동구 성수이로 00길 10",
                    rating = 5,
                    description = "브런치 메뉴가 다양하고 사진 찍기에도 좋았어요.",
                    likeCount = 25,
                    commentCount = 8,
                    dateText = "03.24"
                ),
                FeedPostUiModel(
                    id = "feed_3",
                    authorId = "user_3",
                    authorName = "yummy",
                    placeName = "서울 스테이크",
                    address = "서울 강남구 테헤란로 10",
                    rating = 3,
                    description = "고기 맛은 좋았고 전체적으로 무난했어요.",
                    likeCount = 7,
                    commentCount = 1,
                    dateText = "03.23"
                )
            )
        )
    )

    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    fun increaseLike(feedId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                feedPosts = currentState.feedPosts.map { post ->
                    if (post.id == feedId) {
                        post.copy(likeCount = post.likeCount + 1)
                    } else {
                        post
                    }
                }
            )
        }
    }
}