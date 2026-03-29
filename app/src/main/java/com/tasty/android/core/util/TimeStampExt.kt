package com.tasty.android.core.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
// Timestamp 확장 함수
fun Timestamp.toFormattedDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(this.toDate())
}