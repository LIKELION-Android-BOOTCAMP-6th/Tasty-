package com.tasty.android.core

import android.content.Context
import com.tasty.android.core.firebase.AuthManager
import com.tasty.android.core.firebase.FirestoreManager
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.location.LocationManager
import com.tasty.android.core.place.PlaceManager

// 여기에는 Firebase SDK, Google Maps SDK, Google Places SDK 등의 Manager를 선언해주세요.
class AppContainer(private val context: Context) {
    // Service (Firebase 외부 데이터 소스)
    val authManager = AuthManager()
    val firestoreManager = FirestoreManager()
    val storageManager = StorageManager()
    val locationManager = LocationManager(context = context)
    val placeManager = PlaceManager(context = context)
}