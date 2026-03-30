package com.tasty.android.feature.feed.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Feed(
    val feedId: String = "",
    val authorId: String = "",
    val content: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val geohash: String = "", // 위경도 기반 거리 계산용
    val feedImageUrls: List<String> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val rating: Int = 0,
    val shortReview: String = "",
    val businessHours: String = "",
    val businessStatus: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantPhoneNumber: String = "",
    val restaurantImageUrls: List<String> = emptyList(),
    val addressInfo: AddressInfo = AddressInfo()
)
