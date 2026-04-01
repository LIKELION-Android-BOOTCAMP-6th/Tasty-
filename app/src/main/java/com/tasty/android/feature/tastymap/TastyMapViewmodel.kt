package com.tasty.android.feature.tastymap

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tasty.android.MyApplication
import com.tasty.android.core.location.LocationManager
import com.tasty.android.core.place.PlaceManager

enum class MapSortType {
    DISTANCE,
    RATING,
    REVIEW_COUNT
}

class TastyMapViewmodel(
    val locationManager: LocationManager,
    val placesManager: PlaceManager,
    val fusedLocationClient: FusedLocationProviderClient
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
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
                )
            }
        }
    }

}