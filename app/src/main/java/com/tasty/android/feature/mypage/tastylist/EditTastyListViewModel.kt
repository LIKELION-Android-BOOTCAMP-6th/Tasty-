package com.tasty.android.feature.mypage.tastylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.MyPageStoreManager
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.feature.mypage.MyFeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditTastyListUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val errorMessage: String? = null,
    val myFeeds: List<MyFeedItem> = emptyList(),
    val selectedFeedIds: Set<String> = emptySet(),
    val isSaveEnabled: Boolean = false,
    val isUpdateSuccess: Boolean = false,
    val thumbnailImageUrl: String = ""
)

class EditTastyListViewModel(
    private val tastyStoreManager: TastyStoreManager,
    private val myPageStoreManager: MyPageStoreManager,
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {

    private var tastyListId: String = ""
    private val _uiState = MutableStateFlow(EditTastyListUiState())
    val uiState: StateFlow<EditTastyListUiState> = _uiState.asStateFlow()

    fun initLoad(id: String, currentUserId: String) {
        if (tastyListId == id) return
        tastyListId = id
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // 1. 테이스티 리스트 정보 로드
            val tastyResult = tastyStoreManager.getTastyList(tastyListId)
            val tastyList = tastyResult.getOrNull()
            
            if (tastyList == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "리스트 정보를 불러오지 못했습니다.") }
                return@launch
            }

            // 2. 내 피드 목록 로드 (선택을 위해)
            val feedsResult = myPageStoreManager.getMyFeeds(currentUserId)
            val feeds = feedsResult.getOrNull() ?: emptyList()
            
            // 무거운 리스트 매핑 및 변환 작업을 Default 디스패처에서 수행 (ANR 방지)
            val myFeedItems = withContext(Dispatchers.Default) {
                feeds.map { feed ->
                    MyFeedItem(
                        feedId = feed.feedId,
                        thumbnailUrl = feed.feedImageUrls.firstOrNull(),
                        hasImages = feed.feedImageUrls.isNotEmpty()
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    title = tastyList.title,
                    selectedFeedIds = tastyList.feedIds.toSet(),
                    myFeeds = myFeedItems,
                    thumbnailImageUrl = tastyList.thumbnailImageUrl ?: "",
                    isSaveEnabled = tastyList.feedIds.isNotEmpty()
                )
            }

        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        validate()
    }

    fun toggleFeedSelection(feedId: String) {
        _uiState.update { state ->
            val newSelected = state.selectedFeedIds.toMutableSet()
            if (newSelected.contains(feedId)) {
                newSelected.remove(feedId)
            } else {
                newSelected.add(feedId)
            }
            state.copy(selectedFeedIds = newSelected)
        }
        validate()
    }

    private fun validate() {
        _uiState.update { state ->
            state.copy(
                isSaveEnabled = state.title.isNotBlank() && state.selectedFeedIds.isNotEmpty()
            )
        }
    }

    fun saveChanges() {
        val currentState = _uiState.value
        if (!currentState.isSaveEnabled || tastyListId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val result = tastyStoreManager.updateTastyList(
                tastyListId = tastyListId,
                title = currentState.title,
                feedIds = currentState.selectedFeedIds.toList()
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isUpdateSuccess = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        errorMessage = "저장에 실패했습니다: ${result.exceptionOrNull()?.message}" 
                    ) 
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
