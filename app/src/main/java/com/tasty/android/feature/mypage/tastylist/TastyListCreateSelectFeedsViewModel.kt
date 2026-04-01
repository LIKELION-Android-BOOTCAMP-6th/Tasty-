package com.tasty.android.feature.tastylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.MyPageStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class TastyListFeedSelectionItem(
    val feedId: String,
    val restaurantName: String,
    val thumbnailUrl: String? = null,
    val oneLineReview: String,
    val createdAt: String,
    val isSelected: Boolean = false
)

data class TastyListCreateSelectFeedsUiState(
    val isLoading: Boolean = false,
    val isPagingLoading: Boolean = false,
    val allFeeds: List<TastyListFeedSelectionItem> = emptyList(),
    val visibleFeeds: List<TastyListFeedSelectionItem> = emptyList(),
    val selectedFeedIds: Set<String> = emptySet(),
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val hasNextPage: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCount: Int
        get() = selectedFeedIds.size

    val canGoNext: Boolean
        get() = selectedCount in 1..10
}

class TastyListCreateSelectFeedsViewModel(
    private val myPageStoreManager: MyPageStoreManager
) : ViewModel() {

    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(TastyListCreateSelectFeedsUiState())
    val uiState: StateFlow<TastyListCreateSelectFeedsUiState> = _uiState.asStateFlow()

    private var lastFeedId: String? = null

    init {
        loadMyFeeds()
    }

    private fun loadMyFeeds() {
        val userId = currentUserId
        if (userId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = myPageStoreManager.getMyFeeds(userId)
            
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "피드를 불러오지 못했습니다: ${exception?.message}"
                    ) 
                }
                android.util.Log.e("SelectFeedsVM", "Error loading feeds", exception)
                return@launch
            }

            val feeds = result.getOrNull() ?: emptyList()

            val selectionItems = feeds.map { feed ->
                TastyListFeedSelectionItem(
                    feedId = feed.feedId,
                    restaurantName = feed.restaurantName,
                    thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                    oneLineReview = feed.shortReview,
                    createdAt = feed.createdAt?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    } ?: ""
                )
            }

            lastFeedId = feeds.lastOrNull()?.feedId

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allFeeds = selectionItems,
                    visibleFeeds = selectionItems.take(10),
                    hasNextPage = selectionItems.size >= 10
                )
            }
        }
    }

    fun toggleFeedSelection(feedId: String) {
        _uiState.update { currentState ->
            val selected = currentState.selectedFeedIds.toMutableSet()

            if (selected.contains(feedId)) {
                selected.remove(feedId)
            } else {
                if (selected.size >= 10) {
                    return@update currentState.copy(
                        errorMessage = "피드는 최대 10개까지 선택할 수 있어요."
                    )
                }
                selected.add(feedId)
            }

            currentState.copy(
                selectedFeedIds = selected,
                errorMessage = null,
                visibleFeeds = currentState.visibleFeeds.map { item ->
                    if (item.feedId == feedId) {
                        item.copy(isSelected = selected.contains(feedId))
                    } else {
                        item.copy(isSelected = selected.contains(item.feedId))
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.hasNextPage || currentState.isPagingLoading) return

        val userId = currentUserId
        if (userId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPagingLoading = true) }

            val result = myPageStoreManager.getMyFeeds(userId, limit = 10, lastFeedId = lastFeedId)
            
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                _uiState.update { 
                    it.copy(
                        isPagingLoading = false, 
                        errorMessage = "추가 피드를 불러오지 못했습니다: ${exception?.message}"
                    )
                }
                android.util.Log.e("SelectFeedsVM", "Error loading next page", exception)
                return@launch
            }

            val feeds = result.getOrNull() ?: emptyList()

            if (feeds.isNotEmpty()) {
                val newItems = feeds.map { feed ->
                    TastyListFeedSelectionItem(
                        feedId = feed.feedId,
                        restaurantName = feed.restaurantName,
                        thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                        oneLineReview = feed.shortReview,
                        createdAt = feed.createdAt?.toDate()?.let {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                        } ?: "",
                        isSelected = currentState.selectedFeedIds.contains(feed.feedId)
                    )
                }

                lastFeedId = feeds.last().feedId

                _uiState.update {
                    it.copy(
                        isPagingLoading = false,
                        allFeeds = it.allFeeds + newItems,
                        visibleFeeds = it.visibleFeeds + newItems,
                        hasNextPage = feeds.size >= 10
                    )
                }
            } else {
                _uiState.update { it.copy(isPagingLoading = false, hasNextPage = false) }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveDraftSelection() {
        val selectedIds = _uiState.value.selectedFeedIds.toList()
        TastyListCreateDraftStore.selectedFeedIds = selectedIds
        TastyListCreateDraftStore.selectedFeedCount = selectedIds.size
    }
}