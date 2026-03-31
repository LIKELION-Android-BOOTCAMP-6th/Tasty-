package com.tasty.android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.MyPageStoreManager
import com.tasty.android.core.firebase.UserStoreManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MyPageTab {
    FEED,
    TASTY_LIST
}

data class MyFeedItem(
    val feedId: String,
    val thumbnailUrl: String? = "",
)

data class MyTastyListItem(
    val tastyListId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val feedCount: Int = 0
)

data class MyProfileInfo(
    val userId: String = "",
    val nickname: String = "",
    val userHandle: String = "",
    val bio: String = "",
    val profileImageUrl: String = ""
)

data class MyPageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isLoadingMoreFeeds: Boolean = false,
    val hasMoreFeeds: Boolean = true,

    val profileInfo: MyProfileInfo? = null,

    val feedCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,

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

class MyPageViewModel(
    private val feedStoreManager: FeedStoreManager,
    private val myPageStoreManager: MyPageStoreManager,
    private val userStoreManager: UserStoreManager
) : ViewModel() {
    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private var lastMyFeedId: String? = null

    init {
        loadMyPageData()
    }
    fun loadMyPageData() {
        val safeUserId = currentUserId.ifBlank { "anonymous_user" }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasMoreFeeds = true) }

            val userResultDeferred = async {
               userStoreManager.getUser(safeUserId)
            }
            val feedsResultDeferred = async {
                myPageStoreManager.getMyFeeds(safeUserId)
            }

            val userResult = userResultDeferred.await()
            val feedsResult = feedsResultDeferred.await()

            val user = userResult.getOrNull()
            val feeds = feedsResult.getOrNull() ?: emptyList()

            if (user != null) {
                val profileInfo = MyProfileInfo(
                    userId= user.userId,
                    nickname = user.nickname,
                    userHandle = user.userHandle,
                    bio = user.bio,
                    profileImageUrl = user.profileImageUrl
                )

                val myFeedItems = feeds.map {feed ->
                    MyFeedItem(
                        feedId = feed.feedId,
                        thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                    )

                }
                lastMyFeedId = feeds.lastOrNull()?.feedId

                _uiState.update {currentState ->
                    currentState.copy(
                        isLoading = false,
                        profileInfo = profileInfo,
                        feedCount = user.feedCount,
                        followingCount = user.followingCount,
                        followerCount = user.followerCount,
                        myFeeds = myFeedItems,
                        hasMoreFeeds = feeds.size >= 20
                    )

                }
            } else {
                _uiState.update {currentState ->
                    currentState.copy(
                        isLoading = false, errorMessage = "유저 정보를 불러오지 못했습니다."
                    )
                }
            }
        }
    }

    fun loadMoreMyFeed() {
        if (_uiState.value.isLoadingMoreFeeds || !_uiState.value.hasMoreFeeds) return

        val safeUserId: String = currentUserId.ifBlank { "anonymous_user" }

        viewModelScope.launch {
            _uiState.update {currentState ->
                currentState.copy(
                    isLoadingMoreFeeds = true
                )
            }
            val result = myPageStoreManager.getMyFeeds(safeUserId, lastFeedId = lastMyFeedId)
            val feeds = result.getOrNull() ?: emptyList()

            if (feeds.isNotEmpty()) {
                val newFeedItems = feeds.map {feed ->
                    MyFeedItem(
                        feedId = feed.feedId,
                        thumbnailUrl = feed.feedImageUrls.firstOrNull()
                    )
                }
                lastMyFeedId = feeds.last().feedId

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingMoreFeeds = false,
                        myFeeds = currentState.myFeeds + newFeedItems,
                        hasMoreFeeds = feeds.size >= 20
                    )

                }
            } else {
                _uiState.update { it.copy(isLoadingMoreFeeds = false, hasMoreFeeds = false) }
            }
        }
    }
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
        profileImageUrl: String,
        bio: String
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                profileInfo = currentState.profileInfo?.copy(
                    nickname = nickname,
                    profileImageUrl = profileImageUrl,
                    bio = bio
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

    /*fun addTastyList(
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
    }*/

}