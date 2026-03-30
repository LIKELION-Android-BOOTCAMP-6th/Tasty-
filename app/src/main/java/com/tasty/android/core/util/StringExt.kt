package com.tasty.android.core.util

// User Handle 확장 함수
fun String.toHandle() = "@${this.substringBefore("@")}"