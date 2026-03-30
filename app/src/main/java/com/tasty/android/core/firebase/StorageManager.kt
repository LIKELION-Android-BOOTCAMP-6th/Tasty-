package com.tasty.android.core.firebase

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.collections.mapIndexed

class StorageManager {
    // Firebase Storage 인스턴스 생성
    private val storage = Firebase.storage
    private val storageRef = storage.reference

    // 유저 프로필 이미지 업로드 및 업데이트(동일한 경로인 경우에 덮어쓰기 됨다)/ 반환: 다운로드 Url
    suspend fun uploadProfileImage(profileImageUri: Uri): Result<String> {
        val userId = Firebase.auth.currentUser?.uid ?: return Result.failure(
            FirebaseAuthInvalidUserException("","")
        )
        return try {
            val userProfilePath = "userProfileImages/$userId/profileImage.jpg"
            val ref = storageRef.child(userProfilePath)

            ref.putFile(profileImageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: StorageException) {
            Result.failure(e)
        }
    }

    // 피드 이미지 업로드 / 반환: 다운로드 Url 리스트
    suspend fun uploadFeedImages(feedImageUris: List<Uri>, feedId: String): Result<List<String>> {
        return try {
            val feedImagesPath = "feedImages/$feedId"
            val downloadUrls = coroutineScope {
                feedImageUris.mapIndexed { idx, feedImageUri ->
                    async {
                        val ref = storageRef.child("$feedImagesPath/feedImage$idx.jpg")
                        ref.putFile(feedImageUri).await()
                        ref.downloadUrl.await().toString()
                    }
                }.awaitAll()
            }
            Result.success(downloadUrls)
        } catch(e: StorageException) {
            Result.failure(e)
        }
    }

    // 테이스티 리스트 썸네일 이미지 업로드 / 반환: 다운로드 Url 리스트
    suspend fun uploadThumbnailImages(thumbnailImageUri: Uri, tastyListId: String): Result<String> {
        return try {
            val thumbnailImagesPath = "thumbnailImages/$tastyListId"
            val ref = storageRef.child("$thumbnailImagesPath/thumbnailImage.jpg")

            ref.putFile(thumbnailImageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: StorageException) {
            Result.failure(e)
        }
    }
}