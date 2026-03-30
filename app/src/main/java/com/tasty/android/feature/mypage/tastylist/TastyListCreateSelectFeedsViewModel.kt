package com.tasty.android.feature.tastylist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TastyListFeedSelectionItem(
    val feedId: String,
    val restaurantName: String,
    val firstImageLabel: String = "첫 번째 이미지",
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

class TastyListCreateSelectFeedsViewModel : ViewModel() {

    private val mockFeeds = List(20) { index ->
        TastyListFeedSelectionItem(
            feedId = "feed_${index + 1}",
            restaurantName = "길동이네 식당",
            oneLineReview = when (index % 4) {
                0 -> "분위기 좋고 음식도 깔끔했어요"
                1 -> "분위기 좋고 음식도 깔끔했어요"
                2 -> "분위기 좋고 음식도 깔끔했어요"
                else -> "재방문 의사 있는 맛집"
            },
            createdAt = "2026-08-06"
        )
    }

    private val _uiState = MutableStateFlow(
        TastyListCreateSelectFeedsUiState(
            allFeeds = mockFeeds,
            visibleFeeds = mockFeeds.take(10),
            currentPage = 1,
            pageSize = 10,
            hasNextPage = mockFeeds.size > 10
        )
    )
    val uiState: StateFlow<TastyListCreateSelectFeedsUiState> = _uiState.asStateFlow()

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

        _uiState.update { it.copy(isPagingLoading = true) }

        val nextPage = currentState.currentPage + 1
        val nextVisibleCount = nextPage * currentState.pageSize

        val nextVisibleFeeds = currentState.allFeeds
            .take(nextVisibleCount)
            .map { item ->
                item.copy(isSelected = currentState.selectedFeedIds.contains(item.feedId))
            }

        _uiState.update {
            it.copy(
                isPagingLoading = false,
                currentPage = nextPage,
                visibleFeeds = nextVisibleFeeds,
                hasNextPage = nextVisibleCount < currentState.allFeeds.size
            )
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