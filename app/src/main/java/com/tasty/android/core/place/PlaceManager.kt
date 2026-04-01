package com.tasty.android.core.place

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList

data class RestaurantSearchItem(
    val restaurantId: String,
    val name: String
)
class PlaceManager(private val context: Context) {
    // Places 클라이언트
    private val placeClient = Places.createClient(context)

    // 식당 이름 검색
    suspend fun searchPlaces(query: String, placeTypes: List<String> = emptyList()): Result<List<RestaurantSearchItem>> = withContext(
        Dispatchers.IO) {
        if (query.isBlank()) return@withContext Result.success(emptyList())
        val placeTypes = placeTypes
        try {
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


    suspend fun getRestaurantDetails(restaurantId: String): Result<Place> = withContext(Dispatchers.IO) {
        try {
            val restaurantFields = listOf(
                Place.Field.ID, // 식당 id
                Place.Field.NAME, // 식당명
                Place.Field.ADDRESS, // 주소
                Place.Field.LAT_LNG, // 위경도
                Place.Field.OPENING_HOURS, // 오픈시간
                Place.Field.BUSINESS_STATUS, // 영업상태
                Place.Field.PHONE_NUMBER, // 전화번호
                Place.Field.PHOTO_METADATAS, // 식당 사진들
                Place.Field.TYPES, // 식당 타입
                Place.Field.ADDRESS_COMPONENTS
            )
            val req = FetchPlaceRequest.newInstance(restaurantId, restaurantFields)
            val res = placeClient.fetchPlace(req).await()


            Result.success(res.place)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getRestaurantBitmapImages(
        photoMetaDatas: List<PhotoMetadata>?, // API에서 받은 메타데이터 배열
        maxWidth: Int = 500,
        maxHeight: Int = 500,
        maxImageCount: Int = 5
    ): Result<List<Bitmap>> = withContext(Dispatchers.IO) {
        if (photoMetaDatas.isNullOrEmpty()) return@withContext Result.success(emptyList())


        try {
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
        radiusMeters: Double = 5000.0,
        maxResultCount: Int = 20
    ) : Result<List<Place>> = withContext(Dispatchers.IO) {
        try {
            val center = LatLng(
                latitude,
                longitude
            )

            val circle = CircularBounds.newInstance(center, radiusMeters)

            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.BUSINESS_STATUS
            )

            val req = SearchNearbyRequest
                .builder(circle, placeFields)
                .setIncludedTypes(listOf("restaurant", "cafe", "bakery"))
                .setMaxResultCount(maxResultCount)
                .build()
            val res = placeClient.searchNearby(req).await()

            Result.success(res.places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}