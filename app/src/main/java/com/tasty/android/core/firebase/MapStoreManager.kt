package com.tasty.android.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.tastymap.model.RestaurantInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    // 식당 ID로 피드 목록 조회
    suspend fun getFeedsByRestaurantId(
        restaurantId: String,
    ): Result<List<Feed>> = withContext(Dispatchers.IO) {
        try {
            val query = firebaseDB.collection("feeds")
                .whereEqualTo("restaurantId", restaurantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10) // 최신순 상위 10개만


            val snapshot = query.get().await()
            val feeds = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Feed::class.java)?.copy(feedId = doc.id)
            }
            Result.success(feeds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 레스토랑 통계정보 겟
    suspend fun getRestaurantInfoByRestaurantId(
        restaurantId: String,
    ): Result<RestaurantInfo?> = withContext(Dispatchers.IO) {
        try {
            val query = firebaseDB.collection("restaurantInfo")
                .document(restaurantId)
            val restaurantInfo = query.get().await()
            Result.success(restaurantInfo.toObject(RestaurantInfo::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}