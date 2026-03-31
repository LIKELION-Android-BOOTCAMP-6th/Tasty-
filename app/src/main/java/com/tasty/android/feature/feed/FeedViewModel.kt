package com.tasty.android.feature.feed

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.location.LocationManager
import com.tasty.android.feature.feed.mapper.toFeedPostUiModel
import com.tasty.android.feature.feed.model.FeedLike
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val userRegion: String = "",
    val tastyLists: List<TastyListUiModel> = emptyList(),
    val feedPosts: List<FeedPostUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false, // 무한스크롤 추가 로딩 여부
    val hasMore: Boolean = true,        // 더 불러올 데이터가 있는지 여부
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
    val dateText: String,
    val isLiked: Boolean,
    val imageUrl: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class FeedViewModel(
    private val feedStoreManager: FeedStoreManager,
    private val locationManager: LocationManager
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
        val feeds: List<FeedPostUiModel>,
        val cachedLat: Double,
        val cachedLng: Double,
        val cachedAt: TimeSource.Monotonic.ValueTimeMark
    )
    private var distanceCache: DistanceCache? = null
    private val cacheExpiry = 20.minutes           // 5분 후 만료
    private val cacheInvalidateDistanceM = 3000.0  // 3km 이상 이동 시 캐시 무효화
    private val maxFetchLimit = 20

    init {
        loadLatestFeeds(isRefresh = true)
    }

    /** 최신순 피드 로딩**/
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLatestFeeds(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                // 새로고침: 커서 초기화 + 전체 로딩 상태
                lastFeedId = null
                originalFeedPosts = emptyList()
                _uiState.update { it.copy(isLoading = true, hasMore = true) }
            } else {
                // 추가 로딩: 더 불러올 데이터가 없으면 중단
                if (!_uiState.value.hasMore) return@launch
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            feedStoreManager.getFeeds(
                sortType = FeedSortType.LATEST,
                lastFeedId = lastFeedId
            ).onSuccess { feeds ->
                val newUiModels = feeds.map { feed ->
                    async {
                        val isLiked = feedStoreManager.isLiked(
                            FeedLike(
                                feedId = feed.feedId,
                                userId = currentUserId ?: ""
                            )
                        ).getOrDefault(false)

                        feed.toFeedPostUiModel().copy(isLiked = isLiked)
                    }
                }.awaitAll()

                // 커서 업데이트 (마지막 피드 ID 저장)
                lastFeedId = feeds.lastOrNull()?.feedId

                // 원본 리스트 누적 (새로고침이면 교체, 추가 로딩이면 append)
                originalFeedPosts = if (isRefresh) newUiModels
                                    else (originalFeedPosts + newUiModels).distinctBy { it.id }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        // 가져온 수 < limit 이면 마지막 페이지
                        hasMore = feeds.size.toLong() >= maxFetchLimit,
                        feedPosts = applyRegionFilter(originalFeedPosts, currentState.filter)
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, isLoadingMore = false) }
            }


        }
    }


    /** 거리순 cached 피드 로딩**/
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDistanceFeeds() {
        viewModelScope.launch {
            val location = locationManager.getCurrentLocation().getOrNull() ?: return@launch
            val myLat = location.first
            val myLng = location.second


            val cache = distanceCache
            if (cache != null && isCacheValid(cache, myLat, myLng)) {

                originalFeedPosts = cache.feeds
                _uiState.update { currentState ->
                    currentState.copy(
                        feedPosts = applyRegionFilter(cache.feeds, currentState.filter)
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
                val uiModels = feeds.map { it.toFeedPostUiModel() }

                // 초기화 시점
                distanceCache = DistanceCache(
                    feeds = uiModels,
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

    // 캐시 유효성 판단 (20분 이내 && 3km 이내)
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


    /** 필터 적용 **/
    @RequiresApi(Build.VERSION_CODES.O)
    fun applyFilter(newFilter: FeedFilterUiState) {
        val currentSort = _uiState.value.filter.sortType

        if (newFilter.sortType != currentSort) {
            _uiState.update { it.copy(filter = newFilter) }
            when (newFilter.sortType) {
                FeedSortType.LATEST -> loadLatestFeeds(isRefresh = true)
                FeedSortType.DISTANCE -> loadDistanceFeeds()
            }
            return
        }

        _uiState.update { it.copy(
            filter = newFilter,
            feedPosts = applyRegionFilter(originalFeedPosts, newFilter)
        )}
    }

    // 지역 필터링
    private fun applyRegionFilter(
        feeds: List<FeedPostUiModel>,
        filter: FeedFilterUiState
    ): List<FeedPostUiModel> {
        return feeds
            .let { // 상위 지역 필터
                if (filter.mainRegion.isBlank()) it else it.filter { feed ->
                    feed.address.contains(filter.mainRegion)
                }
            }
            .let {// 하위 지역 필터
                if (filter.subRegion.isBlank()) it else it.filter { feed ->
                    feed.address.contains(filter.subRegion)
                }
            }
    }

    // 무한 스크롤 (최신순 전용)
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadMoreFeeds() {
        if (_uiState.value.filter.sortType != FeedSortType.LATEST) return
        if (_uiState.value.isLoadingMore) return
        loadLatestFeeds(isRefresh = false)
    }

    // 피드 작성 완료 후 → 캐시 무효화 + 최신순 재조회
    @RequiresApi(Build.VERSION_CODES.O)
    fun invalidateCacheAndRefresh() {
        distanceCache = null
        loadLatestFeeds(isRefresh = true)
    }

    // 좋아요 토글
    fun toggleLike(feedId: String) {
        val currentPosts = _uiState.value.feedPosts
        val currentPost = currentPosts.find { it.id == feedId } ?: return
        val isCurrentlyLiked = currentPost.isLiked
        val safeUserId = if (currentUserId.isBlank()) "anonymous_user" else currentUserId

        _uiState.update { state ->
            state.copy(
                feedPosts = state.feedPosts.map { post ->
                    if (post.id == feedId) post.copy(
                        isLiked = !isCurrentlyLiked,
                        likeCount = if (isCurrentlyLiked) post.likeCount - 1
                        else post.likeCount + 1
                    )
                    else post
                }
            )
        }
        val feedLike = FeedLike(feedId = feedId, userId = safeUserId)
        viewModelScope.launch {

            val result = if (isCurrentlyLiked) {
                feedStoreManager.unlikeFeed(feedLike) // 좋아요 취소
            } else {
                feedStoreManager.likeFeed(feedLike)   // 좋아요 추가
            }

            result.onFailure {
                _uiState.update { state ->
                    state.copy(
                        feedPosts = state.feedPosts.map { post ->
                            if (post.id == feedId) post.copy(
                                isLiked = isCurrentlyLiked,          // 원래 상태로 복구
                                likeCount = currentPost.likeCount     // 원래 카운트로 복구
                            )
                            else post
                        }
                    )
                }
            }
        }
    }
}