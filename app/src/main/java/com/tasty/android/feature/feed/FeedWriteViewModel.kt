package com.tasty.android.feature.feed

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.firebase.UserStoreManager
import com.tasty.android.core.location.LocationManager
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.core.place.RestaurantSearchItem
import com.tasty.android.feature.feed.model.AddressInfo
import com.tasty.android.feature.feed.model.Feed
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
    val mainRegion: String = "",
    val subRegion: String = "",
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
        get() = content.trim().length in 10..300

    val isShortReviewValid: Boolean
        get() = shortReview.trim().length in 5..25

    val isRatingValid: Boolean
        get() = rating in 1..5

    val hasPhoto: Boolean
        get() = !photos.isEmpty()

    val canSubmit: Boolean
        get() = isRestaurantSelected &&
                isRatingValid &&
                isShortReviewValid &&
                isContentValid &&
                !isSubmitting &&
                hasPhoto

}




class FeedWriteViewModel(
    private val feedStoreManager: FeedStoreManager,
    private val storageManager: StorageManager,
    private val placeManager: PlaceManager,
    private val locationManager: LocationManager,
    private val userStoreManager: UserStoreManager // 추가
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
                val components = restaurant.addressComponents?.asList() ?: emptyList()
                // 메인 Region
                val adminArea = components.find { it.types.contains("administrative_area_level_1") }?.name ?: ""
                // 서브 Region 추출
                val locality = components.find { it.types.contains("locality") }?.name ?: ""
                val subLocality = components.find { it.types.contains("sublocality_level_1") }?.name ?: ""

                val extractedMainRegion = adminArea
                val extractedSubRegion = locality.ifEmpty { subLocality }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingRestaurants = false,
                        selectedRestaurant = SelectedRestaurantUiModel(
                            restaurantId = restaurant.id ?: "",
                            name = restaurant.name ?: "",
                            address = restaurant.address ?: "",
                            lat = restaurant.latLng?.latitude ?: 0.0,
                            lon = restaurant.latLng?.longitude ?: 0.0,
                            mainRegion = extractedMainRegion,
                            subRegion = extractedSubRegion,
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

    @SuppressLint("MissingPermission")
    fun loadNearbyRestaurants() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoadingRestaurants = true
                )

            }

            val locationResult = locationManager.getCurrentLocation()
            val location = locationResult.getOrNull()

            if (location != null) {
                placeManager.searchNearbyRestaurants(
                    latitude = location.first,
                    longitude = location.second
                ).onSuccess {places ->
                    _searchResults.value = places.map{ place ->
                        RestaurantSearchItem(
                            restaurantId = place.id ?: return@launch,
                            name = place.name ?: "이름 없음"
                        )

                    }

                }
            } else {
                _searchResults.value = emptyList()
            }
            _uiState.update { currentState ->
                currentState.copy(
                    isLoadingRestaurants = false
                )
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
            currentState.copy(content = content.take(300))
        }
    }

    fun updateShortReview(shortReview: String) {
        _uiState.update { currentState ->
            currentState.copy(shortReview = shortReview.take(25))
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
            // 측정 전체 시작 시각
            val startTime = System.currentTimeMillis()
            android.util.Log.d("FeedUpload", "업로드 시퀀스 시작")
            
            _uiState.update {
                it.copy(isSubmitting = true)
            }

            val feedId = feedStoreManager.generateFeedId().getOrNull() ?: ""

            // 1. 이미지 처리 & 업로드 측정
            val uploadStartTime = System.currentTimeMillis()
            val feedImageUrlsResult = storageManager.uploadFeedImages(
                feedId = feedId,
                feedImageUris = currentState.photos.map { it.uri }
            )
            val uploadEndTime = System.currentTimeMillis()
            android.util.Log.d("FeedUpload", " [이미지 업로드]: ${(uploadEndTime - uploadStartTime) / 1000.0}초")

            val feedImageUrls = feedImageUrlsResult.getOrElse {
                _uiState.update { currentState ->
                    currentState.copy(
                        isSubmitting = false,
                        errorMessage = "피드 이미지 업로드 실패"
                    )
                }
                return@launch
            }

            // 유저 정보 조회 측정 (캐시 여부 확인)
            val userStartTime = System.currentTimeMillis()
            val userProfile = userStoreManager.currentUserProfile.value 
                ?: userStoreManager.getUser(authorId).getOrNull() 
            val userEndTime = System.currentTimeMillis()
            android.util.Log.d("FeedUpload", " [유저 정보 조회]: ${(userEndTime - userStartTime) / 1000.0}초")

            val feed = Feed(
                feedId = feedId,
                authorId = authorId,
                authorNickname = userProfile?.nickname ?: "익명",
                authorHandle = userProfile?.userHandle ?: "anonymous",
                authorProfileUrl = userProfile?.profileImageUrl,
                content = currentState.content,
                rating = currentState.rating,
                shortReview = currentState.shortReview,
                feedImageUrls = feedImageUrls,
                restaurantId = restaurant.restaurantId,
                restaurantName = restaurant.name,
                addressInfo = AddressInfo(
                    roadAddress = restaurant.address,
                    latitude = restaurant.lat,
                    longitude = restaurant.lon,
                    mainRegion = restaurant.mainRegion,
                    subRegion = restaurant.subRegion
                )
            )

            // 피드 저장 (Firestore DB) 측정
            val dbStartTime = System.currentTimeMillis()
            feedStoreManager.saveFeed(feed).onSuccess {
                val dbEndTime = System.currentTimeMillis()
                android.util.Log.d("FeedUpload", "[DB 문서 저장]: ${(dbEndTime - dbStartTime) / 1000.0}초")
                
                // 전체 측정 종료
                val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
                android.util.Log.d("FeedUpload", "==== [FINISH] 총 소요 시간: ${totalTime}초 ====")

                _uiState.update { currentState -> currentState.copy(isSubmitting = false) }
                onSuccess()
            }.onFailure {
                _uiState.update { currentState -> 
                    currentState.copy(isSubmitting = false, errorMessage = "피드 저장에 실패했습니다.") 
                }
            }
        }
    }
}
