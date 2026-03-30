package com.tasty.android.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import kotlinx.coroutines.tasks.await

class MyPageStoreManager {
    private val firebaseDB = Firebase.firestore
    // 페이네이션 제한 한 번에 10개씩 load
    private val paginationLimit: Long = 10
    // 거리순의 경우 1회 데이터 상한선
    private val maxFetchLimit: Long = 200

    // 마이페이지의 내 피드 목록 조회
    suspend fun getMyFeeds(
        userId: String,
        limit: Long = paginationLimit,
        lastFeedId: String? = null
    ): Result<List<Feed>> {
        return try {
            var query = firebaseDB.collection("feeds")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            if (lastFeedId != null) {
                val lastSnapshot = firebaseDB
                    .collection("feeds")
                    .document(lastFeedId)
                    .get()
                    .await()
                query = query.startAfter(lastSnapshot)
            }
            Result.success(query.get().await().toObjects(Feed::class.java))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 마이페이지의 내 테이스티 리스트 목록 조회
    suspend fun getMyTastyLists(
        userId: String,
        limit: Long = paginationLimit,
        lastTastyListId: String? = null
    ): Result<List<TastyList>> {
        return try {
            var query = firebaseDB.collection("tastyLists")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            if (lastTastyListId != null) {
                val lastSnapshot = firebaseDB
                    .collection("tastyLists")
                    .document(lastTastyListId)
                    .get()
                    .await()
                query = query.startAfter(lastSnapshot)
            }
            Result.success(query.get().await().toObjects(TastyList::class.java))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
}