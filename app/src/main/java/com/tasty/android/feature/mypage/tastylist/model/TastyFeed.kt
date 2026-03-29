package com.tasty.android.feature.tastylist.model

data class TastyFeed(
    val tastyFeedId: String = "",
    val authorId: String = "",
    val title: String = "",
    val regionText: String = "",
    val thumbnailImageId: String = "",
    val feedIds: List<String> = emptyList(),
    val createdAt: String = ""
)