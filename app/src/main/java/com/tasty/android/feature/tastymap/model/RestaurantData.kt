package com.tasty.android.feature.tastymap.model

import com.google.android.libraries.places.api.model.PhotoMetadata

// places api 파싱 데이터
data class RestaurantData(
    val name: String,
    val address: String,
    val rating: Double?,
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val businessStatus: String,
    val photoMetadata: List<PhotoMetadata>
)