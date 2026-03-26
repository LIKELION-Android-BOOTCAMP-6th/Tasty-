package com.tasty.android.core.model


// 유저 경량 모델
data class UserSummary (
    val userId: String,     // 유저 ID
    val email: String,
    val nickname: String,   // 닉네임
    val profileImageUrl: String? // 프로필 이미지
)