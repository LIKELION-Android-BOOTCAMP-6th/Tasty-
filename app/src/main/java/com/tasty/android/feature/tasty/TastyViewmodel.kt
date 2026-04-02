package com.tasty.android.feature.tasty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasty.android.core.firebase.AuthManager
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.core.firebase.TastyUpdateEvent
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import com.tasty.android.feature.mypage.tastylist.model.TastyListLike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TastySortType {
    LATEST,
    VIEW_COUNT
}

data class TastyItemUiModel(
    val tastyId: String = "",
    val title: String = "",
    val thumbnailImageUrl: String = "",
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val isLiked: Boolean = false
)

data class TastyUiState(
    val isLoading: Boolean = false,
    val selectedSortType: TastySortType = TastySortType.LATEST,
    val tastyList: List<TastyItemUiModel> = emptyList(),
    val errorMessage: String? = null
)

class TastyViewModel : ViewModel() {

    private val tastystoreManager = TastyStoreManager()
    private val authManager = AuthManager()

    private val _uiState = MutableStateFlow(TastyUiState())
    val uiState: StateFlow<TastyUiState> = _uiState.asStateFlow()

    init {
        loadTastyLists()
        observeTastyUpdates()
    }

    private fun observeTastyUpdates() {
        viewModelScope.launch {
            tastystoreManager.tastyUpdateEvents.collectLatest { event ->
                when (event) {
                    is TastyUpdateEvent.TastyListLiked -> updateLikeState(event.tastyListId, true)
                    is TastyUpdateEvent.TastyListUnliked -> updateLikeState(event.tastyListId, false)
                    is TastyUpdateEvent.ViewCountChanged -> updateViewCount(event.tastyListId, event.newCount)
                    is TastyUpdateEvent.TastyListDeleted -> refresh()
                    else -> {}
                }
            }
        }
    }

    private fun updateLikeState(tastyListId: String, isLiked: Boolean) {
        _uiState.update { currentState ->
            val newList = currentState.tastyList.map { item ->
                if (item.tastyId == tastyListId) {
                    val newCount = if (isLiked) item.likeCount + 1 else (item.likeCount - 1).coerceAtLeast(0)
                    item.copy(isLiked = isLiked, likeCount = newCount)
                } else item
            }
            currentState.copy(tastyList = newList)
        }
    }

    private fun updateViewCount(tastyListId: String, newCount: Int) {
        _uiState.update { currentState ->
            val newList = currentState.tastyList.map { item ->
                if (item.tastyId == tastyListId) item.copy(viewCount = newCount) else item
            }
            currentState.copy(tastyList = newList)
        }
    }

    fun selectSort(sortType: TastySortType) {
        _uiState.update {
            it.copy(
                selectedSortType = sortType
            )
        }
        loadTastyLists()
    }

    fun refresh() {
        loadTastyLists()
    }

    fun loadTastyLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentUserId = authManager.getCurrentUser()?.uid
            val result = tastystoreManager.getTastyLists(
                sortType = _uiState.value.selectedSortType
            )

            result
                .onSuccess { tastyLists ->
                    val uiModels = withContext(Dispatchers.Default) {
                        tastyLists.map { tasty ->
                            async {
                                val isLiked = if (currentUserId != null) {
                                    tastystoreManager.isTastyListLiked(
                                        TastyListLike(tastyListId = tasty.tastyListId, userId = currentUserId)
                                    ).getOrDefault(false)
                                } else false
                                
                                tasty.toUiModel(isLiked)
                            }
                        }.awaitAll()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tastyList = uiModels
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "테이스티 목록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }
}

private fun TastyList.toUiModel(isLiked: Boolean): TastyItemUiModel {
    return TastyItemUiModel(
        tastyId = tastyListId,
        title = title,
        thumbnailImageUrl = thumbnailImageUrl ?: "",
        likeCount = likeCount,
        viewCount = viewCount,
        isLiked = isLiked
    )
}