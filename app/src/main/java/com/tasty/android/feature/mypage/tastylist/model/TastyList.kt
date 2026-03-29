package com.tasty.android.feature.tastylist.model

data class TastyList(
    val tastyListId: String = "",
    val authorId: String = "",
    val title: String = "",
    val thumbnailImageUrl: String = "",
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)