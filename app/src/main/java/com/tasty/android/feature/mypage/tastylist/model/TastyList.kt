package com.tasty.android.feature.mypage.tastylist.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import com.tasty.android.core.model.UserSummary
import com.tasty.android.feature.feed.model.Feed

data class TastyList(
    val tastyListId: String = "",                        // Tasty 리스트 고유 ID
    val title: String = "",                            // 리스트 제목
    val thumbnailImageUrl: String? = "",                   // 썸네일 이미지 URL
    @ServerTimestamp
    val createdAt: Timestamp? = null,                        // 생성일 
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    val feedIds: List<String> = emptyList(), // 포함된 피드 아이디들
    val authorId: String = "",        // 작성 유저 ID

    val likeCount: Int = 0,   // 좋아요 수
    val viewCount: Int = 0 // 조회수
)