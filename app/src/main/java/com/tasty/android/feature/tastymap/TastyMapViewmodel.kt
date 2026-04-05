package com.tasty.android.feature.tastymap

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.Task
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
    val lastSearchRadius: Double = 0.0,
    val lastCameraLocation: LatLng = LatLng(0.0,0.0),
    val isSearchPerformed: Boolean = false, // 서치 수행 여부
    val searchRadius: Double = 500.0, // 500m
    val isInitializingLocation: Boolean = false,
    val isLocationLoading: Boolean = true,
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
    // 디바이스 위치 서비스 체크
    fun checkAndLoadLocation(
        onResolvableException: (ResolvableApiException) -> Unit,
        onReady: () -> Unit = {}
    ) {
        locationManager.checkLocationSettings(
            onSuccess = {
                // 위치 서비스가 이미 켜져 있음
                onReady()
            },
            onFailure = { exception ->
                if (exception is ResolvableApiException) {
                    // 위치 서비스가 꺼져 있음 -> UI 레이어에 다이얼로그 요청 전달
                    onResolvableException(exception)
                } else {
                    Log.e("TastyMap", "위치 설정을 사용할 수 없습니다.")
                }
            }
        )
    }

    // 초기 위치
    fun initializeLocation(onReady: (LatLng) -> Unit) {
        // 로딩 시작
        uiState = uiState.copy(isInitializingLocation = true)

        locationManager.getCurrentLocation { lat, lon ->
            val latLng = LatLng(lat, lon)
            uiState = uiState.copy(
                userLocation = latLng,
                isLocationLoading = false,
                isInitializingLocation = false
            )
            onReady(latLng)
        }
    }

    // 식당 선택
    fun selectRestaurant(restaurant: RestaurantData, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
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

            onComplete()
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
    fun searchAndSyncRestaurants(location: LatLng, onComplete: () -> Unit) {
        // 1. 상태 업데이트 (검색 시작)
        uiState = uiState.copy(isSearching = true)

        viewModelScope.launch {
            try {
                // 2. 격자 검색 실행 (기존 콜백 방식 대신 직접 결과 수신)
                // radius 파라미터를 searchRestaurantsByGrid의 totalRangeKm로 전달 (단위 변환 주의: m -> km)
                val googleResults = placesManager.searchRestaurantsByGrid(
                    center = location,
                    totalRangeMeters = uiState.searchRadius
                )

                // 3. Firestore 데이터 연동 (ID 리스트 추출)
                val ids = googleResults.map { it.id }
                val firestoreResult = mapStoreManager.getRestaurantInfoFromIds(ids)

                // 4. 데이터 병합 (Merge)
                val mergedList = firestoreResult.fold(
                    onSuccess = { infoMap ->
                        googleResults.map { rest ->
                            val info = infoMap[rest.id]
                            rest.copy(
                                rating = info?.ratingAvg ?: rest.rating,
                                feedCount = info?.feedCount ?: 0,
                                // 사용자의 현재 위치와 식당 사이의 거리를 계산 (미터 단위)
                                distance = calculateDistance(location.latitude, location.longitude,
                                    rest.latitude, rest.longitude)
                            )
                        }
                    },
                    onFailure = {
                        Log.e("Sync", "Firestore 연동 실패: ${it.message}")
                        googleResults
                    }
                )

                // 5. UI 상태 반영
                uiState = uiState.copy(
                    restaurants = mergedList,
                    selectedRestaurant = null,
                    isSearching = false,
                    lastSearchRadius = uiState.searchRadius
                )

                // 6. 정렬 및 마무리
                setSortType(uiState.sortType)
                onComplete()

                Log.d("test", "restaurants Count: ${uiState.restaurants.count()}")

            } catch (e: Exception) {
                Log.e("Sync", "전체 검색 공정 실패: ${e.message}")
                uiState = uiState.copy(isSearching = false)
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
    fun selectRestaurantById(restaurantId: String, location: LatLng, onComplete: () -> Unit) {
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
                        syncWithFirestoreAndSelect(newRestaurant, location)
                    },
                    onFailure = { /* 에러 처리 */ })
            }

            onComplete()
        }
    }

    private fun syncWithFirestoreAndSelect(restaurant: RestaurantData, location: LatLng) {
        viewModelScope.launch {
            // Firestore에서 리뷰 개수 등 추가 정보 가져오기
            val firestoreResult = mapStoreManager.getRestaurantInfoFromIds(listOf(restaurant.id))

            val finalData = firestoreResult.fold(
                onSuccess = { infoMap ->
                    val info = infoMap[restaurant.id]
                    restaurant.copy(
                        rating = info?.ratingAvg ?: restaurant.rating,
                        feedCount = info?.feedCount ?: 0,
                        distance = calculateDistance(location.latitude, location.longitude,
                            restaurant.latitude, restaurant.longitude)
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
                    currentRestaurants.sortedBy { it.distance }
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

    fun resetSearchState() {
        uiState = uiState.copy(isSearchPerformed = false)
    }

    fun setSearchState() {
        uiState = uiState.copy(isSearchPerformed = true)
    }
    fun setLastCameraLocation(location: LatLng)
    {
        uiState = uiState.copy(lastCameraLocation = location)
    }

    fun setSearchRadius(radiusMeter: Double) {
        uiState = uiState.copy(searchRadius = radiusMeter)
    }
}
