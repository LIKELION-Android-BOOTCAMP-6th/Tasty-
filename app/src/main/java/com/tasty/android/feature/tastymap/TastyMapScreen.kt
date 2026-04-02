package com.tasty.android.feature.tastymap

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.maps.android.compose.*
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen
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
    initialRestaurantId: String? = null,
    viewModel: TastyMapViewmodel = viewModel(factory = TastyMapViewmodel.Factory)
) {
    val uiState = viewModel.uiState
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden, // 초기 상태를 숨김으로
            skipHiddenState = false
        )
    )
    val defaultLocation = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 18f)}
    val scope = rememberCoroutineScope()

    // 바텀 시트가 완전히 펼쳐진 상태(Expanded)인지 확인
    val isSheetExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    BackHandler(enabled = isSheetExpanded) {
        scope.launch {
            // 뒤로가기 클릭 시 축소
            scaffoldState.bottomSheetState.partialExpand()
            viewModel.clearSelection()
        }
    }

    // 화면 진입 시 위치 초기화 및 Scaffold 설정
    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(showTopBar = false, showBottomBar = true)
        )
        viewModel.initializeLocation { latLng ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 18f)

            // 위치 초기화 후, 전달받은 id가 있는 경우 선택 로직 실행
            initialRestaurantId?.let { id ->
                viewModel.selectRestaurantById(id, {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                        scaffoldState.bottomSheetState.show()
                    }
                }
                )
            }
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
                    viewModel.selectRestaurant(restaurant, {
                        scope.launch { scaffoldState.bottomSheetState.expand() }
                    })
                },
                viewModel = viewModel,
                navController
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 구글 맵 렌더링 및 마커 표시
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                        viewModel.clearSelection()
                    }
                },
                properties = MapProperties(isMyLocationEnabled = uiState.userLocation != null),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                )
            ) {
                uiState.restaurants.forEach { rest ->
                    val isSelected = uiState.selectedRestaurant == rest
                    // 평점 기반의 커스텀 마커 생성
                    val ratingIcon = remember(rest.id,rest.rating, isSelected) {
                        Log.d("Marker", "마커 생성 호출: ${rest.name}") // 호출 여부 확인용 로그
                        createSimpleRatingMarker(rest.rating ?: 0.0, isSelected)
                    }

                    Marker(
                        state = MarkerState(position = LatLng(rest.latitude, rest.longitude)),
                        icon = ratingIcon,
                        anchor = Offset(0.5f, 1.0f),
                        onClick = {
                            viewModel.selectRestaurant(rest, {
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            })
                            true
                        }
                    )
                }
            }

            // 검색창, 검색 버튼, 내 위치 버튼
            MapOverlayUI(
                viewModel = viewModel,
                cameraPositionState = cameraPositionState,
                uiState = uiState,
                scaffoldState
            )
        }
    }
}

