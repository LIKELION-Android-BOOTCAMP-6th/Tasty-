package com.tasty.android.core.model


// 유저 경량 모델
data class UserSummary (
    val userId: String,     // 유저 ID
    val userHandle: String, // 사용자 아이디(@형식)
    val nickname: String,   // 닉네임
    val profileImageUrl: String? // 프로필 이미지
)