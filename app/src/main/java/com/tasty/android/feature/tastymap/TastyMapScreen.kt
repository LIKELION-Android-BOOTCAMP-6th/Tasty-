package com.tasty.android.feature.tastymap

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tasty.android.core.design.component.AppBarAction
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.location.LocationManager
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.auth.LoginViewModel
import com.tasty.android.feature.search.PlaceSearchScreen
import com.tasty.android.feature.tastymap.model.RestaurantData
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastyMapScreen(
    navController: NavController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewmodel: TastyMapViewmodel = viewModel(factory = TastyMapViewmodel.Factory)
) {

    // 검색된 식당 리스트
    var restaurants by remember { mutableStateOf<List<RestaurantData>>(emptyList()) }
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    var userLat by remember { mutableDoubleStateOf(0.0) }
    var userLon by remember { mutableDoubleStateOf(0.0) }

    val scope = rememberCoroutineScope()

    // 바텀 시트 제어 상태
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden, // 처음엔 숨김
            skipHiddenState = false
        )
    )

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "테이스티 맵",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
                isCenterAligned = true
            )
        )
    }

    // 초기 위치 설정 및 위치 추적
    LaunchedEffect(Unit) {
        // 현재 위치 업데이트
        viewmodel.locationManager.getCurrentLocation { lat, lon ->
            userLat = lat
            userLon = lon
            Log.d("test", "현재 위치 - 위도: $lat, 경도: $lon")

            val userLatLng = LatLng(lat, lon)
            currentUserLocation = userLatLng

            // 카메라를 사용자 위치로 이동
            cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 18f)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 200.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            // 결과가 있을 때만 리스트 표시
            if (restaurants.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp), contentAlignment = Alignment.Center
                ) {
                    Text("주변에 식당이 없습니다.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(restaurants) { restaurant ->
                        RestaurantItem(restaurant, viewmodel.placesManager, userLat, userLon)
                    }
                }
            }
        }
    )
    {
        Box(modifier = Modifier.fillMaxSize()) {
            // 구글 맵
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // 지도에 내 위치 파란 점 표시
                properties = MapProperties(isMyLocationEnabled = currentUserLocation != null),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false, // 기본 내 위치 버튼 비활성화
                    zoomControlsEnabled = false // 기본 확대/축소 버튼 비활성화
                )
            ) {
                // 검색된 식당들에 마커 표시
                restaurants.forEach { rest ->
                    val restaurantLocation = LatLng(rest.latitude, rest.longitude)

                    // 평점 데이터(rest.rating)를 사용하여 커스텀 아이콘 생성
                    // 파란색 배경에 평점이 적힌 마커
                    val ratingIcon = remember(rest.rating) {
                        createSimpleRatingMarker(rating = rest.rating ?: 0.0)
                    }

                    Marker(
                        state = MarkerState(position = restaurantLocation),
                        icon = ratingIcon,// 생성한 커스텀 아이콘 적용
                        // 0.5f(가로 중앙), 1.0f(세로 최하단)을 기준으로 위치를 잡는다
                        anchor = Offset(0.5f, 1.0f),
                        onClick = {
                            // 클릭 시 바텀 시트를 올리는 등의 로직
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            true
                        }
                    )
                }
            }

            // 식당 검색 버튼 클릭 시 호출
            Button(
                onClick = {
                    val targetLocation = cameraPositionState.position.target
                    viewmodel.placesManager.searchRestaurants(targetLocation, 1000.0) { result ->
                        restaurants = result
                        // 결과가 오면 바텀 시트를 부분 확장 상태로 올림
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            ) {
                Text("이 지역 식당 검색")
            }

            PlaceSearchScreen(
                placesManager = viewmodel.placesManager,
                labelText = "장소 및 음식점 검색",
                onPlaceSelected = { latLng ->
                    // 검색된 위치로 카메라 이동
                    scope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(latLng, 18f),
                            durationMs = 1000
                        )
                    }
                }
            )

            // 커스텀 버튼 배치(카메라 현재 위치로 이동)
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 16.dp),
                onClick = {
                    scope.launch {
                        viewmodel.fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val userLatLng = LatLng(it.latitude, it.longitude)
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(userLatLng, 18f)
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = Color.White,
                contentColor = Color.Blue
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "내 위치")
            }
        }
    }
}

@Composable
fun RestaurantItem(
    restaurant: RestaurantData,
    placesManager: PlaceManager,
    userLat: Double, userLon: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 이름 및 영업 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = restaurant.name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = restaurant.businessStatus,
                color = when (restaurant.businessStatus) {
                    "영업 중" -> Color(0xFF4CAF50)
                    "영업 종료" -> Color.Red
                    else -> Color.Gray
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        // 위치
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = restaurant.address,
            color = Color.Gray,
            fontSize = 14.sp
        )

        // 부가 정보 (평점, 리뷰 개수)
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // 거리 계산
            val distance = remember(restaurant.latitude, restaurant.longitude, userLat, userLon) {
                calculateDistance(userLat, userLon, restaurant.latitude, restaurant.longitude)
            }
            val distanceText = formatDistance(distance)
            InfoText(text = "평점 4.6, 리뷰 55개, 거리: $distanceText")
        }

        // 음식 사진 가로 리스트
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(restaurant.photoMetadata) { metadata ->
                RestaurantPhotoItem(metadata, placesManager)
            }
        }
    }
}

@Composable
fun RestaurantPhotoItem(metadata: PhotoMetadata, placesManager: PlaceManager) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(metadata) {
        placesManager.fetchPhoto(metadata) { loadedBitmap ->
            bitmap = loadedBitmap
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        // 로딩 중일 때 표시할 박스
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F2))
        )
    }
}

