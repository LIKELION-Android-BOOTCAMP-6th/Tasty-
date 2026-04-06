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
    val name: String,
    val address: String
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
                    name = it.getPrimaryText(null).toString(),
                    address = it.getSecondaryText(null).toString()
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

    /**
     * 격자(Grid) 검색을 통해 주변 식당을 촘촘하게 탐색하고 최대 100개까지 반환합니다.
     */
    suspend fun searchRestaurantsByGrid(
        center: LatLng,          // 검색 중심 좌표
        totalRangeMeters: Double = 500.0, // 전체 검색 범위 (m)
    ): List<RestaurantData> = withContext(Dispatchers.IO) {

        val gridStepMeter = when {
            totalRangeMeters <= 500.0 -> 120.0  // 500m 이하: 정밀 검색
            totalRangeMeters <= 1000.0 -> 180.0 // 1km 이하: 표준 검색
            totalRangeMeters <= 5000.0 -> 400.0 // 5km 이하: 광역 검색
            else -> 600.0                       // 그 이상
        }

        // 중복 제거를 위한 ConcurrentHashMap (식당 ID 기준)
        val accumulatedMap = java.util.concurrent.ConcurrentHashMap<String, RestaurantData>()

        // Google Places API 설정값
        val searchRadius = 180.0 // 각 격자점에서의 검색 반경
        val meterPerLat = 110940.0 // 위도 1도당 미터 거리
        val meterPerLon = 88800.0 * kotlin.math.cos(Math.toRadians(center.latitude)) // 경도 1도당 미터 거리

        // 격자 탐색을 위한 스텝 수 계산
        val maxStep = (totalRangeMeters / gridStepMeter).toInt() / 2
        val placeFields = listOf(
            Field.ID, Field.NAME, Field.ADDRESS, Field.LAT_LNG,
            Field.BUSINESS_STATUS, Field.PHOTO_METADATAS, Field.OPENING_HOURS,
            Field.PHONE_NUMBER, Field.TYPES, Field.PRICE_LEVEL, Field.UTC_OFFSET
        )

        // 나선형(Spiral) 경로로 탐색할 위경도 좌표 리스트 생성
        val points = mutableListOf<LatLng>()
        val side = maxStep * 2 + 1
        var x = 0; var y = 0; var dx = 0; var dy = -1
        for (i in 0 until side * side) {
            val targetLat = center.latitude + (x * gridStepMeter / meterPerLat)
            val targetLon = center.longitude + (y * gridStepMeter / meterPerLon)
            points.add(LatLng(targetLat, targetLon))

            // 나선형 방향 전환 로직
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
                val temp = dx; dx = -dy; dy = temp
            }
            x += dx; y += dy
        }

        // 생성된 좌표들을 5개씩 묶어서(Chunk) 병렬로 API 요청 실행
        for (chunk in points.chunked(5)) {
            // 이미 100개를 수집했다면 전체 루프 종료
            if (accumulatedMap.size >= 100) break

            coroutineScope {
                chunk.map { point ->
                    async {
                        // 개별 코루틴 내부에서도 100개 초과 시 요청 스킵
                        if (accumulatedMap.size >= 100) return@async

                        try {
                            // 특정 지점 주변 식당 검색 요청
                            val circle = CircularBounds.newInstance(point, searchRadius)
                            val request = SearchNearbyRequest.builder(circle, placeFields)
                                .setIncludedTypes(listOf("restaurant", "cafe", "bakery", "bar"))
                                .build()

                            val response = placeClient.searchNearby(request).await()

                            // 검색된 결과 처리
                            response.places.forEach { place ->
                                // 폐업하지 않은 곳만 필터링하여 100개까지 저장
                                if (accumulatedMap.size < 100 &&
                                    place.businessStatus != Place.BusinessStatus.CLOSED_PERMANENTLY) {

                                    val statusText = when {
                                        place.businessStatus == BusinessStatus.CLOSED_TEMPORARILY -> "임시 휴업"
                                        place.isOpen == true -> "영업 중"
                                        place.isOpen == false -> "영업 종료"
                                        else -> "영업 정보 없음"
                                    }

                                    // 내부 데이터 모델(RestaurantData)로 변환 후 맵에 삽입
                                    val restaurant = RestaurantData(
                                        id = place.id ?: "",
                                        name = place.name ?: "",
                                        address = place.address ?: "",
                                        latitude = place.latLng?.latitude ?: 0.0,
                                        longitude = place.latLng?.longitude ?: 0.0,
                                        businessStatus = statusText,
                                        photoMetadata = place.photoMetadatas?.take(5) ?: emptyList(),
                                        phoneNumber = place.phoneNumber,
                                        openingHours = place.openingHours?.weekdayText,
                                        priceLevel = place.priceLevel,
                                        types = place.placeTypes
                                    )
                                    accumulatedMap[restaurant.id] = restaurant
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PlaceManager", "Grid Search Error: ${e.message}")
                        }
                    }
                }.awaitAll() // 5개 요청이 모두 끝날 때까지 대기
            }
            // 과도한 API 호출 방지를 위한 미세한 지연 시간
            delay(50L)
        }

        // 최종 결과물 반환 (정확히 100개까지만 리스트로 변환)
        accumulatedMap.values.toList().take(100)
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
            Field.PHONE_NUMBER,
            Field.TYPES,
            Field.PRICE_LEVEL
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
                        openingHours = place.openingHours?.weekdayText,
                        priceLevel = place.priceLevel,
                        types = place.placeTypes
                    )
                onSuccess(restaurant)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}