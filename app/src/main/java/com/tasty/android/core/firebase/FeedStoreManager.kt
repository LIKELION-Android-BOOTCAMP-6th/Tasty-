package com.tasty.android.core.firebase

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tasty.android.feature.feed.FeedSortType
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.feed.model.FeedComment
import com.tasty.android.feature.feed.model.FeedLike
import com.tasty.android.feature.tastymap.model.RestaurantInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


// 동기화 모댈
sealed class FeedUpdateEvent {
    data class CommentCountChanged(val feedId: String, val newCount: Int) : FeedUpdateEvent()
    data class LikeStatusChanged(val feedId: String, val isLiked: Boolean, val likeCount: Int) : FeedUpdateEvent()
    data class FeedCreated(val authorId: String) : FeedUpdateEvent()
    data class AuthorInfoChanged(val authorId: String, val newNickname: String, val newProfileUrl: String?) : FeedUpdateEvent()
}

class FeedStoreManager {

    private val _feedUpdateEvents = MutableSharedFlow<FeedUpdateEvent>(extraBufferCapacity = 1)
    val feedUpdateEvents = _feedUpdateEvents.asSharedFlow()

    suspend fun notifyFeedUpdated(event: FeedUpdateEvent) {
        _feedUpdateEvents.emit(event)
    }

    private val firebaseDB = Firebase.firestore
    // 페이네이션 제한 한 번에 10개씩 load
    private val paginationLimit: Long = 20
    // 거리순의 경우 1회 데이터 상한선
    private val maxFetchLimit: Long = 200

    /*** 피드 작성/조희 ***/

    // 피드 생성(저장) 흐름
    // 피드 게시 클릭
    // ->  피드 아이디 발급
    // -> 피드의 이미지 Uri 스토리지에 저장
    // -> 생성된 피드 아이디의 도큐먼트에 feed 객체 매핑 후 저장

    fun generateFeedId(): Result<String> = Result.success(firebaseDB.collection("feeds").document().id)

    // 피드 저장(작성)
    suspend fun saveFeed(feed: Feed): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val geohash = GeoFireUtils.getGeoHashForLocation( // hash값 발급
                GeoLocation(
                    feed.addressInfo.latitude,
                    feed.addressInfo.longitude
                )
            )
            val currentFeed = feed.copy(geohash = geohash)

            val restaurantRef = firebaseDB
                .collection("restaurantInfo")
                .document(feed.restaurantId)

            val feedRef = firebaseDB.collection("feeds").document(feed.feedId)
            val userRef = firebaseDB.collection("users").document(feed.authorId)

            firebaseDB.runTransaction { transaction ->
                val restaurantSnapshot = transaction.get(restaurantRef)

                // 1. 식당 정보 업데이트
                if (restaurantSnapshot.exists()) {
                    val currentCount = restaurantSnapshot.getLong("feedCount") ?: 0L
                    val currentAvg = restaurantSnapshot.getDouble("ratingAvg") ?: 0.0

                    val newCount = currentCount + 1
                    val newAvg = ((currentAvg * currentCount) + currentFeed.rating) / newCount

                    transaction.update(restaurantRef, "feedCount", newCount)
                    transaction.update(restaurantRef, "ratingAvg", newAvg)
                } else {
                    val restaurantInfo = RestaurantInfo(
                        restaurantId = currentFeed.restaurantId,
                        feedCount = 1,
                        ratingAvg = currentFeed.rating.toDouble()
                    )
                    transaction.set(restaurantRef, restaurantInfo)
                }

                // 2. 유저 프로필 피드 카운트 업데이트
                transaction.update(userRef, "feedCount", FieldValue.increment(1))

                // 3. 피드 저장
                transaction.set(feedRef, currentFeed)

            }.await()

            // 4. 피드 생성 이벤트 전파 (마이페이지 등 동기화용)
            notifyFeedUpdated(FeedUpdateEvent.FeedCreated(feed.authorId))

            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


