package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TastyListUiModel(
    val id: Int,
    val title: String,
    val subtitle: String
)

data class FeedPostUiModel(
    val id: Int,
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
    val tastyLists: List<TastyListUiModel> = emptyList(),
    val feedPosts: List<FeedPostUiModel> = emptyList()
)

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        FeedUiState(
            userRegion = "홍길동동",
            tastyLists = listOf(
                TastyListUiModel(1, "홍길동동", "강원 맛집 모음"),
                TastyListUiModel(2, "홍길동동", "강원 맛집 모음"),
                TastyListUiModel(3, "홍길동동", "강원 맛집 모음"),
                TastyListUiModel(4, "홍길동동", "강원 맛집 모음")
            ),
            feedPosts = listOf(
                FeedPostUiModel(
                    id = 1,
                    authorName = "홍길동동",
                    authorRegion = "홍길동동",
                    placeName = "길동이네 식당",
                    address = "송파구 XX동 XX로 1...",
                    date = "2026-08-06",
                    likeCount = 500,
                    commentCount = 500,
                    rating = 5f,
                    description = "여기 진짜 맛있는 집 같아요. 분위기부터 근데 점자가 너무 많고 글씨 완전 작음 약간 광고충 불러놓음...",
                    imageUrl = null
                )
            )
        )
    )
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
}