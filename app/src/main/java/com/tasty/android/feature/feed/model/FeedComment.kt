package com.tasty.android.feature.feed.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class FeedComment(
    val commentId: String = "",
    val feedId: String = "",
    val authorId: String = "",
    val authorNickname: String = "", // 추가 (역정규화)
    val authorHandle: String = "",   // 추가 (역정규화)
    val authorProfileUrl: String? = null, // 추가
    val content: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)