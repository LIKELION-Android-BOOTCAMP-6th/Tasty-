package com.tasty.android.feature.feed.model

data class FeedPostItem (
    val id: String,
    val authorName: String,
    val authorRegion: String,
    val placeName: String,
    val address: String,
    val date: String,
    val likeCount: Int,
    val commentCount: Int,
    val rating: Float,
    val description: String,
    val imageUrl: String?
)