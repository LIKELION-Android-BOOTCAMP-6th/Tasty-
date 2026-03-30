package com.tasty.android.core.place

import android.content.Context
import android.graphics.Bitmap
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.collections.emptyList

data class RestaurantSearchItem(
    val restaurantId: String,
    val name: String
)
class PlaceManager(private val context: Context) {
    // Places 클라이언트
    private val placeClient = Places.createClient(context)

    // 식당 이름 검색
    suspend fun searchPlaces(query: String, placeTypes: List<String> = emptyList()): Result<List<RestaurantSearchItem>> {
        if (query.isBlank()) return Result.success(emptyList())
        val placeTypes = placeTypes
        return try {
            val req = FindAutocompletePredictionsRequest
                .builder()
                .setQuery(query)
                .setCountries("KR")
                .setTypesFilter(
                    placeTypes // ex) listOf("restaurant", "locality", "sublocality")
                )
                .build()
            val res = placeClient
                .findAutocompletePredictions(req)
                .await()
            val restaurants = res.autocompletePredictions.map {
                RestaurantSearchItem(
                    restaurantId = it.placeId,
                    name = it.getPrimaryText(null).toString()
                )
            }
            Result.success(restaurants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRestaurantDetails(restaurantId: String): Result<Place> {
        return try {
            val restaurantFields = listOf(
                Place.Field.ID, // 식당 id
                Place.Field.NAME, // 식당명
                Place.Field.ADDRESS, // 주소
                Place.Field.OPENING_HOURS, // 오픈시간
                Place.Field.BUSINESS_STATUS, // 영업상태
                Place.Field.PHONE_NUMBER, // 전화번호
                Place.Field.PHOTO_METADATAS // 식당 사진들
            )
            val req = FetchPlaceRequest.newInstance(restaurantId, restaurantFields)
            val res = placeClient.fetchPlace(req).await()

            Result.success(res.place)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRestaurantBitmapImage(
        photoMetaDatas: List<PhotoMetadata>?, // API에서 받은 메타데이터 배열
        maxWidth: Int = 500,
        maxHeight: Int = 500,
        maxImageCount: Int = 5
    ): Result<List<Bitmap>> {
        if (photoMetaDatas.isNullOrEmpty()) return Result.success(emptyList())


        return try {
            val metaDatas = photoMetaDatas.take(maxImageCount)

            val bitmaps = coroutineScope {
                metaDatas.map { metaData ->
                    async {
                        val photoReq = FetchPhotoRequest.builder(metaData)
                            .setMaxWidth(maxWidth)
                            .setMaxHeight(maxHeight)
                            .build()

                        placeClient
                            .fetchPhoto(photoReq)
                            .await()
                            .bitmap
                    }

                }.awaitAll()
            }
            Result.success(bitmaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        radiusMeters: Double = 5000.0
    ): Result<List<Place>> {

    }
}