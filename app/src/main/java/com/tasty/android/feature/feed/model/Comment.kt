package com.tasty.android.feature.feed.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    val commentId: String = "",
    val feedId: String = "",
    val authorId: String = "",
    val content: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
)