@Composable
fun InfoText(text: String) {
    Text(
        text = text,
        color = Color(0xFF757575),
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

/**
 * 두 지점 간의 직선 거리를 미터(m) 단위로 계산합니다.
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371000.0 // 지구 반지름 (미터)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).toInt()
}

/**
 * 거리를 보기 좋게 포맷팅합니다 (1000m 이상은 km로 표시).
 */
fun formatDistance(distanceInMeters: Int): String {
    return if (distanceInMeters >= 1000) {
        String.format("%.1fkm", distanceInMeters / 1000.0)
    } else {
        "${distanceInMeters}m"
    }
}

// 평점 포함한 식당 마커 아이콘 생성
fun createSimpleRatingMarker(rating: Double): BitmapDescriptor {
    // 1. 초기 데이터 및 색상 설정
    val text = rating.toString()
    val mainColor = android.graphics.Color.parseColor("#3B7CFF") // 마커 배경색 (푸른색)
    val shadowColor = android.graphics.Color.parseColor("#40000000") // 그림자색 (25% 투명도 검정)
    val strokeColor = android.graphics.Color.WHITE // 테두리색 (흰색)

    // 2. 텍스트 Paint 설정 (글꼴, 크기, 정렬 등)
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 36f             // 글자 크기
        isFakeBoldText = true      // 굵게 처리
        isAntiAlias = true         // 경계면 부드럽게
        textAlign = Paint.Align.CENTER // 가로 정렬 기준을 중앙으로 설정
    }

    // 3. 텍스트의 실제 점유 영역 계산
    // 입력된 평점 텍스트의 길이에 따라 마커의 너비를 동적으로 조절하기 위함
    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    // 4. 마커 규격 및 여백 수치 정의
    val shadowMargin = 12f       // 그림자가 잘리지 않도록 확보하는 외곽 여백
    val horizontalPadding = 30f  // 텍스트 좌우 안쪽 여백
    val verticalPadding = 20f    // 텍스트 상하 안쪽 여백

    val rectWidth = textBounds.width() + (horizontalPadding * 2) // 마커 몸통 전체 너비
    val rectHeight = textBounds.height() + (verticalPadding * 2) // 마커 몸통 전체 높이
    val cornerRadius = rectHeight / 2 // 양 끝을 완전한 반원으로 만들기 위한 반지름
    val triangleHeight = 16f     // 말풍선 꼬리(삼각형)의 높이
    val triangleWidth = 30f      // 말풍선 꼬리(삼각형)의 밑변 너비

    // 5. 최종 비트맵 생성
    // 몸통 너비/높이에 꼬리 높이와 그림자 여백을 모두 더하여 캔버스 크기를 결정
    val bitmap = Bitmap.createBitmap(
        (rectWidth + shadowMargin * 2).toInt(),
        (rectHeight + triangleHeight + shadowMargin * 2).toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    // 6. 배경 그리기용 Paint 설정
    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = mainColor
        style = Paint.Style.FILL
        // setShadowLayer(반지름, x오프셋, y오프셋, 색상)
        // 하단으로 6f만큼 치우친 그림자를 생성하여 입체감 효과
        setShadowLayer(8f, 0f, 6f, shadowColor)
    }

    // 7. 매끄러운 말풍선 형태의 Path(경로) 설계
    // 몸통과 꼬리를 별개로 그리지 않고 하나의 선으로 연결하여 테두리와 그림자가 끊기지 않게 구성
    val centerX = (rectWidth + shadowMargin * 2) / 2f
    val bodyLeft = shadowMargin
    val bodyTop = shadowMargin
    val bodyRight = shadowMargin + rectWidth
    val bodyBottom = shadowMargin + rectHeight

    val path = android.graphics.Path().apply {
        // 상단 왼쪽 곡선 시작점
        moveTo(bodyLeft + cornerRadius, bodyTop)
        // 상단 직선 및 오른쪽 반원(arc) 그리기
        lineTo(bodyRight - cornerRadius, bodyTop)
        arcTo(RectF(bodyRight - rectHeight, bodyTop, bodyRight, bodyBottom), -90f, 180f, false)

        // 하단 직선 및 중앙 꼬리(삼각형) 그리기
        // 오른쪽 하단에서 중앙 꼬리 시작점까지 이동
        lineTo(centerX + (triangleWidth / 2f), bodyBottom)
        // 꼬리 끝점(뾰족한 부분)으로 선 연결
        lineTo(centerX, bodyBottom + triangleHeight)
        // 꼬리 끝점에서 왼쪽 하단 연결점으로 복귀
        lineTo(centerX - (triangleWidth / 2f), bodyBottom)

        // 왼쪽 하단 직선 및 왼쪽 반원(arc) 마무리
        lineTo(bodyLeft + cornerRadius, bodyBottom)
        arcTo(RectF(bodyLeft, bodyTop, bodyLeft + rectHeight, bodyBottom), 90f, 180f, false)
        close() // 경로 닫기
    }

    // 8. 캔버스에 그리기 작업 수행
    // 배경색과 그림자 그리기
    canvas.drawPath(path, bgPaint)

    // 흰색 테두리(Stroke) 그리기
    val strokePaint = Paint().apply {
        isAntiAlias = true
        color = strokeColor
        style = Paint.Style.STROKE // 채우기가 아닌 선 그리기 모드
        strokeWidth = 3.5f         // 테두리 두께
    }
    canvas.drawPath(path, strokePaint)

    // 텍스트 그리기 (중앙 정렬 계산)
    // 폰트의 Baseline, Ascent, Descent를 고려하여 수직 중앙 위치를 계산
    val textY = (bodyTop + rectHeight / 2) - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(text, centerX, textY, textPaint)

    // 9. 완성된 비트맵을 구글 맵 마커 아이콘으로 변환하여 반환
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}