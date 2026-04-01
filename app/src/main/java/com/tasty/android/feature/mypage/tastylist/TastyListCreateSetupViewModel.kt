package com.tasty.android.feature.tastylist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.firebase.TastyStoreManager
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TastyListCreateSetupUiState(
    val thumbnailImageUrl: String = "",
    val title: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val isTitleValid: Boolean
        get() = title.trim().length in 4..20

    val canComplete: Boolean
        get() = isTitleValid && thumbnailImageUrl.isNotBlank() && !isSaving
}

class TastyListCreateSetupViewModel(
    private val tastyStoreManager: TastyStoreManager,
    private val storageManager: StorageManager
) : ViewModel() {

    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(TastyListCreateSetupUiState())
    val uiState: StateFlow<TastyListCreateSetupUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        if (title.length > 20) return
        _uiState.update {
            it.copy(
                title = title,
                errorMessage = null
            )
        }
    }

    fun updateThumbnailImageUrl(url: String) {
        _uiState.update {
            it.copy(
                thumbnailImageUrl = url,
                errorMessage = null
            )
        }
    }

    fun buildTastyList(tastyListId: String, authorId: String, thumbnailUrl: String): TastyList {
        return TastyList(
            tastyListId = tastyListId,
            authorId = authorId,
            title = _uiState.value.title.trim(),
            thumbnailImageUrl = thumbnailUrl,
            feedIds = TastyListCreateDraftStore.selectedFeedIds,
            likeCount = 0,
            viewCount = 0,
            createdAt = null,
            updatedAt = null
        )
    }

    fun completeCreation() {
        val currentState = _uiState.value
        val userId = currentUserId

        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "로그인이 필요합니다.") }
            return
        }

        if (currentState.title.trim().length !in 4..20) {
            _uiState.update { it.copy(errorMessage = "제목은 4자 이상 20자 이하로 입력해주세요.") }
            return
        }

        if (currentState.thumbnailImageUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "썸네일을 선택해주세요.") }
            return
        }

        val selectedFeeds = TastyListCreateDraftStore.selectedFeedIds
        if (selectedFeeds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "선택된 피드가 없습니다.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val tastyListId = tastyStoreManager.generateTastyListId()
                var finalThumbnailUrl = currentState.thumbnailImageUrl

                // 로컬 이미지인 경우 업로드 진행
                if (finalThumbnailUrl.startsWith("content://") || finalThumbnailUrl.startsWith("file://")) {
                    val uploadResult = storageManager.uploadThumbnailImages(
                        thumbnailImageUri = Uri.parse(finalThumbnailUrl),
                        tastyListId = tastyListId
                    )
                    
                    if (uploadResult.isSuccess) {
                        finalThumbnailUrl = uploadResult.getOrThrow()
                    } else {
                        _uiState.update { 
                            it.copy(
                                isSaving = false, 
                                errorMessage = "이미지 업로드에 실패했습니다: ${uploadResult.exceptionOrNull()?.message}"
                            ) 
                        }
                        return@launch
                    }
                }

                val tastyList = buildTastyList(tastyListId, userId, finalThumbnailUrl)
                val saveResult = tastyStoreManager.createTastyList(tastyList)

                if (saveResult.isSuccess) {
                    clearDraft()
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                } else {
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            errorMessage = "목록 저장에 실패했습니다: ${saveResult.exceptionOrNull()?.message}"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isSaving = false, errorMessage = "오류가 발생했습니다: ${e.message}") 
                }
            }
        }
    }

    fun clearDraft() {
        TastyListCreateDraftStore.clear()
        _uiState.update { 
            it.copy(
                thumbnailImageUrl = "",
                title = "",
                isSaving = false
            )
        }
    }
}