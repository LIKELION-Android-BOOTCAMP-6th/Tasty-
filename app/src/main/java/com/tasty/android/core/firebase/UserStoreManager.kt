package com.tasty.android.core.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.tasty.android.core.model.Follow
import com.tasty.android.core.model.User
import com.tasty.android.core.model.UserSummary
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserStoreManager {
    private val firebaseDB = Firebase.firestore
    /***
     * 유저 저장&조회&수정
     ***/
    // 유저 회원가입 정보 저장
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

    // 프로필 정보 업데이트 (닉네임, 소개글, 프로필 이미지 URL)
    suspend fun updateProfile(
        userId: String,
        nickname: String,
        bio: String,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return try {
            val userRef = firebaseDB.collection("users").document(userId)
            val updateData = mutableMapOf<String, Any>(
                "nickname" to nickname,
                "bio" to bio
            )
            
            // 이미지 URL이 있을 경우에만 포함
            profileImageUrl?.let {
                updateData["profileImageUrl"] = it
            }

            userRef.update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
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

    // 실시간 유저 정보 관찰
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val registration = firebaseDB.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(User::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
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
}