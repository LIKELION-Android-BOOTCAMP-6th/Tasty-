package com.tasty.android.feature.tastymap

import android.annotation.SuppressLint
import android.graphics.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.maps.android.compose.*
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.search.PlaceSearchScreen
import com.tasty.android.feature.tastymap.model.RestaurantData
import kotlinx.coroutines.launch
import kotlin.math.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TastyMapScreen(
    navController: NavController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: TastyMapViewmodel = viewModel(factory = TastyMapViewmodel.Factory),
    initialRestaurantId: String? = null
) {
    val uiState = viewModel.uiState
    val scaffoldState = rememberBottomSheetScaffoldState()
    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

    // 화면 진입 시 위치 초기화 및 Scaffold 설정
    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(title = "테이스티 맵", showTopBar = false, showBottomBar = true)
        )
        viewModel.initializeLocation { latLng ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 18f)

            // 위치 초기화 후, 전달받은 id가 있는 경우 선택 로직 실행
            initialRestaurantId?.let { id ->
                viewModel.selectRestaurantById(id)}
        }
    }

    // 선택된 식당이 변경되면 해당 위치로 지도 이동
    LaunchedEffect(uiState.selectedRestaurant) {
        uiState.selectedRestaurant?.let { restaurant ->
            val targetLatLng = LatLng(restaurant.latitude, restaurant.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(targetLatLng, 18f),
                500 // 0.5초 동안 부드럽게 이동
            )
        }
    }

    // 바텀 시트와 지도를 포함하는 스캐폴드
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetContent = {
            RestaurantListSheet(
                uiState = uiState,
                onItemClick = { restaurant ->
                    viewModel.selectRestaurant(restaurant)
                    scope.launch { scaffoldState.bottomSheetState.expand() }
                },
                viewModel = viewModel
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 구글 맵 렌더링 및 마커 표시
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = {
                    viewModel.clearSelection()
                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                },
                properties = MapProperties(isMyLocationEnabled = uiState.userLocation != null),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
            ) {
                uiState.restaurants.forEach { rest ->
                    val isSelected = uiState.selectedRestaurant == rest
                    // 평점 기반의 커스텀 마커 생성
                    val ratingIcon = remember(rest.rating, isSelected) {
                        createSimpleRatingMarker(rest.rating ?: 0.0, isSelected)
                    }

                    Marker(
                        state = MarkerState(position = LatLng(rest.latitude, rest.longitude)),
                        icon = ratingIcon,
                        anchor = Offset(0.5f, 1.0f),
                        onClick = {
                            viewModel.selectRestaurant(rest)
                            scope.launch { scaffoldState.bottomSheetState.expand() }
                            true
                        }
                    )
                }
            }

            // 검색창, 검색 버튼, 내 위치 버튼
            MapOverlayUI(
                viewModel = viewModel,
                cameraPositionState = cameraPositionState,
                uiState = uiState
            )
        }
    }
}

@Composable
fun RestaurantListSheet(
    uiState: TastyMapUiState,
    onItemClick: (RestaurantData) -> Unit,
    viewModel: TastyMapViewmodel
) {
    // 선택된 식당이 있으면 단일 항목만, 없으면 전체 리스트 노출
    val displayList = uiState.selectedRestaurant?.let { listOf(it) } ?: uiState.restaurants

    if (displayList.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("주변에 식당이 없습니다.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().background(Color.White),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(displayList) { restaurant ->
                RestaurantItem(
                    restaurant = restaurant,
                    placesManager = viewModel.placesManager,
                    userLocation = uiState.userLocation,
                    onItemClick = { onItemClick(restaurant) },
                    showComments = uiState.isCommentVisible && uiState.selectedRestaurant?.id == restaurant.id,
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
fun MapOverlayUI(
    viewModel: TastyMapViewmodel,
    cameraPositionState: CameraPositionState,
    uiState: TastyMapUiState
) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        PlaceSearchScreen(
            placesManager = viewModel.placesManager,
            labelText = "장소 및 음식점 검색",
            onFocusChange = { viewModel.setSearchFocus(it) },
            onPlaceSelected = { latLng ->
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                }
            }
        )

        AnimatedVisibility(
            visible = !uiState.isSearchFocused,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 120.dp)
        ) {
            Button(onClick = {
                viewModel.searchAndSyncRestaurants(cameraPositionState.position.target, 1000.0)
            }) {
                Text("이 지역 식당 검색")
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp),
            onClick = {
                uiState.userLocation?.let {
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 18f)) }
                }
            },
            containerColor = Color.White,
            contentColor = Color.Blue
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "내 위치")
        }
    }
}

