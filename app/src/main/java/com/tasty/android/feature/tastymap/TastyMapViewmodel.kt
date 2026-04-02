package com.tasty.android.feature.tastymap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.tasty.android.MyApplication
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.MapStoreManager
import com.tasty.android.core.location.LocationManager
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.tastymap.model.RestaurantData
import kotlinx.coroutines.launch

enum class SortType { DISTANCE, RATING }

// UI에 필요한 모든 상태를 하나의 클래스로 관리
data class TastyMapUiState(
    val sortType: SortType = SortType.DISTANCE,
    val restaurants: List<RestaurantData> = emptyList(),
    val selectedRestaurant: RestaurantData? = null,
    val isCommentVisible: Boolean = false,
    val restaurantFeeds: List<Feed> = emptyList(),
    val isFeedsLoading: Boolean = false,
    val isSearchFocused: Boolean = false,
    val userLocation: LatLng? = null,
    val isSearching: Boolean = false,
)

class TastyMapViewmodel(
    val locationManager: LocationManager,
    val placesManager: PlaceManager,
    val fusedLocationClient: FusedLocationProviderClient,
    private val mapStoreManager: MapStoreManager,
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {

    var uiState by mutableStateOf(TastyMapUiState())
        private set

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                TastyMapViewmodel(
                    placesManager = app.container.placeManager,
                    locationManager = app.container.locationManager,
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(app),
                    mapStoreManager = app.container.mapStoreManager,
                    feedStoreManager = app.container.feedStoreManager
                )
            }
        }
    }

    // 초기 위치
    fun initializeLocation(onReady: (LatLng) -> Unit) {
        locationManager.getCurrentLocation { lat, lon ->
            val latLng = LatLng(lat, lon)
            uiState = uiState.copy(userLocation = latLng)
            onReady(latLng)
        }
    }

    // 식당 선택
    fun selectRestaurant(restaurant: RestaurantData) {
        val isAlreadySelected = uiState.selectedRestaurant?.id == restaurant.id

        if (isAlreadySelected) {
            // 이미 선택된 식당을 다시 클릭한 경우 -> 리뷰(피드) 표시
            uiState = uiState.copy(isCommentVisible = true)
            if (uiState.restaurantFeeds.isEmpty()) {
                fetchFeedsForRestaurant(restaurant.id)
            }
        } else {
            // 처음 선택 시 해당 식당으로 리스트를 필터링하고 리뷰(피드)는 숨김
            uiState = uiState.copy(
                selectedRestaurant = restaurant,
                isCommentVisible = false,
                restaurantFeeds = emptyList()
            )
        }
    }

    fun clearSelection() {
        uiState = uiState.copy(
            selectedRestaurant = null,
            isCommentVisible = false
        )
    }

    fun setSearchFocus(isFocused: Boolean) {
        uiState = uiState.copy(isSearchFocused = isFocused)
    }

    // Google Places API 결과와 Firestore의 맛집 정보를 병합
    fun searchAndSyncRestaurants(location: LatLng, radius: Double, onComplete: () -> Unit) {
        uiState = uiState.copy(isSearching = true)

        placesManager.searchRestaurants(location, radius) { googleResults ->
            viewModelScope.launch {
                val ids = googleResults.map { it.id }
                val firestoreResult = mapStoreManager.getRestaurantInfoFromIds(ids)

                val mergedList = firestoreResult.fold(
                    onSuccess = { infoMap ->
                        googleResults.map { rest ->
                            val info = infoMap[rest.id]
                            rest.copy(
                                rating = info?.ratingAvg ?: rest.rating,
                                feedCount = info?.feedCount ?: 0
                            )
                        }
                    },
                    onFailure = { googleResults }
                )
                uiState = uiState.copy(
                    restaurants = mergedList,
                    selectedRestaurant = null,
                    isSearching = false
                )

                onComplete()
            }
        }
    }

    // 특정 식당에 작성된 리뷰(피드) 목록을 가져옴
    private fun fetchFeedsForRestaurant(restaurantId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isFeedsLoading = true)
            val result = mapStoreManager.getFeedsByRestaurantId(restaurantId)
            uiState = uiState.copy(
                restaurantFeeds = result.getOrDefault(emptyList()),
                isFeedsLoading = false
            )
        }
    }

    // id로 식당을 찾음
    fun selectRestaurantById(restaurantId: String) {
        viewModelScope.launch {
            // 이미 리스트에 데이터가 있는지 확인
            val target = uiState.restaurants.find { it.id == restaurantId }

            if (target != null) {
                selectRestaurant(target)
            } else {
                placesManager.fetchPlaceDetails(
                    placeId = restaurantId,
                    onSuccess = { newRestaurant ->
                        // Firestore 정보와 병합
                        syncWithFirestoreAndSelect(newRestaurant)
                    },
                    onFailure = { /* 에러 처리 */ })
            }
        }
    }

    private fun syncWithFirestoreAndSelect(restaurant: RestaurantData) {
        viewModelScope.launch {
            // Firestore에서 리뷰 개수 등 추가 정보 가져오기
            val firestoreResult = mapStoreManager.getRestaurantInfoFromIds(listOf(restaurant.id))

            val finalData = firestoreResult.fold(
                onSuccess = { infoMap ->
                    val info = infoMap[restaurant.id]
                    restaurant.copy(
                        rating = info?.ratingAvg ?: restaurant.rating,
                        feedCount = info?.feedCount ?: 0
                    )
                },
                onFailure = { restaurant }
            )

            // 해당 식당 리스트에 추가하고 선택 처리
            uiState = uiState.copy(
                restaurants = uiState.restaurants + finalData,
                selectedRestaurant = finalData
            )
        }
    }

    fun setSortType(sortType: SortType) {
        val currentRestaurants = uiState.restaurants
        val userLoc = uiState.userLocation

        val sortedList = when (sortType) {
            SortType.DISTANCE -> {
                // 유저 위치가 있을 때만 거리순 정렬, 없으면 그대로 유지
                if (userLoc != null) {
                    currentRestaurants.sortedBy { rest ->
                        // 위도/경도 차이를 이용한 단순 거리 비교 (정교한 계산이 필요하면 calculateDistance 사용)
                        val latDiff = rest.latitude - userLoc.latitude
                        val lngDiff = rest.longitude - userLoc.longitude
                        latDiff * latDiff + lngDiff * lngDiff
                    }
                } else {
                    currentRestaurants
                }
            }

            SortType.RATING -> {
                // 평점 높은 순 정렬 (null일 경우 0.0 처리)
                currentRestaurants.sortedByDescending { it.rating ?: 0.0 }
            }
        }

        // uiState 업데이트
        uiState = uiState.copy(
            sortType = sortType,
            restaurants = sortedList
        )
    }
}
