package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SelectedRestaurantUiModel(
    val name: String = "",
    val address: String = ""
)

data class FeedPhotoUiModel(
    val id: String
)

data class FeedWriteUiState(
    val selectedRestaurant: SelectedRestaurantUiModel? = null,
    val rating: Int = 0,
    val content: String = "",
    val shortReview: String = "",
    val photos: List<FeedPhotoUiModel> = emptyList(),
    val isSubmitting: Boolean = false
) {
    val isRestaurantSelected: Boolean
        get() = selectedRestaurant != null

    val isContentValid: Boolean
        get() = content.trim().length >= 10

    val isShortReviewValid: Boolean
        get() = shortReview.trim().isNotBlank()

    val isRatingValid: Boolean
        get() = rating > 0

    val canSubmit: Boolean
        get() = isRestaurantSelected &&
                isRatingValid &&
                isShortReviewValid &&
                isContentValid &&
                !isSubmitting
}

class FeedWriteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedWriteUiState())
    val uiState: StateFlow<FeedWriteUiState> = _uiState.asStateFlow()

    fun selectRestaurant(
        name: String,
        address: String
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedRestaurant = SelectedRestaurantUiModel(
                    name = name,
                    address = address
                )
            )
        }
    }

    fun clearRestaurant() {
        _uiState.update { currentState ->
            currentState.copy(selectedRestaurant = null)
        }
    }

    fun updateRating(rating: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                rating = rating.coerceIn(0, 5)
            )
        }
    }

    fun updateContent(content: String) {
        _uiState.update { currentState ->
            currentState.copy(content = content)
        }
    }

    fun updateShortReview(shortReview: String) {
        _uiState.update { currentState ->
            currentState.copy(shortReview = shortReview)
        }
    }

    fun addDummyPhoto() {
        _uiState.update { currentState ->
            if (currentState.photos.size >= 4) {
                currentState
            } else {
                currentState.copy(
                    photos = currentState.photos + FeedPhotoUiModel(
                        id = "photo_${currentState.photos.size + 1}"
                    )
                )
            }
        }
    }

    fun removePhoto(photoId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                photos = currentState.photos.filterNot { it.id == photoId }
            )
        }
    }

    fun submitPost(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true) }

        // TODO: 실제 업로드 로직 연결
        _uiState.update { it.copy(isSubmitting = false) }

        onSuccess()
    }
}