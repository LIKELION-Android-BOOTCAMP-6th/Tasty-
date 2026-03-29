package com.tasty.android.feature.tastylist.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import com.tasty.android.core.model.UserSummary
import com.tasty.android.feature.feed.model.Feed

data class TastyList(
    val tastyListId: String = "",                        // Tasty 리스트 고유 ID
    val title: String = "",                            // 리스트 제목
    val thumbnailImageUrl: String? = "",                   // 썸네일 이미지 URL
    val feeds: List<Feed> = emptyList(),                   // 리스트에 포함된 피드 목록
    @ServerTimestamp
    val createdAt: Timestamp? = null,                        // 생성일 
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val author: UserSummary,        // 작성 유저 정보

    val likeCount: Int = 0,   // 좋아요 수
    val viewCount: Int = 0 // 조회수
)