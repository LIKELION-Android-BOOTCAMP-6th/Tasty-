package com.tasty.android.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import com.tasty.android.feature.mypage.tastylist.model.TastyListLike
import com.tasty.android.feature.tasty.TastySortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class TastyUpdateEvent {
    data class TastyListCreated(val authorId: String) : TastyUpdateEvent()
    data class TastyListUpdated(val tastyListId: String) : TastyUpdateEvent()
    data class TastyListDeleted(val tastyListId: String) : TastyUpdateEvent()
    data class ViewCountChanged(val tastyListId: String, val newCount: Int) : TastyUpdateEvent()
    data class TastyListLiked(val tastyListId: String, val userId: String) : TastyUpdateEvent()
    data class TastyListUnliked(val tastyListId: String, val userId: String) : TastyUpdateEvent()
}

class TastyStoreManager {

    private val firebaseDB = Firebase.firestore

    // 페이네이션 제한 한 번에 10개씩 load
    private val paginationLimit: Long = 10

    // 거리순의 경우 1회 데이터 상한선
    private val maxFetchLimit: Long = 200

    private val _tastyUpdateEvents = MutableSharedFlow<TastyUpdateEvent>(extraBufferCapacity = 1)
    val tastyUpdateEvents: SharedFlow<TastyUpdateEvent> = _tastyUpdateEvents.asSharedFlow()

    suspend fun notifyTastyUpdated(event: TastyUpdateEvent) {
        _tastyUpdateEvents.emit(event)
    }

    /*** 마이페이지&&테이스티리스트 홈/테이스티리스트 CRUD(생성/조회/수정/삭제) ***/
    // 테이스티 리스트 아이디 생성
    fun generateTastyListId(): String = firebaseDB.collection("tastyLists").document().id

    // 테이스티 리스트 저장/생성
    suspend fun createTastyList(tastyList: TastyList): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseDB
                .collection("tastyLists")
                .document(tastyList.tastyListId)
                .set(tastyList)
                .await()

            notifyTastyUpdated(TastyUpdateEvent.TastyListCreated(tastyList.authorId))

            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 다수 테이스티 리스트 목록 조회(단발성)
    suspend fun getTastyLists(
        sortType: TastySortType,
        limit: Long = paginationLimit,
        lastTastyListId: String? = null
    ): Result<List<TastyList>> = withContext(Dispatchers.IO) {
        try {
            val orderField = when (sortType) {
                TastySortType.LATEST -> "createdAt"
                TastySortType.VIEW_COUNT -> "viewCount"
            }
            var query = firebaseDB.collection("tastyLists")
                .orderBy(orderField, Query.Direction.DESCENDING)
                .limit(limit)
            if (lastTastyListId != null) {
                val lastSnapshot = firebaseDB.collection("tastyLists")
                    .document(lastTastyListId).get().await()
                query = query.startAfter(lastSnapshot)
            }
            val snapshots = query.get().await()
            val list = snapshots.documents.mapNotNull { doc ->
                doc.toObject(TastyList::class.java)?.copy(tastyListId = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 테이스티 리스트 실시간 조회 (DB 상시 동기화용)
    fun getTastyListsFlow(
        sortType: TastySortType,
        limit: Long = paginationLimit
    ): Flow<List<TastyList>> = callbackFlow {
        val orderField = when (sortType) {
            TastySortType.LATEST -> "createdAt"
            TastySortType.VIEW_COUNT -> "viewCount"
        }

        val query = firebaseDB.collection("tastyLists")
            .orderBy(orderField, Query.Direction.DESCENDING)
            .limit(limit)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TastyList::class.java)?.copy(tastyListId = doc.id)
                }
                trySend(list)
            }
        }

        awaitClose { subscription.remove() }
    }

