package com.tasty.android.feature.tasty

import android.util.Log
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

class TastyViewModel(
    private val tastyStoreManager: TastyStoreManager,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TastyUiState())
    val uiState: StateFlow<TastyUiState> = _uiState.asStateFlow()

    private var observeJob: kotlinx.coroutines.Job? = null

    init {
        observeTastyLists()
        observeTastyUpdates()
    }

    private fun observeTastyUpdates() {
        viewModelScope.launch {
            tastyStoreManager.tastyUpdateEvents.collectLatest { event ->
                when (event) {
                    is TastyUpdateEvent.TastyListDeleted -> refresh()
                    else -> {}
                }
            }
        }
    }

    fun selectSort(sortType: TastySortType) {
        _uiState.update {
            it.copy(
                selectedSortType = sortType
            )
        }
        observeTastyLists()
    }

    fun refresh() {
        observeTastyLists()
    }

    private fun observeTastyLists() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val sortType = _uiState.value.selectedSortType
            tastyStoreManager.getTastyListsFlow(sortType).collect { tastyLists ->
                val currentUserId = authManager.getCurrentUser()?.uid
                
                val uiModels = withContext(Dispatchers.Default) {
                    tastyLists.map { tasty ->
                        async {
                            val isLiked = if (currentUserId != null) {
                                tastyStoreManager.isTastyListLiked(

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