@Composable
fun RestaurantItem(
    restaurant: RestaurantData,
    placesManager: PlaceManager,
    userLocation: LatLng?,
    onItemClick: () -> Unit,
    showComments: Boolean,
    uiState: TastyMapUiState
) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onItemClick() }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(restaurant.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp))
            Text(
                restaurant.businessStatus,
                color = if (restaurant.businessStatus == "영업 중") Color(0xFF4CAF50) else Color.Red,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
        }
        Text(restaurant.address, color = Color.Gray, fontSize = 14.sp)

        val distance = userLocation?.let {
            calculateDistance(it.latitude, it.longitude, restaurant.latitude, restaurant.longitude)
        } ?: 0
        InfoText("평점: ${restaurant.rating}, 리뷰: ${restaurant.feedCount}개, 거리: ${formatDistance(distance)}")

        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(restaurant.photoMetadata) { metadata ->
                RestaurantPhotoItem(metadata, placesManager)
            }
        }

        if (showComments) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("방문자 리뷰", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            if (uiState.isFeedsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                uiState.restaurantFeeds.forEach { feed ->
                    CommentItem(comment = feed.content)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// --- 유틸리티 및 헬퍼 함수 (기존 로직 유지) ---

@Composable
fun CommentItem(comment: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF9F9F9)).padding(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("익명 사용자", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(comment, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun RestaurantPhotoItem(metadata: PhotoMetadata, placesManager: PlaceManager) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(metadata) { placesManager.fetchPhoto(metadata) { bitmap = it } }
    if (bitmap != null) {
        Image(bitmap!!.asImageBitmap(), null, Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F2F2)))
    }
}

@Composable
fun InfoText(text: String) {
    Text(text, color = Color(0xFF757575), fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp))
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
}

fun formatDistance(distanceInMeters: Int): String {
    return if (distanceInMeters >= 1000) "%.1fkm".format(distanceInMeters / 1000.0) else "${distanceInMeters}m"
}

// Canvas를 이용해 평점이 적힌 말풍선 모양의 비트맵 마커를 생성
fun createSimpleRatingMarker(rating: Double, isSelected: Boolean): BitmapDescriptor {
    val text = rating.toString()
    val mainColor = if (isSelected) android.graphics.Color.parseColor("#3B7CFF") else android.graphics.Color.parseColor("#A0C4FF")
    val textPaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = 36f; isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val rectWidth = textBounds.width() + 60f
    val rectHeight = textBounds.height() + 40f
    val bitmap = Bitmap.createBitmap(rectWidth.toInt() + 24, rectHeight.toInt() + 40, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply { isAntiAlias = true; color = mainColor; style = Paint.Style.FILL; setShadowLayer(8f, 0f, 6f, android.graphics.Color.parseColor("#40000000")) }

    val path = Path().apply {
        addRoundRect(RectF(12f, 12f, rectWidth + 12f, rectHeight + 12f), rectHeight / 2, rectHeight / 2, Path.Direction.CW)
        moveTo(rectWidth / 2 + 12f - 15f, rectHeight + 12f)
        lineTo(rectWidth / 2 + 12f, rectHeight + 28f)
        lineTo(rectWidth / 2 + 12f + 15f, rectHeight + 12f)
    }
    canvas.drawPath(path, bgPaint)
    canvas.drawText(text, rectWidth / 2 + 12f, (rectHeight / 2 + 12f) - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}