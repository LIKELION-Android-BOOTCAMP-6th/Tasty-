package com.tasty.android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.FeedUpdateEvent
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.core.firebase.TastyUpdateEvent
import com.tasty.android.core.firebase.AuthManager
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
    val hasImages: Boolean = false
)

data class MyTastyListItem(
    val tastyListId: String = "",
    val title: String = "",
    val thumbnailUrl: String = "",
    val feedCount: Int = 0,
    val viewCount: Int = 0
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
    private val authManager: AuthManager,
    private val feedStoreManager: FeedStoreManager,
    private val myPageStoreManager: MyPageStoreManager,
    private val userStoreManager: UserStoreManager,
    private val tastyStoreManager: TastyStoreManager
) : ViewModel() {
    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private var lastMyFeedId: String? = null

    init {
        loadMyPageData()
        observeDataUpdates()
        observeUserProfile()
    }

    private fun observeUserProfile() {
        val safeUserId = currentUserId.ifBlank { return }
        viewModelScope.launch {
            userStoreManager.observeUser(safeUserId).collect { user ->
                if (user != null) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            profileInfo = MyProfileInfo(
                                userId = user.userId,
                                nickname = user.nickname,
                                userHandle = user.userHandle,
                                bio = user.bio,
                                profileImageUrl = user.profileImageUrl
                            ),
                            feedCount = user.feedCount,
                            followingCount = user.followingCount,
                            followerCount = user.followerCount
                        )
                    }
                }
            }
        }
    }

    private fun observeDataUpdates() {
        // Feed 업데이트 감지
        viewModelScope.launch {
            feedStoreManager.feedUpdateEvents.collect { event ->
                if (event is FeedUpdateEvent.FeedCreated && event.authorId == currentUserId) {
                    loadMyPageData()
                }
            }
        }
        
        // Tasty 리스트 업데이트 감지
        viewModelScope.launch {
            tastyStoreManager.tastyUpdateEvents.collect { event ->
                when (event) {
                    is TastyUpdateEvent.TastyListCreated,
                    is TastyUpdateEvent.TastyListUpdated,
                    is TastyUpdateEvent.TastyListDeleted -> {
                        loadMyPageData()
                    }
                    is TastyUpdateEvent.ViewCountChanged -> {
                        _uiState.update { state ->
                            state.copy(
                                myTastyLists = state.myTastyLists.map { item ->
                                    if (item.tastyListId == event.tastyListId) {
                                        item.copy(viewCount = event.newCount)
                                    } else {
                                        item
                                    }
                                }
                            )
                        }
                    }
                    is TastyUpdateEvent.TastyListLiked -> {}
                    is TastyUpdateEvent.TastyListUnliked -> {}
                }
            }
        }
    }
    fun refresh() {
        loadMyPageData()
    }

    fun loadMyPageData() {
        val safeUserId = currentUserId.ifBlank { "anonymous_user" }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasMoreFeeds = true) }

            val feedsResultDeferred = async {
                myPageStoreManager.getMyFeeds(safeUserId)
            }
            val tastyListsResultDeferred = async {
                myPageStoreManager.getMyTastyLists(safeUserId)
            }

            val feedsResult = feedsResultDeferred.await()
            val tastyListsResult = tastyListsResultDeferred.await()

            if (feedsResult.isFailure) {
                val error = feedsResult.exceptionOrNull()
                _uiState.update { it.copy(errorMessage = "피드를 불러오지 못했습니다: ${error?.message}") }
            }
            if (tastyListsResult.isFailure) {
                val error = tastyListsResult.exceptionOrNull()
                _uiState.update { it.copy(errorMessage = "테이스티를 불러오지 못했습니다: ${error?.message}") }
            }
            val feeds = feedsResult.getOrNull() ?: emptyList()
            val tastyLists = tastyListsResult.getOrNull() ?: emptyList()
            
            android.util.Log.d("MyPageVM", "Loading data for userId: $safeUserId")
            android.util.Log.d("MyPageVM", "Feeds found: ${feeds.size}, TastyLists found: ${tastyLists.size}")

            val myFeedItems = feeds.map {feed ->
                MyFeedItem(
                    feedId = feed.feedId,
                    thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                    hasImages = feed.feedImageUrls.isNotEmpty()
                )

            }
            lastMyFeedId = feeds.lastOrNull()?.feedId

            val myTastyListItems = (tastyListsResult.getOrNull() ?: emptyList()).map { tastyList ->
                MyTastyListItem(
                    tastyListId = tastyList.tastyListId,
                    title = tastyList.title,
                    thumbnailUrl = tastyList.thumbnailImageUrl ?: "",
                    feedCount = tastyList.feedIds.size,
                    viewCount = tastyList.viewCount
                )
            }

            _uiState.update {currentState ->
                currentState.copy(
                    isLoading = false,
                    myFeeds = myFeedItems,
                    myTastyLists = myTastyListItems,
                    hasMoreFeeds = feeds.size >= 10
                )
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
                        thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                        hasImages = feed.feedImageUrls.isNotEmpty()
                    )
                }
                lastMyFeedId = feeds.last().feedId

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingMoreFeeds = false,
                        myFeeds = currentState.myFeeds + newFeedItems,
                        hasMoreFeeds = feeds.size >= 10
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

    fun deleteTastyList(tastyListId: String) {
        viewModelScope.launch {
            tastyStoreManager.deleteTastyList(tastyListId)
        }
    }

    fun signOut() {
        authManager.logOut()
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