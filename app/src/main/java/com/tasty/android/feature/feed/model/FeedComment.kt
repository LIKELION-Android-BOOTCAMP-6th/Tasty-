package com.tasty.android.feature.feed.model

data class FeedComment(
    val commentId: String = "",
    val feedId: String = "",
    val authorId: String = "",
    val content: String = "",
    val createdAt: String = ""
)