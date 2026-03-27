package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FeedSortType {
    LATEST,
    DISTANCE
}

data class FeedFilterUiState(
    val sortType: FeedSortType = FeedSortType.LATEST,
    val mainRegion: String = "",
    val subRegion: String = ""
) {
    val selectedRegionText: String
        get() = when {
            mainRegion.isBlank() && subRegion.isBlank() -> ""
            mainRegion.isNotBlank() && subRegion.isBlank() -> mainRegion
            else -> "$mainRegion $subRegion"
        }
}

data class FeedUiState(
    val userRegion: String = "성수동",
    val tastyLists: List<TastyListUiModel> = emptyList(),
    val feedPosts: List<FeedPostUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val filter: FeedFilterUiState = FeedFilterUiState()
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

    private val originalFeedPosts = listOf(
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
            address = "서울 성동구 성수동 77-10",
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
            placeName = "강남 스테이크",
            address = "서울 강남구 테헤란로 10",
            rating = 3,
            description = "고기 맛은 좋았고 전체적으로 무난했어요.",
            likeCount = 7,
            commentCount = 1,
            dateText = "03.23"
        ),
        FeedPostUiModel(
            id = "feed_4",
            authorId = "user_4",
            authorName = "mukstar",
            placeName = "서초 태국요리",
            address = "서울 서초구 서초대로 20",
            rating = 5,
            description = "향신료가 강하지 않아서 먹기 편했고 전체적으로 만족스러웠어요.",
            likeCount = 18,
            commentCount = 4,
            dateText = "03.22"
        ),
        FeedPostUiModel(
            id = "feed_5",
            authorId = "user_5",
            authorName = "ricecat",
            placeName = "송파 덮밥집",
            address = "서울 송파구 올림픽로 33",
            rating = 4,
            description = "가성비가 좋고 점심으로 먹기 좋은 메뉴가 많았어요.",
            likeCount = 10,
            commentCount = 2,
            dateText = "03.21"
        )
    )

    private val _uiState = MutableStateFlow(
        FeedUiState(
            userRegion = "성수동",
            tastyLists = listOf(
                TastyListUiModel(
                    id = "tasty_1",
                    title = "성수 브런치",
                    subTitle = "브런치"
                ),
                TastyListUiModel(
                    id = "tasty_2",
                    title = "강남 파스타",
                    subTitle = "양식"
                ),
                TastyListUiModel(
                    id = "tasty_3",
                    title = "홍대 술집",
                    subTitle = "주점"
                ),
                TastyListUiModel(
                    id = "tasty_4",
                    title = "잠실 데이트",
                    subTitle = "데이트"
                )
            ),
            feedPosts = originalFeedPosts,
            filter = FeedFilterUiState()
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

    fun applyFilter(newFilter: FeedFilterUiState) {
        var filteredPosts = originalFeedPosts

        if (newFilter.mainRegion.isNotBlank()) {
            filteredPosts = filteredPosts.filter { post ->
                post.address.contains(newFilter.mainRegion)
            }
        }

        if (newFilter.subRegion.isNotBlank()) {
            filteredPosts = filteredPosts.filter { post ->
                post.address.contains(newFilter.subRegion)
            }
        }

        filteredPosts = when (newFilter.sortType) {
            FeedSortType.LATEST -> filteredPosts.sortedByDescending { it.dateText }
            FeedSortType.DISTANCE -> {
                // TODO: 실제 거리순 정렬 로직 연결
                filteredPosts
            }
        }

        _uiState.update { currentState ->
            currentState.copy(
                filter = newFilter,
                feedPosts = filteredPosts
            )
        }
    }
}