package com.tasty.android.feature.tastymap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.tasty.android.feature.tastymap.model.RestaurantInfo
import kotlinx.coroutines.launch

enum class MapSortType {
    DISTANCE,
    RATING,
    REVIEW_COUNT
}

class TastyMapViewmodel(
    val locationManager: LocationManager,
    val placesManager: PlaceManager,
    val fusedLocationClient: FusedLocationProviderClient,
    private val mapStoreManager: MapStoreManager,
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                TastyMapViewmodel(
                    // 인스턴스 생성
                    placesManager = app.container.placeManager,
                    locationManager = app.container.locationManager,
                    // 위치 서비스 클라이언트 초기화
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(app),
                    mapStoreManager = app.container.mapStoreManager,
                    feedStoreManager = app.container.feedStoreManager
                )
            }
        }
    }

    // 식당 목록
    var restaurants by mutableStateOf<List<RestaurantData>>(emptyList())
        private set

    // 피드 목록
    var restaurantFeeds by mutableStateOf<List<Feed>>(emptyList())
        private set

    // 피드 로딩 상태
    var isFeedsLoading by mutableStateOf(false)
        private set

    fun searchAndSyncRestaurants(location: LatLng, radius: Double) {
        placesManager.searchRestaurants(location, radius) { googleResults ->
            viewModelScope.launch {
                val ids = googleResults.map { it.id }

                // 1. Firestore에서 해당 식당들의 통계 정보 가져오기
                val firestoreResult = mapStoreManager.getRestaurantInfoFromIds(ids)

                firestoreResult.onSuccess { infoMap ->
                    // 2. Google 데이터와 Firestore 데이터 결합
                    restaurants = googleResults.map { rest ->
                        val info = infoMap[rest.id]
                        rest.copy(
                            rating = info?.ratingAvg ?: rest.rating,
                            feedCount = info?.feedCount ?: 0
                        )
                    }
                }.onFailure {
                    // 실패 시 Google 데이터라도 표시
                    restaurants = googleResults
                }
            }
        }
    }

    // 식당 ID로 피드 가져오기
    fun fetchFeedsForRestaurant(restaurantId: String) {
        viewModelScope.launch {
            isFeedsLoading = true
            val result = mapStoreManager.getFeedsByRestaurantId(restaurantId)
            result.onSuccess { feeds ->
                restaurantFeeds = feeds
            }.onFailure {
                restaurantFeeds = emptyList() // 실패 시 빈 리스트
            }
            isFeedsLoading = false
        }
    }

    fun clearFeeds() {
        restaurantFeeds = emptyList()
    }
}