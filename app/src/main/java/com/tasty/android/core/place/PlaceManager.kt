package com.tasty.android.core.place

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Place.BusinessStatus
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.tasty.android.feature.tastymap.model.RestaurantData
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
        radiusMeters: Double = 5000.0,
        maxResultCount: Int = 20
    ) : Result<List<Place>>{
        return try {
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

    fun getPredictions(query: String, onResult: (List<AutocompletePrediction>) -> Unit) {
        if (query.isBlank()) {
            onResult(emptyList())
            return
        }

        // 검색 요청 설정 (한국 지역 제한 등)
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("KR")
            .build()

        placeClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                onResult(response.autocompletePredictions)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    fun searchRestaurants(
        location: LatLng,
        radiusInMeters: Double,
        onResult: (List<RestaurantData>) -> Unit
    ) {
        // 가져오고 싶은 필드 정의
        val placeFields =
            listOf(
                Field.ID,
                Field.NAME,
                Field.ADDRESS,
                Field.RATING,
                Field.LAT_LNG,
                Field.BUSINESS_STATUS,
                Field.PHOTO_METADATAS,
                Field.CURRENT_OPENING_HOURS,
            )

        // 검색 지역 설정 (중심점과 반경)
        val circle = CircularBounds.newInstance(location, radiusInMeters)

        // 요청 생성 (식당 타입으로 제한)
        val searchNearbyRequest = SearchNearbyRequest.builder(circle, placeFields)
            .setIncludedTypes(listOf("restaurant"))
            .setMaxResultCount(20)
            // 거리순으로 정렬
            .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
            .build()

        // 데이터 요청
        placeClient.searchNearby(searchNearbyRequest)
            .addOnSuccessListener { response ->
                val places = response.places
                for (place in places) {
                    Log.d("test", "식당 이름: ${place.name}, 평점: ${place.rating}")
                }

                // 결과 데이터를 콜백으로 전달
                val mappedList = response.places.map { place ->

                    val isCurrentlyOpen = place.isOpen ?: (place.currentOpeningHours != null)

                    RestaurantData(
                        name = place.name ?: "",
                        address = place.address ?: "",
                        rating = place.rating,
                        id = place.id ?: "",
                        latitude = place.latLng?.latitude ?: 0.0,
                        longitude = place.latLng?.longitude ?: 0.0,
                        businessStatus = when {
                            // 영업 상태 우선 확인
                            place.businessStatus == BusinessStatus.CLOSED_TEMPORARILY -> "임시 휴업"
                            place.businessStatus == BusinessStatus.CLOSED_PERMANENTLY -> "폐업"

                            // 현재 영업 여부 확인
                            isCurrentlyOpen == true -> "영업 중"

                            // 영업시간 정보가 아예 없는 경우
                            else -> "영업 종료"
                        },
                        photoMetadata = place.photoMetadatas?.take(5) ?: emptyList()
                    )
                }
                onResult(mappedList) // UI로 데이터 전달
            }
            .addOnFailureListener { exception ->
                Log.e("test", "에러 발생: ${exception.message}")
            }
    }

    fun fetchPhoto(photoMetadata: PhotoMetadata, onComplete: (Bitmap?) -> Unit) {
        val photoRequest =
            com.google.android.libraries.places.api.net.FetchPhotoRequest.builder(photoMetadata)
                .setMaxWidth(300) // 비용 절감을 위해 적절한 크기 지정
                .setMaxHeight(300) // 비용 절감을 위해 적절한 크기 지정
                .build()

        placeClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                onComplete(fetchPhotoResponse.bitmap)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun getPlaceLatLng(placeId: String, onResult: (LatLng?) -> Unit) {
        val placeFields = listOf(Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placeClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                onResult(response.place.latLng)
            }
            .addOnFailureListener { exception ->
                Log.e("PlacesManager", "Place not found: ${exception.message}")
                onResult(null)
            }
    }
}