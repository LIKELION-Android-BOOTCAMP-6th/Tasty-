package com.tasty.android.core.place

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.Place.BusinessStatus
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.tasty.android.feature.tastymap.model.RestaurantData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.emptyList
import kotlin.math.cos

data class RestaurantSearchItem(
    val restaurantId: String,
    val name: String
)

class PlaceManager(private val context: Context) {
    // Places 클라이언트
    private val placeClient = Places.createClient(context)

    // 식당 이름 검색
    suspend fun searchPlaces(
        query: String,
        placeTypes: List<String> = emptyList()
    ): Result<List<RestaurantSearchItem>> = withContext(
        Dispatchers.IO
    ) {
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


    suspend fun getRestaurantDetails(restaurantId: String): Result<Place> =
        withContext(Dispatchers.IO) {
            try {
                val restaurantFields = listOf(
                    Place.Field.ID, // 식당 id
                    Place.Field.NAME, // 식당명
                    Place.Field.ADDRESS, // 주소
                    Place.Field.LAT_LNG, // 위경도
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
    ): Result<List<Place>> = withContext(Dispatchers.IO) {
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

    suspend fun searchRestaurantsByGrid(
        center: LatLng,
        totalRangeKm: Double = 0.5,   // 전체 탐색 범위를 0.5km로 제한
        gridStepMeter: Double = 150.0  // 격자 간격을 150m로 설정
    ): List<RestaurantData> = withContext(Dispatchers.IO) {

        val accumulatedMap = mutableMapOf<String, RestaurantData>()
        // 격자 간격이 150m일 때 반경을 180m로 잡으면 인접 격자와 충분히 중첩
        val searchRadius = 180.0

        val meterPerLat = 110940.0
        val meterPerLon = 88800.0 * cos(Math.toRadians(center.latitude))

        // 최대 단계 계산
        val maxStep = (totalRangeKm * 1000 / gridStepMeter).toInt() / 2

        // 수집 요소
        val placeFields = listOf(
            Field.ID,
            Field.NAME,
            Field.ADDRESS,
            Field.LAT_LNG,
            Field.BUSINESS_STATUS,
            Field.PHOTO_METADATAS,
            Field.OPENING_HOURS,
            Field.CURRENT_OPENING_HOURS,
            Field.UTC_OFFSET,
            Field.PHONE_NUMBER
        )

        // 나선형 탐색 알고리즘
        // (0,0) 중심점에서 시작하여 외곽으로 확장
        var x = 0
        var y = 0
        var dx = 0
        var dy = -1

        // 총 탐색해야 할 포인트 개수
        val side = maxStep * 2 + 1
        val maxPoints = side * side

        for (i in 0 until maxPoints) {
            // 현재 (x, y) 좌표 계산
            val targetLat = center.latitude + (x * gridStepMeter / meterPerLat)
            val targetLon = center.longitude + (y * gridStepMeter / meterPerLon)
            val gridPoint = LatLng(targetLat, targetLon)

            // API 호출
            val circle = CircularBounds.newInstance(gridPoint, searchRadius)
            val request = SearchNearbyRequest.builder(circle, placeFields)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .setIncludedTypes(listOf("restaurant", "cafe", "bakery", "bar"))
                .build()

            try {
                // 필터링 및 데이터 파싱
                val response = placeClient.searchNearby(request).await()
                response.places.forEach { place ->
                    if (place.businessStatus != Place.BusinessStatus.CLOSED_PERMANENTLY) {
                        // 영업 정보 구분
                        val statusText = when {
                            place.businessStatus == BusinessStatus.CLOSED_TEMPORARILY -> "임시 휴업"
                            place.isOpen == true -> "영업 중"
                            place.isOpen == false -> "영업 종료"
                            else -> "영업 정보 없음"
                        }
                        val restaurant = RestaurantData(
                            id = place.id ?: "",
                            name = place.name ?: "",
                            address = place.address ?: "",
                            latitude = place.latLng?.latitude ?: 0.0,
                            longitude = place.latLng?.longitude ?: 0.0,
                            businessStatus = statusText,
                            photoMetadata = place.photoMetadatas?.take(5) ?: emptyList(),
                            phoneNumber = place.phoneNumber,
                            openingHours = place.openingHours?.weekdayText
                        )
                        accumulatedMap[restaurant.id] = restaurant
                    }
                }
                Log.d("PlaceManager", "좌표 ($x, $y) 탐색 완료. 현재 총: ${accumulatedMap.size}개")
                delay(150L)
            } catch (e: Exception) {
                Log.e("PlaceManager", "에러: ${e.message}")
            }

            // 나선형 이동 로직: 다음 (x, y) 결정
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
                val temp = dx
                dx = -dy
                dy = temp
            }
            x += dx
            y += dy
        }

        accumulatedMap.values.toList()
    }

    fun fetchPhoto(photoMetadata: PhotoMetadata, onComplete: (Bitmap?) -> Unit) {
        val photoRequest =
            FetchPhotoRequest.builder(photoMetadata)
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

    fun fetchPlaceDetails(
        placeId: String,
        onSuccess: (RestaurantData) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // 가져올 필드 정의
        val placeFields = listOf(
            Field.ID,
            Field.NAME,
            Field.ADDRESS,
            Field.LAT_LNG,
            Field.RATING,
            Field.BUSINESS_STATUS,
            Field.PHOTO_METADATAS,
            Field.OPENING_HOURS,
            Field.CURRENT_OPENING_HOURS,
            Field.UTC_OFFSET,
            Field.PHONE_NUMBER
        )

        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placeClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                // 영업 정보 구분
                val statusText = when {
                    place.businessStatus == BusinessStatus.CLOSED_TEMPORARILY -> "임시 휴업"
                    place.isOpen == true -> "영업 중"
                    place.isOpen == false -> "영업 종료"
                    else -> "영업 정보 없음"
                }

                // RestaurantData 모델로 변환
                val restaurant =
                    RestaurantData(
                        name = place.name ?: "",
                        address = place.address ?: "",
                        id = place.id ?: "",
                        latitude = place.latLng?.latitude ?: 0.0,
                        longitude = place.latLng?.longitude ?: 0.0,
                        businessStatus = statusText,
                        photoMetadata = place.photoMetadatas?.take(5) ?: emptyList(),
                        phoneNumber = place.phoneNumber,
                        openingHours = place.openingHours?.weekdayText
                    )
                onSuccess(restaurant)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}