package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RestaurantSearchItem(
    val id: String,
    val name: String,
    val roadAddress: String
)

data class FeedWriteUiState(
    val selectedRestaurant: RestaurantSearchItem? = null,
    val rating: Int = 0,
    val content: String = "",
    val shortReview: String = "",
    val imageUris: List<String> = emptyList()
) {
    val canSubmit: Boolean
        get() = selectedRestaurant != null &&
                rating > 0 &&
                shortReview.isNotBlank() &&
                content.trim().length >= 10
}

class FeedWriteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedWriteUiState())
    val uiState: StateFlow<FeedWriteUiState> = _uiState.asStateFlow()

    fun setRestaurant(restaurant: RestaurantSearchItem) {
        _uiState.update { it.copy(selectedRestaurant = restaurant) }
    }

    fun setRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }

    fun setContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun setShortReview(shortReview: String) {
        _uiState.update { it.copy(shortReview = shortReview) }
    }

    fun addImage(uri: String) {
        _uiState.update {
            if (it.imageUris.size >= 4) it
            else it.copy(imageUris = it.imageUris + uri)
        }
    }

    fun removeImage(uri: String) {
        _uiState.update { current ->
            current.copy(imageUris = current.imageUris - uri)
        }
    }
}