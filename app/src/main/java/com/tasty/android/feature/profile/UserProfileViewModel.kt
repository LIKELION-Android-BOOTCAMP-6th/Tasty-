package com.tasty.android.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.*
import com.tasty.android.core.model.Follow
import com.tasty.android.core.model.User
import com.tasty.android.feature.feed.FeedPostUiModel
import com.tasty.android.feature.feed.mapper.toFeedPostUiModel
import com.tasty.android.feature.mypage.MyFeedItem
import com.tasty.android.feature.mypage.MyPageTab
import com.tasty.android.feature.mypage.MyProfileInfo
import com.tasty.android.feature.mypage.MyTastyListItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val profileInfo: MyProfileInfo? = null,
    val myFeeds: List<MyFeedItem> = emptyList(),
    val myTastyLists: List<MyTastyListItem> = emptyList(),
    val selectedTab: MyPageTab = MyPageTab.FEED,
    val isFollowing: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val feedCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val userHandle: String = ""
)

class UserProfileViewModel(
    private val targetUserId: String,
    private val userStoreManager: UserStoreManager,
    private val myPageStoreManager: MyPageStoreManager,
    private val tastyStoreManager: TastyStoreManager,
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""

    init {
        loadUserProfile()
        observeDataUpdates()
    }

    private fun observeDataUpdates() {
        viewModelScope.launch {
            // 좋아요나 댓글 수 변경 시 피드 목록 업데이트
            feedStoreManager.feedUpdateEvents.collect { event ->
                when (event) {
                    is FeedUpdateEvent.LikeStatusChanged -> updateFeedStatus(event.feedId, event.isLiked, event.likeCount)
                    is FeedUpdateEvent.CommentCountChanged -> updateCommentCount(event.feedId, event.newCount)
                    else -> {}
                }
            }
        }
    }

    private fun updateFeedStatus(feedId: String, isLiked: Boolean, likeCount: Int) {
        // 상세 구현 생략 (마이페이지와 유사)
    }

    private fun updateCommentCount(feedId: String, newCount: Int) {
        // 상세 구현 생략 (마이페이지와 유사)
    }

    fun refresh() {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userResultDeferred = async { userStoreManager.getUser(targetUserId) }
            val feedsResultDeferred = async { myPageStoreManager.getMyFeeds(targetUserId) }
            val tastyListsResultDeferred = async { myPageStoreManager.getMyTastyLists(targetUserId) }
            val isFollowingDeferred = async { 
                if (currentUserId.isNotBlank()) {
                    userStoreManager.isFollowing(Follow(followerUserId = currentUserId, followingUserId = targetUserId))
                } else Result.success(false)
            }

            val userResult = userResultDeferred.await()
            val feedsResult = feedsResultDeferred.await()
            val tastyListsResult = tastyListsResultDeferred.await()
            val isFollowingResult = isFollowingDeferred.await()

            if (userResult.isFailure || feedsResult.isFailure || tastyListsResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "정보를 불러오지 못했습니다.") }
                return@launch
            }

            val user = userResult.getOrNull()
            val feeds = feedsResult.getOrNull() ?: emptyList()
            val tastyLists = tastyListsResult.getOrNull() ?: emptyList()
            val isFollowing = isFollowingResult.getOrDefault(false)

            if (user != null) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        profileInfo = MyProfileInfo(
                            userId = user.userId,
                            nickname = user.nickname,
                            userHandle = user.userHandle,
                            bio = user.bio,
                            profileImageUrl = user.profileImageUrl
                        ),
                        userHandle = user.userHandle,
                        feedCount = user.feedCount,
                        followerCount = user.followerCount,
                        followingCount = user.followingCount,
                        isFollowing = isFollowing,
                        myFeeds = feeds.map { MyFeedItem(it.feedId, it.feedImageUrls.firstOrNull(), it.feedImageUrls.isNotEmpty()) },
                        myTastyLists = tastyLists.map { 
                            MyTastyListItem(
                                tastyListId = it.tastyListId,
                                title = it.title,
                                thumbnailUrl = it.thumbnailImageUrl ?: "",
                                feedCount = it.feedIds.size,
                                viewCount = it.viewCount
                            )
                        }
                    )
                }
            }
        }
    }

    fun toggleFollow() {
        if (currentUserId.isBlank() || targetUserId == currentUserId) return
        
        val follow = Follow(followerUserId = currentUserId, followingUserId = targetUserId)
        val isCurrentlyFollowing = _uiState.value.isFollowing
        
        viewModelScope.launch {
            _uiState.update { it.copy(isFollowActionLoading = true) }
            
            val result = if (isCurrentlyFollowing) {
                userStoreManager.unfollowUser(follow)
            } else {
                userStoreManager.followUser(follow)
            }
            
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        isFollowing = !isCurrentlyFollowing,
                        followerCount = if (isCurrentlyFollowing) state.followerCount - 1 else state.followerCount + 1,
                        isFollowActionLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isFollowActionLoading = false, errorMessage = "작업에 실패했습니다.") }
            }
        }
    }

    fun selectTab(tab: MyPageTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}
