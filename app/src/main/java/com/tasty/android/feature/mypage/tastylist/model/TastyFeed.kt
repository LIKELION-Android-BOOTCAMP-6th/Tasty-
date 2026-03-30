package com.tasty.android.feature.mypage.tastylist.model
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class TastyFeed(
    val tastyFeedId: String = "",
    val authorId: String = "",
    val title: String = "",
    val regionText: String = "",
    val thumbnailImageUrl: String = "",
    val feedIds: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Timestamp? = null
)