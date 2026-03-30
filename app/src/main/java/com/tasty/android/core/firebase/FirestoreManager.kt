package com.tasty.android.core.firebase


import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tasty.android.core.model.Follow
import com.tasty.android.core.model.User
import com.tasty.android.core.model.UserSummary
import com.tasty.android.feature.feed.FeedSortType
import com.tasty.android.feature.feed.model.Comment
import com.tasty.android.feature.feed.model.Feed
import com.tasty.android.feature.feed.model.FeedLike
import com.tasty.android.feature.mypage.tastylist.model.TastyList
import com.tasty.android.feature.mypage.tastylist.model.TastyListLike
import com.tasty.android.feature.tasty.TastySortType
import com.tasty.android.feature.tastymap.model.RestaurantInfo

import kotlinx.coroutines.tasks.await


class FirestoreManager {
    private val firebaseDB = Firebase.firestore
    // 페이네이션 제한 한 번에 10개씩 load
    private val paginationLimit: Long = 10
    // 거리순의 경우 1회 데이터 상한선
    private val maxFetchLimit: Long = 200

    /***
     * 회원가입/마이페이지/피드/테이스티리스트용
     * 유저 생성&조회&수정
     ***/
    // 유저 회원가입 정보 저장/유저 프로필 수정
    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firebaseDB.collection("users")
                .document(user.userId)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 단일 유저 전체 정보 조회
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = firebaseDB
                .collection("users")
                .document(userId)
                .get()
                .await()
            // 유저 전체 정보 User에 매핑
            val user = snapshot.toObject(User::class.java)
            Result.success(user)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 단일 유저 요약 정보 조회
    suspend fun getUserSummary(userId: String): Result<UserSummary?> {
        return try {
            // 유저 요약 정보 get
            val snapshot = firebaseDB
                .collection("users")
                .document(userId)
                .get()
                .await()
            // 유저 요약 정보 UserSummary에 매핑
            val userSummary = snapshot.toObject(UserSummary::class.java)
            Result.success(userSummary)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    /***
     * 팔로우 / 언팔로우 로직
     ***/

    // 팔로우/팔로잉 increment
    suspend fun followUser(
        follow: Follow
    ) : Result<Unit> {
        return try {
            val batch = firebaseDB.batch()

            val followRef = firebaseDB
                .collection("follows")
                .document()
            batch.set(followRef, follow.copy(followId = followRef.id))

            batch.update( // 팔로우 누른 유저 팔로잉 카운트 업데이트
                firebaseDB
                    .collection("users")
                    .document(follow.followerUserId),
                "followingCount",
                FieldValue.increment(1)
            )

            batch.update( // 해당 유저가 팔로우한 유저의 팔로워 카운트 업데이트
                firebaseDB
                    .collection("users")
                    .document(follow.followingUserId),
                "followerCount",
                FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 언팔로우/언팔로잉 decrement
    suspend fun unfollowUser(
        follow: Follow
    ) : Result<Unit> {
        return try {
            val batch = firebaseDB.batch()

            val followDoc = firebaseDB
                .collection("follows")
                .whereEqualTo("followerUserId", follow.followerUserId)
                .whereEqualTo("followingUserId", follow.followingUserId)
                .get().await()
                .documents.firstOrNull() ?: return Result.failure(Exception("팔로잉 관계 아님"))

            batch.delete(followDoc.reference)

            batch.update( // 언팔로우 누른 유저 팔로잉 카운트 업데이트
                firebaseDB
                    .collection("users")
                    .document(follow.followerUserId),
                "followingCount",
                FieldValue.increment(-1)
            )

            batch.update( // 해당 유저가 언팔로우한 유저의 팔로워 카운트 업데이트
                firebaseDB
                    .collection("users")
                    .document(follow.followingUserId),
                "followerCount",
                FieldValue.increment(-1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch(e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 팔로우 여부 확인
    suspend fun isFollowing(follow:Follow): Result<Boolean> {
        return try {
            val result = firebaseDB
                .collection("follows")
                .whereEqualTo("followerUserId", follow.followerUserId)
                .whereEqualTo("followingUserId", follow.followingUserId)
                .get()
                .await()
            Result.success(!result.isEmpty)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    /*** 피드 작성/조희 ***/

    // 피드 생성(저장) 흐름
    // 피드 게시 클릭
    // ->  피드 아이디 발급
    // -> 피드의 이미지 Uri 스토리지에 저장
    // -> 생성된 피드 아이디의 도큐먼트에 feed 객체 매핑 후 저장


    fun generateFeedId(): String = firebaseDB.collection("feeds").document().id

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
        limit: Long = paginationLimit, // 페이지네이션 상수 기본: 10개씩
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
                .collection("feeds").document(feedLike.feedId)
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

    /*** 마이페이지&&테이스티리스트 홈/테이스티리스트 CRUD(생성/조회/수정/삭제) ***/
    // 테이스티 리스트 아이디 생성
    fun generateTastyListId():String = firebaseDB.collection("tastyLists").document().id

    // 테이스티 리스트 저장/생성
    suspend fun createTastyList(tastyList: TastyList) : Result<Unit> {
        return try {
            firebaseDB
                .collection("tastyLists")
                .document(tastyList.tastyListId)
                .set(tastyList)
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 다수 테이스티 리스트 목록 조회
    suspend fun getTastyLists(
        sortType: TastySortType,
        limit: Long = paginationLimit,
        lastTastyListId: String? = null
    ): Result<List<TastyList>> {
        return try {
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
            Result.success(query.get().await().toObjects(TastyList::class.java))
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 단일 테이스티 리스트 조회
    suspend fun getTastyList(tastyListId: String) : Result<TastyList?> {
        return try {
            val snapshot = firebaseDB
                .collection("tastyLists")
                .document(tastyListId)
            val tastyList = snapshot
                .get()
                .await()
                .toObject(TastyList::class.java)
            Result.success(tastyList)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 테이스티 피드 목록 조회
    suspend fun getFeedsByIDs(feedIds:List<String>): Result<List<Feed>>{
        if (feedIds.isEmpty()) return Result.failure(Exception("포함된 피드들이 없습니다."))
        return try {
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
    ): Result<Unit> {
        return try {
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
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 테이스티 리스트 삭제
    suspend fun deleteTastyList(tastyListId: String): Result<Unit> {
        return try {
            firebaseDB.collection("tastyLists")
                .document(tastyListId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }

    // 조회수 증가
    suspend fun incrementTastyListViewCount(tastyListId: String): Result<Unit> {
        return try {
            firebaseDB.collection("tastyLists")
                .document(tastyListId)
                .update("viewCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 좋아요 추가
    suspend fun likeTastyList(tastyListLike: TastyListLike): Result<Unit> {
        return try {
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
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 좋아요 취소
    suspend fun unlikeTastyList(tastyListLike: TastyListLike): Result<Unit> {
        return try {
            val batch = firebaseDB.batch()

            val likeDoc = firebaseDB
                .collection("tastyLists")
                .document(tastyListLike.tastyListId)
                .collection("tastyListLikes")
                .whereEqualTo("userId", tastyListLike.userId)
                .get().await()
                .documents.firstOrNull() ?: return Result.failure(Exception("좋아요 상태 아님"))

            batch.delete(likeDoc.reference)
            batch.update(
                firebaseDB
                    .collection("tastyLists")
                    .document(tastyListLike.tastyListId),
                "likeCount", FieldValue.increment(-1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }
    // 좋아요 여부 확인
    suspend fun isTastyListLiked(tastyListLike: TastyListLike): Result<Boolean> {
        return try {
            val result = firebaseDB
                .collection("tastyLists")
                .document(tastyListLike.tastyListId)
                .collection("tastyListLikes")
                .whereEqualTo("userId", tastyListLike.userId)
                .get().await()
            Result.success(!result.isEmpty)
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
        return query.get().await().toObjects(Feed::class.java)
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
                    .toObjects(Feed::class.java)
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