    // 단일 테이스티 리스트 조회
    suspend fun getTastyList(tastyListId: String): Result<TastyList?> =
        withContext(Dispatchers.IO) {
            if (tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
            try {
                val doc = firebaseDB
                    .collection("tastyLists")
                    .document(tastyListId)
                    .get()
                    .await()
                val tastyList = doc.toObject(TastyList::class.java)?.copy(tastyListId = doc.id)
                Result.success(tastyList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // 테이스티 피드 목록 조회
    suspend fun getFeedsByIDs(feedIds: List<String>): Result<List<Feed>> =
        withContext(Dispatchers.IO) {
            if (feedIds.isEmpty()) return@withContext Result.failure(Exception("포함된 피드들이 없습니다."))
            try {
                val feeds = feedIds.chunked(30).flatMap { chunk ->
                    firebaseDB.collection("feeds")
                        .whereIn("feedId", chunk)
                        .get().await()
                        .toObjects(Feed::class.java)
                }
                Result.success(feeds)
            } catch (e: FirebaseFirestoreException) {
                Result.failure(e)
            }
        }

    // 테이스티 리스트 수정
    suspend fun updateTastyList(
        tastyListId: String,
        title: String? = null,
        thumbnailImageUrl: String? = null,
        feedIds: List<String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
        try {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.let { updates["title"] = it }
            thumbnailImageUrl?.let { updates["thumbnailImageUrl"] = it }
            feedIds?.let { updates["feedIds"] = it }
            firebaseDB.collection("tastyLists")
                .document(tastyListId)
                .update(updates)
                .await()

            // 실시간 이벤트 전파
            notifyTastyUpdated(TastyUpdateEvent.TastyListUpdated(tastyListId))

            Result.success(Unit)
        } catch (e: FirebaseException) {
            Result.failure(e)
        }
    }




    // 테이스티 리스트 삭제
    suspend fun deleteTastyList(tastyListId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
        try {
            firebaseDB.collection("tastyLists")
                .document(tastyListId)
                .delete()
                .await()
            
            // 실시간 이벤트 전파
            notifyTastyUpdated(TastyUpdateEvent.TastyListDeleted(tastyListId))
            
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


    // 조회수 증가
    suspend fun incrementTastyListViewCount(tastyListId: String): Result<Int> = withContext(Dispatchers.IO) {
        if (tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
        try {
            val docRef = firebaseDB.collection("tastyLists").document(tastyListId)
            
            val newCount = firebaseDB.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("viewCount") ?: 0L
                val nextCount = currentCount + 1
                transaction.update(docRef, "viewCount", nextCount)
                nextCount.toInt()
            }.await()

            notifyTastyUpdated(TastyUpdateEvent.ViewCountChanged(tastyListId, newCount))
            
            Result.success(newCount)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 좋아요 추가
    suspend fun likeTastyList(tastyListLike: TastyListLike): Result<Unit> = withContext(Dispatchers.IO) {
        if (tastyListLike.tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
        try {
            val batch = firebaseDB.batch()
            val likeRef = firebaseDB
                .collection("tastyLists")
                .document(tastyListLike.tastyListId)
                .collection("tastyListLikes")
                .document()
            batch.set(
                likeRef,
                tastyListLike.copy(likeId = likeRef.id)
            )
            batch.update(
                firebaseDB
                    .collection("tastyLists")
                    .document(tastyListLike.tastyListId),
                "likeCount", FieldValue.increment(1)
            )
            batch.commit().await()
            
            // 실시간 이벤트 전파
            notifyTastyUpdated(TastyUpdateEvent.TastyListLiked(tastyListLike.tastyListId, tastyListLike.userId))
            
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 좋아요 취소
    suspend fun unlikeTastyList(tastyListLike: TastyListLike): Result<Unit> = withContext(Dispatchers.IO) {
        if (tastyListLike.tastyListId.isBlank()) return@withContext Result.failure(Exception("Invalid tastyListId"))
        try {
            val batch = firebaseDB.batch()

            val likeDoc = firebaseDB
                .collection("tastyLists")
                .document(tastyListLike.tastyListId)
                .collection("tastyListLikes")
                .whereEqualTo("userId", tastyListLike.userId)
                .get().await()
                .documents.firstOrNull() ?: return@withContext Result.failure(Exception("좋아요 상태 아님"))

            batch.delete(likeDoc.reference)
            batch.update(
                firebaseDB
                    .collection("tastyLists")
                    .document(tastyListLike.tastyListId),
                "likeCount", FieldValue.increment(-1)
            )
            batch.commit().await()

            // 실시간 이벤트 전파
            notifyTastyUpdated(TastyUpdateEvent.TastyListUnliked(tastyListLike.tastyListId, tastyListLike.userId))

            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 좋아요 여부 확인
    suspend fun isTastyListLiked(tastyListLike: TastyListLike): Result<Boolean> = withContext(Dispatchers.IO) {
        if (tastyListLike.tastyListId.isBlank()) return@withContext Result.success(false)
        try {
            val result = firebaseDB
                .collection("tastyLists")
                .document(tastyListLike.tastyListId)
                .collection("tastyListLikes")
                .whereEqualTo("userId", tastyListLike.userId)
                .get().await()
            Result.success(!result.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 팔로잉 유저들의 테이스티 리스트 조회
    suspend fun getTastyListsByUserIds(userIds: List<String>, limit: Long = 30): Result<List<TastyList>> = withContext(Dispatchers.IO) {
        if (userIds.isEmpty()) return@withContext Result.success(emptyList())
        try {
            val query = firebaseDB.collection("tastyLists")
                .whereIn("authorId", userIds.take(30))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            
            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(TastyList::class.java)?.copy(tastyListId = doc.id)
            }
            Result.success(list)
        } catch (e: Exception) {
            android.util.Log.e("TastyStoreManager", "Firestore query error: ${e.message}", e)
            Result.failure(e)
        }
    }
}