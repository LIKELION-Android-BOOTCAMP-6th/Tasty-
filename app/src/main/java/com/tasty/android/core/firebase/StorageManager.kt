package com.tasty.android.core.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toFile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.collections.mapIndexed
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import java.io.File
import java.io.FileOutputStream
import kotlin.apply

class StorageManager(private val context: Context) {
    // Firebase Storage 인스턴스 생성
    private val storage = Firebase.storage
    private val storageRef = storage.reference

    // 유저 프로필 이미지 업로드 및 업데이트(동일한 경로인 경우에 덮어쓰기 됨다)/ 반환: 다운로드 Url
    suspend fun uploadProfileImage(profileImageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val userId = Firebase.auth.currentUser?.uid ?: return@withContext Result.failure(
            FirebaseAuthInvalidUserException("","")
        )
        try {
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
    suspend fun uploadFeedImages(feedImageUris: List<Uri>, feedId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val feedImagesPath = "feedImages/$feedId"

            val downloadUrls = coroutineScope {
                feedImageUris.mapIndexed { idx, feedImageUri ->
                    async {
                        val inputStream = context.contentResolver.openInputStream(feedImageUri)
                        val tempFile = File.createTempFile("temp_img_$idx", ".jpg", context.cacheDir)
                        val outputStream = FileOutputStream(tempFile)

                        inputStream.use { input ->
                            outputStream.use {output ->
                                input?.copyTo(output)
                            }
                        } ?: throw Exception("스트림 불가")
                        val compressedFile = Compressor.compress(context, tempFile) {
                            default(width = 1024, height = 1024, quality = 70)
                        }
                        val ref = storageRef.child("$feedImagesPath/feedImage$idx.jpg")
                        ref.putFile(Uri.fromFile(compressedFile)).await()

                        val url = ref.downloadUrl.await().toString()

                        tempFile.delete()
                        compressedFile.delete()

                        url
                    }
                }.awaitAll()
            }
            Result.success(downloadUrls)
        } catch(e: StorageException) {
            Result.failure(e)
        }
    }


    // 테이스티 리스트 썸네일 이미지 업로드 / 반환: 다운로드 Url
    suspend fun uploadThumbnailImages(thumbnailImageUri: Uri, tastyListId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val thumbnailImagesPath = "thumbnailImages/$tastyListId"
            val ref = storageRef.child("$thumbnailImagesPath/thumbnailImage.jpg")

            ref.putFile(thumbnailImageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: StorageException) {
            Result.failure(e)
        }
    }


    // 식당 이미지 비트맵 업로드 / 반환: 다운로드 Url 리스트
    suspend fun uploadRestaurantImages(bitmaps: List<Bitmap>, restaurantId:String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val restaurantImagesPath = "restaurantImages/$restaurantId"
            val downloadUrls = coroutineScope {
                bitmaps.mapIndexed { idx, bitmap ->
                    async {
                        val ref = storageRef.child("$restaurantImagesPath/restaurantImage_$idx.jpg")


                        val byteOutput = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteOutput)
                        val data = byteOutput.toByteArray()

                        ref.putBytes(data).await()
                        ref.downloadUrl.await().toString()
                    }
                }.awaitAll()
            }
            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}