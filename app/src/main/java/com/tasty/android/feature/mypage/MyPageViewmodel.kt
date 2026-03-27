package com.tasty.android.feature.mypage

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class MyPageTab {
    FEED,
    TASTY_LIST
}

data class MyFeedItem(
    val id: Long,
    val thumbnailUrl: String = "",
    val title: String = "",
    val restaurantName: String = "",
    val rating: Float = 0f
)

data class MyTastyListItem(
    val id: Long,
    val title: String,
    val thumbnailUrl: String = "",
    val feedCount: Int = 0
)

data class MyProfileInfo(
    val userId: Long = 0L,
    val nickname: String = "",
    val username: String = "",
    val intro: String = "",
    val profileImageUrl: String = ""
)

data class MyPageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,

    val profileInfo: MyProfileInfo = MyProfileInfo(
        userId = 1L,
        nickname = "길동길동11",
        username = "@woals8888",
        intro = "맛있는 걸 좋아합니다."
    ),

    val feedCount: Int = 10,
    val followerCount: Int = 10,
    val followingCount: Int = 10,

    val selectedTab: MyPageTab = MyPageTab.FEED,

    val myFeeds: List<MyFeedItem> = emptyList(),
    val myTastyLists: List<MyTastyListItem> = emptyList(),

    val selectedFeedIdsForTastyList: Set<Long> = emptySet(),
    val tastyListTitleInput: String = "",
    val tastyListThumbnailUri: String? = null
) {
    val isFeedTabSelected: Boolean
        get() = selectedTab == MyPageTab.FEED

    val isTastyListTabSelected: Boolean
        get() = selectedTab == MyPageTab.TASTY_LIST

    val shouldShowTastyListFab: Boolean
        get() = selectedTab == MyPageTab.TASTY_LIST

    val isFeedEmpty: Boolean
        get() = myFeeds.isEmpty()

    val isTastyListEmpty: Boolean
        get() = myTastyLists.isEmpty()
}

class MyPageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createMockState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    fun selectTab(tab: MyPageTab) {
        _uiState.update { currentState ->
            currentState.copy(selectedTab = tab)
        }
    }

    fun selectFeedTab() {
        _uiState.update { currentState ->
            currentState.copy(selectedTab = MyPageTab.FEED)
        }
    }

    fun selectTastyListTab() {
        _uiState.update { currentState ->
            currentState.copy(selectedTab = MyPageTab.TASTY_LIST)
        }
    }

    fun updateProfile(
        nickname: String,
        username: String,
        intro: String
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                profileInfo = currentState.profileInfo.copy(
                    nickname = nickname,
                    username = username,
                    intro = intro
                )
            )
        }
    }

    fun setMyFeeds(feeds: List<MyFeedItem>) {
        _uiState.update { currentState ->
            currentState.copy(
                myFeeds = feeds,
                feedCount = feeds.size
            )
        }
    }

    fun setMyTastyLists(tastyLists: List<MyTastyListItem>) {
        _uiState.update { currentState ->
            currentState.copy(
                myTastyLists = tastyLists
            )
        }
    }

    fun updateFollowCounts(
        followerCount: Int,
        followingCount: Int
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                followerCount = followerCount,
                followingCount = followingCount
            )
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = message)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isLoading = isLoading)
        }
    }

    fun setRefreshing(isRefreshing: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isRefreshing = isRefreshing)
        }
    }


    fun toggleFeedSelection(feedId: Long) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedFeedIdsForTastyList.toMutableSet()
            if (currentSelected.contains(feedId)) {
                currentSelected.remove(feedId)
            } else {
                currentSelected.add(feedId)
            }

            currentState.copy(
                selectedFeedIdsForTastyList = currentSelected
            )
        }
    }

    fun clearSelectedFeedsForTastyList() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFeedIdsForTastyList = emptySet()
            )
        }
    }

    fun updateTastyListTitleInput(title: String) {
        _uiState.update { currentState ->
            currentState.copy(
                tastyListTitleInput = title
            )
        }
    }

    fun updateTastyListThumbnailUri(uri: String?) {
        _uiState.update { currentState ->
            currentState.copy(
                tastyListThumbnailUri = uri
            )
        }
    }

    fun resetTastyListCreationState() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFeedIdsForTastyList = emptySet(),
                tastyListTitleInput = "",
                tastyListThumbnailUri = null
            )
        }
    }

    fun addTastyList(
        title: String,
        thumbnailUrl: String = ""
    ) {
        _uiState.update { currentState ->
            val nextId = (currentState.myTastyLists.maxOfOrNull { it.id } ?: 0L) + 1L
            val newItem = MyTastyListItem(
                id = nextId,
                title = title,
                thumbnailUrl = thumbnailUrl,
                feedCount = currentState.selectedFeedIdsForTastyList.size
            )

            currentState.copy(
                myTastyLists = listOf(newItem) + currentState.myTastyLists,
                selectedFeedIdsForTastyList = emptySet(),
                tastyListTitleInput = "",
                tastyListThumbnailUri = null,
                selectedTab = MyPageTab.TASTY_LIST
            )
        }
    }

    companion object {
        private fun createMockState(): MyPageUiState {
            return MyPageUiState(
                isLoading = false,
                profileInfo = MyProfileInfo(
                    userId = 1L,
                    nickname = "길동길동11",
                    username = "@woals8888",
                    intro = "맛있는 걸 좋아합니다."
                ),
                feedCount = 10,
                followerCount = 10,
                followingCount = 10,
                selectedTab = MyPageTab.FEED,
                myFeeds = emptyList(),
                myTastyLists = emptyList()
            )
        }
    }
}