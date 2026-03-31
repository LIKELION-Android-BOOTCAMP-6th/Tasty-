package com.tasty.android.feature.feed.model

import com.google.firebase.firestore.DocumentId

data class FeedLike(
    @DocumentId
    val likeId: String = "",
    val feedId: String = "",
    val userId: String = ""
)