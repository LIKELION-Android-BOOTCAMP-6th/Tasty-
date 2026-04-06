package com.tasty.android.feature.feed

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.FeedUpdateEvent
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.core.firebase.TastyUpdateEvent
import com.tasty.android.core.firebase.UserStoreManager
import com.tasty.android.core.location.LocationManager
import com.tasty.android.feature.feed.mapper.toFeedPostUiModel
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.feed.model.FeedLike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

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
    val tastyLists: List<TastyListUiModel> = emptyList(),
    val feedPosts: List<FeedPostUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false, // 무한스크롤 추가 로딩 여부
    val hasMore: Boolean = true,        // 더 불러올 데이터가 있는지 여부
    val filter: FeedFilterUiState = FeedFilterUiState()
)

data class TastyListUiModel(
    val tastyListId: String,
    val title: String,
    val authorNickname: String,
    val thumbnailImageUrl: String? = null
)

data class FeedPostUiModel(
    val feedId: String,
    val authorId: String,
    val authorNickname: String,
    val userHandle: String,
    val authorProfileUrl: String? = null,
    val placeName: String,
    val address: String,
    val rating: Int,
    val description: String,
    val likeCount: Int,
    val commentCount: Int,
    val dateText: String,
    val isLiked: Boolean,
    val thumbnailImageUrl: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class FeedViewModel(
    private val feedStoreManager: FeedStoreManager,
    private val locationManager: LocationManager,
    private val userStoreManager: UserStoreManager,
    private val tastyStoreManager: TastyStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    // 캐쉬되는 서버 원본 데이터
    private var originalFeedPosts: List<FeedPostUiModel> = emptyList()

    // 최신순용 페이지네이션 cursor init
    private var lastFeedId: String? = null

    // 거리순 캐싱 데이타
    private data class DistanceCache(
        val feeds: List<Feed>, //
        val cachedLat: Double,
        val cachedLng: Double,
        val cachedAt: TimeSource.Monotonic.ValueTimeMark
    )
    private var distanceCache: DistanceCache? = null
    private val cacheExpiry = 20.minutes           // 20분 후 만료
    private val cacheInvalidateDistanceM = 3000.0  // 3km 이상 이동 시 캐시 무효화
    private val maxFetchLimit = 20

    init {
        // 실시간 피드 업데이트 이벤트 구독 (댓글 수, 좋아요 상태 동기화)
        viewModelScope.launch {
            feedStoreManager.feedUpdateEvents.collect { event ->
                when (event) {
                    is FeedUpdateEvent.FeedCreated -> {
                        loadLatestFeeds(isRefresh = true)
                    }
                    is FeedUpdateEvent.CommentCountChanged -> {
                        launch { updateFeedPostInState(event.feedId) { it.copy(commentCount = event.newCount) } }
                    }
                    is FeedUpdateEvent.LikeStatusChanged -> {
                        launch {
                            updateFeedPostInState(event.feedId) {
                                it.copy(isLiked = event.isLiked, likeCount = event.likeCount)
                            }
                        }
                    }
                    is FeedUpdateEvent.AuthorInfoChanged -> {
                        launch { updateAuthorInfoInState(event.authorId, event.newNickname, event.newProfileUrl) }
                    }
                    else -> {}
                }
            }
        }

        // 테이스티 리스트 업데이트 이벤트 구독
        viewModelScope.launch {
            tastyStoreManager.tastyUpdateEvents.collect { event ->
                if (event is TastyUpdateEvent.TastyListCreated) {
                    loadFollowingTastyLists()
                }
            }
        }

        loadLatestFeeds(isRefresh = true)
        loadFollowingTastyLists()
    }

    private suspend fun updateFeedPostInState(feedId: String, transform: (FeedPostUiModel) -> FeedPostUiModel) = withContext(Dispatchers.Default) {
        _uiState.update { state ->
            val updatedPosts = state.feedPosts.map { 
                if (it.feedId == feedId) transform(it) else it 
            }
            state.copy(feedPosts = updatedPosts)
        }
        originalFeedPosts = originalFeedPosts.map {
            if (it.feedId == feedId) transform(it) else it
        }
        
        // 캐시 데이터 동기화
        distanceCache = distanceCache?.let { cache ->
            val updatedFeeds = cache.feeds.map { feed ->
                if (feed.feedId == feedId) {
                    val updatedUi = transform(feed.toFeedPostUiModel())
                    feed.copy(
                        likeCount = updatedUi.likeCount,
                        commentCount = updatedUi.commentCount
                    )
                } else feed
            }
            cache.copy(feeds = updatedFeeds)
        }
    }

    private suspend fun updateAuthorInfoInState(authorId: String, newNickname: String, newProfileUrl: String?) = withContext(Dispatchers.Default) {
        _uiState.update { state ->
            val updatedPosts = state.feedPosts.map { post ->
                if (post.authorId == authorId) {
                    post.copy(authorNickname = newNickname, authorProfileUrl = newProfileUrl)
                } else {
                    post
                }
            }
            state.copy(feedPosts = updatedPosts)
        }
        originalFeedPosts = originalFeedPosts.map { post ->
            if (post.authorId == authorId) {
                post.copy(authorNickname = newNickname, authorProfileUrl = newProfileUrl)
            } else {
                post
            }
        }

        // 캐시 데이터 동기화
        distanceCache = distanceCache?.let { cache ->
            val updatedFeeds = cache.feeds.map { feed ->
                if (feed.authorId == authorId) {
                    feed.copy(
                        authorNickname = newNickname,
                        authorProfileUrl = newProfileUrl
                    )
                } else feed
            }
            cache.copy(feeds = updatedFeeds)
        }
    }

    private suspend fun mapFeedsToUiModels(feeds: List<Feed>): List<FeedPostUiModel> = withContext(Dispatchers.Default) {
        feeds.map { feed ->
            async {
                val isLiked = feedStoreManager.isLiked(
                    FeedLike(
                        feedId = feed.feedId,
                        userId = currentUserId
                    )
                ).getOrDefault(false)

                val authorNickname = feed.authorNickname.ifBlank { "작성자" }
                val userHandle = feed.authorHandle.ifBlank { "tastier" }

                feed.toFeedPostUiModel(
                    authorNickname = authorNickname,
                    userHandle = userHandle,
                    authorProfileUrl = feed.authorProfileUrl,
                    isLiked = isLiked
                )
            }
        }.awaitAll()
    }

    fun refresh() {
        when (_uiState.value.filter.sortType) {
            FeedSortType.LATEST -> loadLatestFeeds(isRefresh = true)
            FeedSortType.DISTANCE -> loadDistanceFeeds()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLatestFeeds(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                lastFeedId = null
                _uiState.update { it.copy(isLoading = true, hasMore = true) }
                loadFollowingTastyLists()
            } else {
                if (!_uiState.value.hasMore) return@launch
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            feedStoreManager.getFeeds(
                sortType = FeedSortType.LATEST,
                lastFeedId = lastFeedId
            ).onSuccess { feeds ->
                // 데이터 변환 및 필터링 작업을 백그라운드 스레드에서 수행
                val newUiModels = mapFeedsToUiModels(feeds)

                lastFeedId = feeds.lastOrNull()?.feedId

                // 대규모 리스트 병합 및 필터링을 백그라운드에서 처리
                val updatedOriginalPosts = withContext(Dispatchers.Default) {
                    if (isRefresh) newUiModels
                    else (originalFeedPosts + newUiModels).distinctBy { it.feedId }
                }
                originalFeedPosts = updatedOriginalPosts

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = feeds.size.toLong() >= maxFetchLimit,
                        feedPosts = updatedOriginalPosts
                    )
                }

                applyFilter(uiState.value.filter)

            }.onFailure {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDistanceFeeds() {
        viewModelScope.launch {
            val locationResult = locationManager.getCurrentLocation()
            val location = locationResult.getOrNull()
            if (location == null) {

                _uiState.update { it.copy(
                    isLoading = false
                ) }
                return@launch
            }
            val myLat = location.first
            val myLng = location.second

            val cache = distanceCache
            if (cache != null && isCacheValid(cache, myLat, myLng)) {
                val uiModels = mapFeedsToUiModels(cache.feeds)
                originalFeedPosts = uiModels
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        feedPosts = applyRegionFilter(uiModels, currentState.filter)
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            feedStoreManager.getFeeds(
                sortType = FeedSortType.DISTANCE,
                userLat = myLat,
                userLon = myLng
            ).onSuccess { feeds ->
                // 리스트 매핑을 백그라운드에서 수행
                val uiModels = mapFeedsToUiModels(feeds)

                distanceCache = DistanceCache(
                    feeds = feeds, // 원본 Feed 리스트 저장
                    cachedLat = myLat,
                    cachedLng = myLng,
                    cachedAt = TimeSource.Monotonic.markNow()
                )
                originalFeedPosts = uiModels

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        feedPosts = applyRegionFilter(uiModels, currentState.filter)
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun isCacheValid(
        cache: DistanceCache,
        lat: Double,
        lng: Double
    ): Boolean {
        val isNotExpired = cache.cachedAt.elapsedNow() < cacheExpiry
        val isNearby = locationManager
            .calculateDistanceBetween(
                cache.cachedLat,
                cache.cachedLng,
                lat,
                lng) < cacheInvalidateDistanceM

        return isNotExpired && isNearby
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun applyFilter(newFilter: FeedFilterUiState) {
        viewModelScope.launch {
            val currentSort = _uiState.value.filter.sortType

            if (newFilter.sortType != currentSort) {
                _uiState.update { it.copy(filter = newFilter) }
                when (newFilter.sortType) {
                    FeedSortType.LATEST -> loadLatestFeeds(isRefresh = true)
                    FeedSortType.DISTANCE -> loadDistanceFeeds()
                }
                return@launch
            }

            // 필터링 작업을 백그라운드 스레드에서 수행
            val filteredPosts = withContext(Dispatchers.Default) {
                applyRegionFilter(originalFeedPosts, newFilter)
            }

            _uiState.update { it.copy(
                filter = newFilter,
                feedPosts = filteredPosts
            )}
        }
    }

    private fun applyRegionFilter(
        feeds: List<FeedPostUiModel>,
        filter: FeedFilterUiState
    ): List<FeedPostUiModel> {
        return feeds
            .let {
                if (filter.mainRegion.isBlank()) it else it.filter { feed ->
                    feed.address.contains(filter.mainRegion)
                }
            }
            .let {
                if (filter.subRegion.isBlank()) it else it.filter { feed ->
                    feed.address.contains(filter.subRegion)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadMoreFeeds() {
        if (_uiState.value.filter.sortType != FeedSortType.LATEST) return
        if (_uiState.value.isLoadingMore) return
        loadLatestFeeds(isRefresh = false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun invalidateCacheAndRefresh() {
        distanceCache = null
        loadLatestFeeds(isRefresh = true)
    }

    fun toggleLike(feedId: String) {
        val currentPosts = _uiState.value.feedPosts
        val currentPost = currentPosts.find { it.feedId == feedId } ?: return
        val isCurrentlyLiked = currentPost.isLiked
        val safeUserId = if (currentUserId.isBlank()) "anonymous_user" else currentUserId

        viewModelScope.launch {
            // 원거리 정렬 캐시 및 상태 통합 업데이트
            updateFeedPostInState(feedId) { post ->
                post.copy(
                    isLiked = !isCurrentlyLiked,
                    likeCount = if (isCurrentlyLiked) post.likeCount - 1 else post.likeCount + 1
                )
            }

            val feedLike = FeedLike(feedId = feedId, userId = safeUserId)
            val result = if (isCurrentlyLiked) {
                feedStoreManager.unlikeFeed(feedLike)
            } else {
                feedStoreManager.likeFeed(feedLike)
            }

            result.onFailure {
                // 실패 시 롤백
                updateFeedPostInState(feedId) { post ->
                    post.copy(
                        isLiked = isCurrentlyLiked,
                        likeCount = currentPost.likeCount
                    )
                }
            }
        }
    }

    private fun loadFollowingTastyLists() {
        viewModelScope.launch {
            if (currentUserId.isBlank()) return@launch

            userStoreManager.getFollowingUserIds(currentUserId).onSuccess { followingIds ->
                Log.d("FeedViewModel", "Following User IDs: $followingIds")
                if (followingIds.isEmpty()) {
                    _uiState.update { it.copy(tastyLists = emptyList()) }
                    return@launch
                }

                val usersResult = userStoreManager.getUsers(followingIds)

                val uiModels = withContext(Dispatchers.Default) {
                    val nicknameMap = usersResult.getOrNull()?.associate { it.userId to it.nickname } ?: emptyMap()

                    tastyStoreManager.getTastyListsByUserIds(followingIds).getOrNull()?.map { item ->
                        val authorNickname = nicknameMap[item.authorId] ?: "사용자"
                        TastyListUiModel(
                            tastyListId = item.tastyListId,
                            title = item.title,
                            authorNickname = authorNickname,
                            thumbnailImageUrl = item.thumbnailImageUrl
                        )
                    } ?: emptyList()
                }

                _uiState.update { it.copy(tastyLists = uiModels) }
            }
        }
    }

    fun checkLocationSettings(
        onResolvableException: (ResolvableApiException) -> Unit,
        onSuccess: () -> Unit
    ) {
        locationManager.checkLocationSettings(
            onSuccess = onSuccess,
            onFailure = { exception ->
                if (exception is ResolvableApiException) {
                    onResolvableException(exception)
                } else {
                    Log.e("FeedViewModel", "위치 설정을 사용할 수 없습니다: ${exception.message}")
                }
            }
        )
    }
}