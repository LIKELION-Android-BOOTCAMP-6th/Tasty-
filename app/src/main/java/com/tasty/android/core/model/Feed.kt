package com.tasty.android.core.model

data class Feed(
    val feedId: String = "",
    val authorId: String = "",
    val content: String = "",
    val createdAt: String = "",
    val imagesUrl: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val rating: Int = 0,
    val shortReview: String = "",
    val businessHours: String = "",
    val businessStatus: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantPhoneNumber: String = "",
    val restaurantThumbnailUrl: String = "",
    val addressInfo: AddressInfo = AddressInfo()
)
