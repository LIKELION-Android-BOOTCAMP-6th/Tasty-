package com.tasty.android.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.tasty.android.feature.tastymap.model.RestaurantInfo
import kotlinx.coroutines.tasks.await

class MapStoreManager {
    private val firebaseDB = Firebase.firestore

    // 식당 통계 정보
    suspend fun getRestaurantInfoFromIds(placeIds: List<String>): Result<Map<String, RestaurantInfo>> {
        if (placeIds.isEmpty()) return Result.success(emptyMap())

        return try {
            val dbRef = firebaseDB.collection("restaurantInfo")
            val infoMap = mutableMapOf<String, RestaurantInfo>()
            val chunks = placeIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = dbRef.whereIn("restaurantId", chunk).get().await()

                for (doc in snapshot.documents) {

                    val info = doc.toObject(RestaurantInfo::class.java) ?: continue

                    infoMap[info.restaurantId] = info
                }
            }
            Result.success(infoMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}