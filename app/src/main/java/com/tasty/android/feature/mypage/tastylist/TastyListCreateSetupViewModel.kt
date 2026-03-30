package com.tasty.android.feature.tastylist

import androidx.lifecycle.ViewModel
import com.tasty.android.feature.tastylist.model.TastyFeed
import com.tasty.android.feature.tastylist.model.TastyList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TastyListCreateSetupUiState(
    val thumbnailImageUrl: String = "",
    val title: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val isTitleValid: Boolean
        get() = title.trim().length in 4..20

    val canComplete: Boolean
        get() = isTitleValid && thumbnailImageUrl.isNotBlank() && !isSaving
}

class TastyListCreateSetupViewModel : ViewModel() {

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

    fun buildTastyList(authorId: String = "tempAuthorId"): TastyList {
        return TastyList(
            tastyListId = "",
            authorId = authorId,
            title = _uiState.value.title.trim(),
            thumbnailImageUrl = _uiState.value.thumbnailImageUrl,
            likeCount = 0,
            viewCount = 0,
            createdAt = "",
            updatedAt = ""
        )
    }

    fun buildTastyFeed(authorId: String = "tempAuthorId"): TastyFeed {
        return TastyFeed(
            tastyFeedId = "",
            authorId = authorId,
            title = _uiState.value.title.trim(),
            regionText = "",
            thumbnailImageId = _uiState.value.thumbnailImageUrl,
            feedIds = TastyListCreateDraftStore.selectedFeedIds,
            createdAt = ""
        )
    }

    fun completeCreation(): Boolean {
        val currentState = _uiState.value

        if (currentState.title.trim().length !in 4..20) {
            _uiState.update {
                it.copy(errorMessage = "제목은 4자 이상 20자 이하로 입력해주세요.")
            }
            return false
        }

        if (currentState.thumbnailImageUrl.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "썸네일을 선택해주세요.")
            }
            return false
        }

        if (TastyListCreateDraftStore.selectedFeedIds.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "선택된 피드가 없습니다.")
            }
            return false
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        val tastyList = buildTastyList()
        val tastyFeed = buildTastyFeed()

        // TODO:
        // Firebase Firestore 연결 후 저장
        // 1) tastyLists 컬렉션에 tastyList 저장
        // 2) 생성된 tastyListId 하위 tastyFeeds 서브컬렉션에 tastyFeed 저장
        // 3) 필요 시 createdAt / updatedAt / 문서 ID 반영
        // 현재는 화면만 구현하는 단계라 실제 저장은 하지 않음

        println(tastyList)
        println(tastyFeed)

        _uiState.update { it.copy(isSaving = false) }
        return true
    }

    fun clearDraft() {
        TastyListCreateDraftStore.clear()
        _uiState.value = TastyListCreateSetupUiState()
    }
}