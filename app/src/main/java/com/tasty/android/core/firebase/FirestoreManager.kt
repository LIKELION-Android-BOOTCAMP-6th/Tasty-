package com.tasty.android.core.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException

import com.google.firebase.firestore.firestore
import com.tasty.android.core.model.User
import com.tasty.android.core.model.UserSummary
import com.tasty.android.feature.feed.model.Feed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    private val firebaseDB = Firebase.firestore

    /*** 유저 생성&조회&수정 ***/
    // 유저 회원가입 정보 저장/유저 프로필 수정
    suspend fun saveUser(user: User): Result<Unit>{
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
    // 유저 이메일 단일 조희
    suspend fun getUserEmail(userId: String): Result<String?> {
        return try {
            val snapshot = firebaseDB
                .collection("users")
                .document(userId)
                .get()
                .await()
            // 유저 이메일
            val userEmail = snapshot.getString("email")
            Result.success(userEmail)
        } catch (e: FirebaseException) {
            Result.failure(e)
        }
    }
    /*** 피드 작성/조희 ***/
    /*// 다수 피드 조회
    suspend fun getLatestFeeds(): Flow<List<Feed>> {

    }*/

    suspend fun saveFeed(feed: Feed): Result<Unit> {
        return try {
            // doc Reference 생성
            val docRef = firebaseDB.collection("feeds").document()
            val feedId = feed.copy(feedId = docRef.id)
            docRef.set(feedId).await()
            Log.d("jjam", feedId.toString())
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Result.failure(e)
        }
    }


}