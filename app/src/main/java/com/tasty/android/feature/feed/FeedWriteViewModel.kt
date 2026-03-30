package com.tasty.android.feature.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.core.place.RestaurantSearchItem
import com.tasty.android.feature.feed.model.AddressInfo
import com.tasty.android.feature.feed.model.Feed
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedRestaurantUiModel(
    val restaurantId: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val photoMetaDatas: List<PhotoMetadata> = emptyList()
)

data class FeedPhotoUiModel(
    val id: String,
    val uri: Uri,
    val order: Int
)

data class FeedWriteUiState(
    val selectedRestaurant: SelectedRestaurantUiModel? = null,
    val rating: Int = 0,
    val content: String = "",
    val shortReview: String = "",
    val photos: List<FeedPhotoUiModel> = emptyList(),
    val isSubmitting: Boolean = false,
    val isLoadingRestaurants: Boolean = false, // 식당 정보 로딩 상태
    val errorMessage: String? = null
) {
    val isRestaurantSelected: Boolean
        get() = selectedRestaurant != null

    val isContentValid: Boolean
        get() = content.trim().length >= 10

    val isShortReviewValid: Boolean
        get() = shortReview.trim().isNotBlank()

    val isRatingValid: Boolean
        get() = rating in 1..5

    val canSubmit: Boolean
        get() = isRestaurantSelected &&
                isRatingValid &&
                isShortReviewValid &&
                isContentValid &&
                !isSubmitting
}




class FeedWriteViewModel(
    private val feedStoreManager: FeedStoreManager,
    private val storageManager: StorageManager,
    private val placeManager: PlaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedWriteUiState())
    val uiState: StateFlow<FeedWriteUiState> = _uiState.asStateFlow()

    // 검색결과 Flow
    private val _searchResults = MutableStateFlow<List<RestaurantSearchItem>>(emptyList())

    val searchResults: StateFlow<List<RestaurantSearchItem>> = _searchResults.asStateFlow()

    fun searchRestaurant(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            placeManager.searchPlaces(
                query,
                listOf(
                    "restaurant", "cafe", "bakery"
                )
            ).onSuccess {
                _searchResults.value = it
            }
        }
    }

    // 검색화면 내 식당 선택
    fun selectRestaurant(
        restaurantId: String
    ) {
        viewModelScope.launch {
            _searchResults.value = emptyList()
            _uiState.update {currentState ->
                currentState.copy(isLoadingRestaurants = true)
            }

            placeManager.getRestaurantDetails(restaurantId).onSuccess { restaurant ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingRestaurants = false,
                        selectedRestaurant = SelectedRestaurantUiModel(
                            restaurantId = restaurant.id ?: "",
                            name = restaurant.name ?: "",
                            address = restaurant.address ?: "",
                            lat = restaurant.latLng?.latitude ?: 0.0,
                            lon = restaurant.latLng?.longitude ?: 0.0,
                            photoMetaDatas = restaurant.photoMetadatas ?: emptyList()
                        )
                    )
                }
            }.onFailure {
                _uiState.update {currentState ->
                    currentState.copy(
                        isLoadingRestaurants = false,
                        errorMessage = "식당 정보를 불러오지 못했습니다."
                    )
                }
            }
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

    fun addPhoto(uri: Uri) {
        _uiState.update { currentState ->
            if (currentState.photos.size >= 5) {
                currentState
            } else {
                val nextOrder = currentState.photos.size + 1
                currentState.copy(
                    photos = currentState.photos + FeedPhotoUiModel(
                        id = "photo_$nextOrder",
                        uri = uri,
                        order = nextOrder
                    )
                )
            }
        }
    }

    fun removePhoto(photoId: String) {
        _uiState.update { currentState ->
            val updatedPhotos = currentState.photos
                .filterNot { it.id == photoId }
                .mapIndexed { index, photo ->
                    photo.copy(
                        id = "photo_${index + 1}",
                        order = index + 1
                    )
                }

            currentState.copy(photos = updatedPhotos)
        }
    }

    fun submitPost(authorId: String, onSuccess: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canSubmit) return

        val restaurant = currentState.selectedRestaurant ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true)
            }

            val feedId = feedStoreManager.generateFeedId()

            val feedImagesDeferred = async {
                storageManager.uploadFeedImages(
                    feedId = feedId,
                    feedImageUris = currentState.photos.map {
                        it.uri
                    }
                )
            }
            val restaurantImagesDeferred = async {
                val bitmapResult = placeManager.getRestaurantBitmapImages(
                    photoMetaDatas = restaurant.photoMetaDatas
                )
                bitmapResult.getOrNull()?.let {bimaps ->
                    storageManager.uploadRestaurantImages(
                        bitmaps = bimaps,
                        restaurantId = restaurant.restaurantId
                    )

                }
            }

            val feedImageUrls = feedImagesDeferred.await().getOrElse {
                _uiState.update {currentState ->
                    currentState.copy(
                        isSubmitting = false,
                        errorMessage = "피드 이미지 업로드 실패"
                    )
                }
                return@launch
            }

            val restaurantUrls = restaurantImagesDeferred.await()?.getOrNull() ?: emptyList()

            val feed = Feed(
                feedId = feedId,
                authorId = authorId,
                content = currentState.content,
                rating = currentState.rating,
                shortReview = currentState.shortReview,
                feedImageUrls = feedImageUrls,
                restaurantId = restaurant.restaurantId,
                restaurantImageUrls = restaurantUrls,
                restaurantName = restaurant.name,
                addressInfo = AddressInfo(
                    roadAddress = restaurant.address,
                    latitude = restaurant.lat,
                    longitude = restaurant.lon
                )
            )

            feedStoreManager.saveFeed(feed).onSuccess {
                _uiState.update { currentState -> currentState.copy(isSubmitting = false) }
                onSuccess()
            }.onFailure {
                _uiState.update {currentState.copy(isSubmitting = false, errorMessage = "피드 저장에 실패했습니다.") }
            }

        }
    }
}