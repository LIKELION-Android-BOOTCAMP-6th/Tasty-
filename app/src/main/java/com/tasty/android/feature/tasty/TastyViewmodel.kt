package com.tasty.android.feature.tasty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasty.android.core.firebase.FirestoreManager
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TastySortType {
    LATEST,
    VIEW_COUNT
}

data class TastyItemUiModel(
    val tastyId: String = "",
    val title: String = "",
    val thumbnailImageUrl: String = "",
    val likeCount: Int = 0,
    val viewCount: Int = 0
)

data class TastyUiState(
    val isLoading: Boolean = false,
    val selectedSortType: TastySortType = TastySortType.LATEST,
    val tastyList: List<TastyItemUiModel> = emptyList(),
    val errorMessage: String? = null
)

class TastyViewModel : ViewModel() {

    private val firestoreManager = FirestoreManager()

    private val _uiState = MutableStateFlow(TastyUiState())
    val uiState: StateFlow<TastyUiState> = _uiState.asStateFlow()

    init {
        loadTastyLists()
    }

    fun selectSort(sortType: TastySortType) {
        _uiState.update {
            it.copy(
                selectedSortType = sortType
            )
        }
        loadTastyLists()
    }

    fun loadTastyLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = firestoreManager.getTastyLists(
                sortType = _uiState.value.selectedSortType
            )

            result
                .onSuccess { tastyLists ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tastyList = tastyLists.map { tastyList ->
                                tastyList.toUiModel()
                            }
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

private fun TastyList.toUiModel(): TastyItemUiModel {
    return TastyItemUiModel(
        tastyId = tastyListId,
        title = title,
        thumbnailImageUrl = thumbnailImageUrl ?: "",
        likeCount = likeCount,
        viewCount = viewCount
    )
}