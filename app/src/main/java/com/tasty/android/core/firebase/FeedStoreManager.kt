package com.tasty.android.core.firebase

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tasty.android.feature.feed.FeedSortType
import com.tasty.android.feature.feed.model.Comment
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.feed.model.FeedLike
import com.tasty.android.feature.tastymap.model.RestaurantInfo
import kotlinx.coroutines.tasks.await

class FeedStoreManager {

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
    suspend fun saveFeed(feed: Feed): Result<Unit> {
        return try {
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

            firebaseDB.runTransaction { transaction ->
                val snapshot = transaction.get(restaurantRef)

                if (snapshot.exists()) {
                    val currentCount = snapshot.getLong("feedCount") ?: 0L
                    val currentAvg = snapshot.getDouble("ratingAvg") ?: 0.0

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
                    transaction.set(feedRef, currentFeed)
                }

            }.await()

            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 피드 상세 조회(피드 상세)
    suspend fun getFeedDetail(feedId: String): Result<Feed?> {
        return try {
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

    // 피드 다수 조회
    suspend fun getFeeds(
        sortType: FeedSortType = FeedSortType.LATEST,
        limit: Long = paginationLimit, // 페이지네이션 상수 기본: 20개씩
        lastFeedId: String? = null, // 마지막 피드 기준(마지막 피드 기준으로 다음 피드들을 불러오면 됨다)
        userLat: Double? = null,
        userLon: Double? = null,
        radiusKm: Double = 10.0,
        maxFetch: Long = maxFetchLimit
    ): Result<List<Feed>> {
        return try {
            val feeds = when (sortType) {
                FeedSortType.LATEST -> { // 최신순
                    fetchLatestFeeds(limit,lastFeedId)
                }
                FeedSortType.DISTANCE -> { // 거리순
                    if (userLat == null || userLon == null) return Result.failure(Exception("유저 위/경도 필요"))
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
    suspend fun likeFeed(feedLike: FeedLike) : Result<Unit> {
        return try {
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
    suspend fun unlikeFeed(feedLike: FeedLike) : Result<Unit> {
        return try {
            val batch = firebaseDB.batch()
            val likeDoc = firebaseDB
                .collection("feeds")
                .document(feedLike.feedId)
                .collection("feedLikes")
                .whereEqualTo("userId", feedLike.userId)
                .get()
                .await()
                .documents.firstOrNull() ?: return Result.failure(Exception("좋아요 상태 아님"))

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
    suspend fun isLiked(feedLike: FeedLike) : Result<Boolean> {
        return try {
            if (feedLike.feedId.isBlank()) return Result.success(false)
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
    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            val batch = firebaseDB.batch()

            val commentRef = firebaseDB
                .collection("feeds")
                .document(comment.feedId)
                .collection("comments")
                .document()

            batch.set(
                commentRef,
                comment.copy(commentId = commentRef.id)
            )

            batch.update(
                firebaseDB
                    .collection("feeds")
                    .document(comment.feedId),
                "commentCount", FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 댓글 목록 조회
    suspend fun getComments(
        feedId: String,
        limit: Long = paginationLimit,
        lastCommentId: String? = null
    ): Result<List<Comment>> {
        return try {
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
            Result.success(query.get().await().toObjects(Comment::class.java))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    private suspend fun fetchLatestFeeds(
        limit: Long = paginationLimit,
        lastFeedId: String? = null,
    ): List<Feed> {
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
        return query.get().await().documents.mapNotNull { doc ->
            doc.toObject(Feed::class.java)?.copy(feedId = doc.id)
        }
    }

    private suspend fun fetchFeedsByDistance(
        userLat: Double? = null,
        userLon: Double? = null,
        radiusKm: Double = 10.0,
        maxFetch: Long = maxFetchLimit
    ): List<Feed> {
        val center = GeoLocation(userLat!!, userLon!!)
        val radiusInMeters = radiusKm * 1000

        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)

        return bounds
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