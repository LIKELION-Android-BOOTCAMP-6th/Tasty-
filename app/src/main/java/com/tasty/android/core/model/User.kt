package com.tasty.android.core.model

data class User (
    // 사용자 기본 정보
    val userId: String,                       // 사용자 고유 ID
    val email: String = "",            // Added this to match your screenshot
    val profileImageUrl: String = "",          // 프로필 이미지 URL
    val nickname: String,                  // 닉네임
    val userHandle: String = "",                // 사용자 아이디 (@형식 가능)
    val bio: String = "",                       // 자기소개

    // 사용자 활동 수치
    val feedCount: Int = 0,                // 작성한 피드 수
    val followerCount: Int = 0,            // 팔로워 수
    val followingCount: Int = 0,           // 팔로잉 수

)