@Composable
fun RestaurantListSheet(
    uiState: TastyMapUiState,
    onItemClick: (RestaurantData) -> Unit,
    viewModel: TastyMapViewmodel,
    navController: NavController
) {
    // 선택된 식당이 있으면 단일 항목만, 없으면 전체 리스트 노출
    val displayList = uiState.selectedRestaurant?.let { listOf(it) } ?: uiState.restaurants
    val listState = rememberLazyListState()

    // 리스트가 변경될 때마다 최상단으로 스크롤 (순간적인 튀는 현상 방지)
    LaunchedEffect(displayList.size) {
        listState.scrollToItem(0)
    }

    Crossfade(
        targetState = displayList,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "SheetContentTransition"
    ) { currentList ->
        if (currentList.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center
            ) {
                Text("주변에 식당이 없습니다.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 바텀 시트 정렬 버튼
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 거리순 정렬 버튼
                        FilterChip(
                            selected = uiState.sortType == SortType.DISTANCE,
                            onClick = { viewModel.setSortType(SortType.DISTANCE) },
                            label = { Text("거리순") },
                            leadingIcon = if (uiState.sortType == SortType.DISTANCE) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )

                        // 평점순 정렬 버튼
                        FilterChip(
                            selected = uiState.sortType == SortType.RATING,
                            onClick = { viewModel.setSortType(SortType.RATING) },
                            label = { Text("평점순") },
                            leadingIcon = if (uiState.sortType == SortType.RATING) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
                items(currentList) { restaurant ->
                    RestaurantItem(
                        restaurant = restaurant,
                        placesManager = viewModel.placesManager,
                        userLocation = uiState.userLocation,
                        onItemClick = { onItemClick(restaurant) },
                        showComments = uiState.isCommentVisible && uiState.selectedRestaurant?.id == restaurant.id,
                        uiState = uiState,
                        navController
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapOverlayUI(
    viewModel: TastyMapViewmodel,
    cameraPositionState: CameraPositionState,
    uiState: TastyMapUiState,
    scaffoldState: BottomSheetScaffoldState
) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        PlaceSearchScreen(
            labelText = "장소 및 음식점 검색",
            onFocusChange = {
                viewModel.setSearchFocus(it)
                scope.launch {
                    scaffoldState.bottomSheetState.hide()
                }
            },
            onPlaceSelectedLocation = { latLng ->
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                }
            },
            onPlaceSelectedRestaurant = { restaurantId ->
                viewModel.selectRestaurantById(restaurantId, {
                    scope.launch {
                        scaffoldState.bottomSheetState.show()
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                })
            }
        )

        AnimatedVisibility(
            visible = !uiState.isSearchFocused,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Button(
                onClick = {
                    val location = cameraPositionState.position.target
                    viewModel.searchAndSyncRestaurants(
                        location,
                        {
                            scope.launch {
                                scaffoldState.bottomSheetState.show()
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 16f)
                            }
                        }
                    )
                },
                // 로딩 중에는 버튼 클릭 방지
                enabled = !uiState.isSearching,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B7CFF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF3B7CFF).copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("검색 중...", fontSize = 14.sp)
                    } else {
                        Text("주변 식당 검색", fontSize = 14.sp)
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 50.dp, end = 16.dp),
            onClick = {
                uiState.userLocation?.let {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                it,
                                18f
                            )
                        )
                    }
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
    uiState: TastyMapUiState,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(restaurant.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp))
            Text(
                restaurant.businessStatus,
                color = if (restaurant.businessStatus == "영업 중") Color(0xFF4CAF50)
                else if(restaurant.businessStatus == "영업 종료") Color.Red
                else Color.Gray,
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
        }
        Text(restaurant.address, color = Color.Gray, fontSize = 14.sp)

        if(showComments) {
            // 전화번호 및 영업시간
            Spacer(modifier = Modifier.height(4.dp))

            // 전화번호 표시
            val displayPhone =
                if (restaurant.phoneNumber.isNullOrBlank()) "전화번호 정보 없음" else formatKoreanPhoneNumber(restaurant.phoneNumber)
            Text(
                text = "📞 $displayPhone",
                color = if (restaurant.phoneNumber.isNullOrBlank()) Color.LightGray else Color.DarkGray,
                fontSize = 13.sp
            )

            // 영업시간 표시
            if (restaurant.openingHours.isNullOrEmpty()) {
                Text(
                    text = "⏰ 영업시간 정보 없음",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            } else {
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "⏰ ",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Column {
                        restaurant.openingHours.forEach { hour ->
                            Text(
                                text = hour,
                                color = Color.DarkGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
        val distance = userLocation?.let {
            calculateDistance(it.latitude, it.longitude, restaurant.latitude, restaurant.longitude)
        } ?: 0
        InfoText(
            "평점: ${if (restaurant.rating!! > 0) restaurant.rating else "( - - )"}, 리뷰: ${restaurant.feedCount}개, 거리: ${
                formatDistance(
                    distance
                )
            }"
        )

        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(restaurant.photoMetadata) { metadata ->
                RestaurantPhotoItem(metadata, placesManager)
            }
        }

        if (showComments) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("방문자 리뷰", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Spacer(modifier = Modifier.height(8.dp)) // 제목과 내용 사이 간격

            if (uiState.isFeedsLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (uiState.restaurantFeeds.isEmpty()) {
                // 리뷰가 없는 경우 표시할 문구
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "리뷰가 아직 없어요.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // 리뷰 리스트 출력
                uiState.restaurantFeeds.forEach { feed ->
                    CommentItem(
                        nickName = feed.authorNickname,
                        authorProfileUrl = feed.authorProfileUrl,
                        comment = feed.content,
                        onItemClick = {
                            navController.navigate("${Screen.FEED_DETAIL.route}/${feed.feedId}")
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// 상세화면의 리뷰(피드) 출력
@Composable
fun CommentItem(
    nickName: String,
    authorProfileUrl: String?,
    comment: String,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9F9F9))
            .clickable {
                onItemClick()
            }
            .padding(12.dp)
    ) {
        if (authorProfileUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "기본 프로필",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically),
                tint = Color(0xFFB5B5B5)
            )
        } else {
            AsyncImage(
                model = authorProfileUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterVertically),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(nickName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                comment,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RestaurantPhotoItem(metadata: PhotoMetadata, placesManager: PlaceManager) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(metadata) { placesManager.fetchPhoto(metadata) { bitmap = it } }
    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            null,
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F2))
        )
    }
}

@Composable
fun InfoText(text: String) {
    Text(
        text,
        color = Color(0xFF757575),
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(
            2
        )
    return (r * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
}

fun formatDistance(distanceInMeters: Int): String {
    return if (distanceInMeters >= 1000) "%.1fkm".format(distanceInMeters / 1000.0) else "${distanceInMeters}m"
}

// Canvas를 이용해 평점이 적힌 말풍선 모양의 비트맵 마커를 생성
fun createSimpleRatingMarker(rating: Double, isSelected: Boolean): BitmapDescriptor {

    val text = if (rating > 0) rating.toString() else "( - - )"

    val mainColor =
        if (isSelected) android.graphics.Color.parseColor("#3B7CFF") else android.graphics.Color.parseColor(
            "#A0C4FF"
        )
    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE; textSize = 36f; isFakeBoldText = true; isAntiAlias =
        true; textAlign = Paint.Align.CENTER
    }
    val textBounds = Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)

    val rectWidth = textBounds.width() + 60f
    val rectHeight = textBounds.height() + 40f
    val bitmap = Bitmap.createBitmap(
        rectWidth.toInt() + 24,
        rectHeight.toInt() + 40,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply {
        isAntiAlias = true; color = mainColor; style = Paint.Style.FILL; setShadowLayer(
        8f,
        0f,
        6f,
        android.graphics.Color.parseColor("#40000000")
    )
    }

    val path = Path().apply {
        addRoundRect(
            RectF(12f, 12f, rectWidth + 12f, rectHeight + 12f),
            rectHeight / 2,
            rectHeight / 2,
            Path.Direction.CW
        )
        moveTo(rectWidth / 2 + 12f - 15f, rectHeight + 12f)
        lineTo(rectWidth / 2 + 12f, rectHeight + 28f)
        lineTo(rectWidth / 2 + 12f + 15f, rectHeight + 12f)
    }
    canvas.drawPath(path, bgPaint)
    canvas.drawText(
        text,
        rectWidth / 2 + 12f,
        (rectHeight / 2 + 12f) - (textPaint.descent() + textPaint.ascent()) / 2,
        textPaint
    )
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun formatKoreanPhoneNumber(number: String?): String {
    if (number == null) return "전화번호 정보 없음"

    // 국제전화 형식 처리: +82(한국 국가코드)를 국내 번호 형식인 0으로 변환
    // 예: +82 10... -> 010...
    var cleanNumber = number.replace("+82", "0")

    // 숫자만 추출
    val digits = cleanNumber.filter { it.isDigit() }

    return when {
        // 서울 지역번호 (02) 처리
        digits.startsWith("02") -> {
            when (digits.length) {
                9 -> digits.replaceFirst("(\\d{2})(\\d{3})(\\d{4})".toRegex(), "$1-$2-$3")
                10 -> digits.replaceFirst("(\\d{2})(\\d{4})(\\d{4})".toRegex(), "$1-$2-$3")
                else -> digits
            }
        }

        // 일반 번호 (휴대폰 010, 지역번호 031 등) 및 특수 번호 처리
        // 12자리: 0507-12345-6789 등 긴 안심번호 대응
        digits.length == 12 -> digits.replaceFirst("(\\d{3})(\\d{5})(\\d{4})".toRegex(), "$1-$2-$3")
        digits.length == 11 -> digits.replaceFirst("(\\d{3})(\\d{4})(\\d{4})".toRegex(), "$1-$2-$3")
        digits.length == 10 -> digits.replaceFirst("(\\d{3})(\\d{3})(\\d{4})".toRegex(), "$1-$2-$3")

        // 예외: 위 조건에 해당하지 않는 자릿수는 숫자만 그대로 노출
        else -> digits
    }
}
