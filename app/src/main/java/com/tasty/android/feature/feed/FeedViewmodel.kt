package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import com.tasty.android.core.model.AddressInfo
import com.tasty.android.core.model.Feed
import com.tasty.android.feature.feed.mapper.toFeedPostItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TastyListItem(
    val id: Int,
    val title: String,
    val subtitle: String
)

data class FeedPostItem(
    val id: String,
    val authorName: String,
    val authorRegion: String,
    val placeName: String,
    val address: String,
    val date: String,
    val likeCount: Int,
    val commentCount: Int,
    val rating: Float,
    val description: String,
    val imageUrl: String? = null
)

data class FeedUiState(
    val userRegion: String = "홍길동동",
    val tastyLists: List<TastyListItem> = emptyList(),
    val feedPosts: List<FeedPostItem> = emptyList()
)

class FeedViewModel : ViewModel() {

    private val sampleFeeds = listOf(
        Feed(
            feedId = "feed_1",
            authorId = "user_1",
            content = "여기 진짜 맛있는 집 같아요. 분위기도 좋고 음식도 괜찮았어요.",
            createdAt = "2026-08-06",
            imagesUrl = listOf(""),
            likeCount = 500,
            commentCount = 500,
            rating = 5,
            shortReview = "맛있어요",
            businessHours = "09:00 ~ 21:00",
            businessStatus = "영업중",
            restaurantId = "restaurant_1",
            restaurantName = "길동이네 식당",
            restaurantPhoneNumber = "02-0000-0000",
            restaurantThumbnailUrl = "",
            addressInfo = AddressInfo(
                latitude = 37.0,
                longitude = 127.0,
                mainRegion = "서울",
                roadAddress = "송파구 XX동 XX로 1",
                subRegion = "홍길동동"
            )
        )
    )

    private val _uiState = MutableStateFlow(
        FeedUiState(
            userRegion = "홍길동동",
            tastyLists = listOf(
                TastyListItem(1, "홍길동동", "강원 맛집 모음"),
                TastyListItem(2, "홍길동동", "강원 맛집 모음"),
                TastyListItem(3, "홍길동동", "강원 맛집 모음"),
                TastyListItem(4, "홍길동동", "강원 맛집 모음")
            ),
            feedPosts = sampleFeeds.map { feed ->
                feed.toFeedPostItem(authorName = "홍길동")
            }
        )
    )

    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
}