    // 작성한 모든 피드 게시물의 작성자 정보를 일괄 업데이트
    suspend fun syncAuthorInfoInFeeds(
        userId: String,
        nickname: String,
        profileImageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val feedsQuery = firebaseDB.collection("feeds")
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            if (feedsQuery.isEmpty) return@withContext Result.success(Unit)

            val batch = firebaseDB.batch()
            feedsQuery.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    "authorNickname", nickname,
                    "authorProfileUrl", profileImageUrl
                )
            }
            batch.commit().await()

            // 실시간 이벤트 전파
            notifyFeedUpdated(FeedUpdateEvent.AuthorInfoChanged(userId, nickname, profileImageUrl))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // 피드 상세 조회(피드 상세)
    suspend fun getFeedDetail(feedId: String): Result<Feed?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firebaseDB
                .collection("feeds")
                .document(feedId)
                .get()
                .await()
            val feed = snapshot.toObject(Feed::class.java)
            Result.success(feed)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 식당 ID로 피드 목록 조회
    suspend fun getFeedsByRestaurantId(
        restaurantId: String,
        limit: Long = paginationLimit,
        lastFeedId: String? = null
    ): Result<List<Feed>> = withContext(Dispatchers.IO) {
        try {
            var query = firebaseDB.collection("feeds")
                .whereEqualTo("restaurantId", restaurantId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastFeedId != null) {
                val lastSnapshot = firebaseDB.collection("feeds")
                    .document(lastFeedId)
                    .get()
                    .await()
                query = query.startAfter(lastSnapshot)
            }

            val snapshot = query.get().await()
            val feeds = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Feed::class.java)?.copy(feedId = doc.id)
            }
            Result.success(feeds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // 피드 다수 조회
    suspend fun getFeeds(
        sortType: FeedSortType = FeedSortType.LATEST,
        limit: Long = paginationLimit,
        lastFeedId: String? = null,
        userLat: Double? = null,
        userLon: Double? = null,
        radiusKm: Double = 10.0,
        maxFetch: Long = maxFetchLimit
    ): Result<List<Feed>> = withContext(Dispatchers.IO) {
        try {
            val feeds = when (sortType) {
                FeedSortType.LATEST -> {
                    fetchLatestFeeds(limit, lastFeedId)
                }
                FeedSortType.DISTANCE -> {
                    if (userLat == null || userLon == null) return@withContext Result.failure(Exception("유저 위/경도 필요"))
                    fetchFeedsByDistance(
                        userLat,
                        userLon,
                        radiusKm,
                        maxFetch
                    )
                }
            }
            Result.success(feeds)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    /*** 피드 좋아요/댓글 ***/

    // 좋아요 추가
    suspend fun likeFeed(feedLike: FeedLike) : Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firebaseDB.batch()

            val likeRef = firebaseDB
                .collection("feeds").document(feedLike.feedId)
                .collection("feedLikes").document()
            batch.set(
                likeRef,
                feedLike.copy(likeId = likeRef.id)
            )
            batch.update(
                firebaseDB
                    .collection("feeds")
                    .document(feedLike.feedId),
                "likeCount", FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


    // 좋아요 취소
    suspend fun unlikeFeed(feedLike: FeedLike) : Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val batch = firebaseDB.batch()
            val likeDoc = firebaseDB
                .collection("feeds")
                .document(feedLike.feedId)
                .collection("feedLikes")
                .whereEqualTo("userId", feedLike.userId)
                .get()
                .await()
                .documents.firstOrNull() ?: return@withContext Result.failure(Exception("좋아요 상태 아님"))

            batch.delete(likeDoc.reference)
            batch.update(
                firebaseDB
                    .collection("feeds")
                    .document(feedLike.feedId),
                "likeCount", FieldValue.increment(-1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


    // 좋아요 여부 확인
    suspend fun isLiked(feedLike: FeedLike) : Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (feedLike.feedId.isBlank()) return@withContext Result.success(false)
            val result = firebaseDB
                .collection("feeds")
                .document(feedLike.feedId)
                .collection("feedLikes")
                .whereEqualTo("userId", feedLike.userId)
                .get()
                .await()
            Result.success(!result.isEmpty)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 댓글 추가
    suspend fun addComment(comment: FeedComment): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val batch = firebaseDB.batch()

            val commentRef = firebaseDB
                .collection("feeds")
                .document(comment.feedId)
                .collection("comments")
                .document()

            val generatedId = commentRef.id
            batch.set(
                commentRef,
                comment.copy(commentId = generatedId)
            )

            batch.update(
                firebaseDB
                    .collection("feeds")
                    .document(comment.feedId),
                "commentCount", FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(generatedId)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }



    // 댓글 목록 조회
    suspend fun getComments(
        feedId: String,
        limit: Long = paginationLimit,
        lastCommentId: String? = null
    ): Result<List<FeedComment>> = withContext(Dispatchers.IO) {
        try {
            var query = firebaseDB
                .collection("feeds").document(feedId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            if (lastCommentId != null) {
                val lastSnapshot = firebaseDB
                    .collection("feeds").document(feedId)
                    .collection("comments").document(lastCommentId)
                    .get().await()
                query = query.startAfter(lastSnapshot)
            }
            val snapshot = query.get().await()
            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FeedComment::class.java)?.copy(commentId = doc.id)
            }
            Result.success(comments)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


    private suspend fun fetchLatestFeeds(
        limit: Long = paginationLimit,
        lastFeedId: String? = null,
    ): List<Feed> = withContext(Dispatchers.IO) {
        var query = firebaseDB
            .collection("feeds")
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
        return@withContext query.get().await().documents.mapNotNull { doc ->
            doc.toObject(Feed::class.java)?.copy(feedId = doc.id)
        }
    }

    private suspend fun fetchFeedsByDistance(
        userLat: Double? = null,
        userLon: Double? = null,
        radiusKm: Double = 10.0,
        maxFetch: Long = maxFetchLimit
    ): List<Feed> = withContext(Dispatchers.IO) {
        val center = GeoLocation(userLat!!, userLon!!)
        val radiusInMeters = radiusKm * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)

        return@withContext bounds
            .flatMap { bound ->
                firebaseDB
                    .collection("feeds")
                    .orderBy("geohash")
                    .startAt(bound.startHash)
                    .endAt(bound.endHash)
                    .get()
                    .await()
                    .documents.mapNotNull { doc ->
                        doc.toObject(Feed::class.java)?.copy(feedId = doc.id)
                    }
            }
            .distinctBy { it.feedId }
            .map { feed ->
                feed to GeoFireUtils.getDistanceBetween(
                    GeoLocation(feed.addressInfo.latitude, feed.addressInfo.longitude),
                    center
                )
            }
            .filter {(_, distance) ->
                distance <= radiusInMeters // 반경 내 거리만 매핑
            }
            .sortedBy { (_, distance) ->  // 거리순으로 재정렬
                distance
            }
            .map {(feed, _) -> // 정렬된 피드 반환
                feed
            }
            .take(maxFetch.toInt()) // 상한선만큼 take
    }
}