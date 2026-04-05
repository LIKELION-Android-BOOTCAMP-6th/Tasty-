package com.tasty.android.feature.tastymap.model

import com.google.android.libraries.places.api.model.PhotoMetadata

// places api 파싱 데이터
data class RestaurantData(
    val name: String,
    val address: String,
    val rating: Double? = 0.0,
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val businessStatus: String,
    val photoMetadata: List<PhotoMetadata>,
    val phoneNumber: String?,
    val openingHours: List<String>?,
    val types: List<String>?,         // 식당 카테고리
    val priceLevel: Int? = 0,         // 가격대
    val feedCount: Int = 0,
    val distance: Int = 0
)

// RestaurantData.kt 에 추가

/**
 * Google Places API의 영문 타입을 한글 카테고리로 변환하는 맵
 */
private val categoryMap = mapOf(
    "restaurant" to "음식점",
    "cafe" to "카페",
    "bakery" to "베이커리",
    "bar" to "술집",
    "meal_takeaway" to "포장음식",
    "coffee_shop" to "카페",
    "korean_restaurant" to "한식",
    "japanese_restaurant" to "일식",
    "chinese_restaurant" to "중식"
)

/**
 * 카테고리 리스트 중 가장 적절한 한글 명칭 하나를 반환
 */
fun RestaurantData.getPrimaryCategory(): String {
    if (types.isNullOrEmpty()) return "미분류"

    // 맵에 정의된 핵심 키워드가 있는지 먼저 확인
    for (type in types) {
        categoryMap[type]?.let { return it }
    }

    // 정의되지 않은 타입인 경우 기본값 반환
    return "음식점"
}

/**
 * 가격대(0~4)를 Google API 기준 명칭으로 변환
 */
fun RestaurantData.formatPriceLevel(): String {
    return when (priceLevel) {
        0 -> "무료"
        1 -> "저렴함"
        2 -> "보통"
        3 -> "비쌈"
        4 -> "매우 비쌈"
        else -> "가격 정보 없음" // null이거나 범위를 벗어난 경우
    }
}