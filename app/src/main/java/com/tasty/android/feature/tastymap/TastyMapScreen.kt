package com.tasty.android.feature.tastymap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.maps.android.compose.*
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.search.PlaceSearchScreen
import com.tasty.android.feature.tastymap.model.RestaurantData
import com.tasty.android.feature.tastymap.model.formatPriceLevel
import com.tasty.android.feature.tastymap.model.getPrimaryCategory
import kotlinx.coroutines.launch
import kotlin.math.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TastyMapScreen(
    navController: NavController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    initialRestaurantId: String? = null,
    viewModel: TastyMapViewmodel = viewModel(factory = TastyMapViewmodel.Factory)
) {
    val uiState = viewModel.uiState

    val listState = rememberLazyListState()

    // 드래그 핸들을 통한 강제 닫기인지 확인하는 플래그
    var isForceClosing by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden, // 초기 상태를 숨김으로
            skipHiddenState = false,
            // 바텀시트 콘텐츠 목록 최상단에 있지 않을 때는 시트가 접히지 않도록 방어
            confirmValueChange = { newValue ->
                // 핸들을 잡고 있는 상태라면(isForceClosing) 어떤 상태 변화(접기/닫기)도 허용
                if (isForceClosing) {
                    true
                } else {
                    // 핸들이 아닐 때는 리스트가 최상단일 때만 시트 변화 허용
                    val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    newValue != SheetValue.Hidden || isAtTop
                }
            }
        )
    )
    val defaultLocation = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 18f)
    }
    val scope = rememberCoroutineScope()

    // 바텀 시트가 완전히 펼쳐진 상태(Expanded)인지 확인
    val isSheetExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    // 위치 설정 팝업 결과 처리를 위한 런처
    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 사용자가 '확인'을 눌러 GPS를 켰을 때
            viewModel.initializeLocation { latLng ->

                if (initialRestaurantId == null)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 18f)

                // 위치 초기화 후, 전달받은 id가 있는 경우 선택 로직 실행
                if (initialRestaurantId != null && !uiState.isLocationLoading) {
                    // 위치 로딩이 끝나고 ID가 있을 때 실행
                    viewModel.selectRestaurantById(
                        initialRestaurantId,
                        viewModel.uiState.userLocation!!
                    ) {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }
                }
            }

        } else {
            // 사용자가 '취소'를 눌렀을 때 처리
            Log.d("test", "위치 서비스 활성화 거부됨")
        }
    }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status is com.google.accompanist.permissions.PermissionStatus.Granted) {
            // 권한이 허용된 경우: 기존 GPS 활성화 체크 로직 실행
            viewModel.checkAndLoadLocation(
                onResolvableException = { exception ->
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    settingResultLauncher.launch(intentSenderRequest)
                },
                onReady = {
                    viewModel.initializeLocation { latLng ->
                        if (initialRestaurantId == null) {
                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(latLng, 18f)
                        }
                    }
                }
            )
        } else {
            // 권한이 없는 경우: 시스템 권한 요청 팝업 띄우기
            locationPermissionState.launchPermissionRequest()
        }
    }

    // 카메라 이동 감지 로직
    LaunchedEffect(cameraPositionState.isMoving) {
        if (viewModel.uiState.isSearchPerformed && cameraPositionState.isMoving) {
            viewModel.resetSearchState()
        }
    }

    // 화면 진입 시 위치 서비스 체크 실행
    LaunchedEffect(Unit) {
        viewModel.checkAndLoadLocation(
            onResolvableException = { exception ->
                // 시스템 다이얼로그 띄우기
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                settingResultLauncher.launch(intentSenderRequest)
            }
        )
    }

    // 디바이스 백버튼 처리
    BackHandler(enabled = isSheetExpanded) {
        scope.launch {
            // 뒤로가기 클릭 시 축소
            if (viewModel.uiState.selectedRestaurant == null) {
                scope.launch {
                    listState.scrollToItem(0)
                }
                scaffoldState.bottomSheetState.partialExpand()
            }
            viewModel.clearSelection()
        }
    }

    LaunchedEffect(uiState.isLocationLoading) {
        if (uiState.isLocationLoading) {
            // 로딩 중일 때는 바텀바를 숨김
            onScaffoldConfigChange(
                ScaffoldConfig(showTopBar = false, showBottomBar = false)
            )
        } else {
            // 로딩 완료 후 다시 표시
            onScaffoldConfigChange(
                ScaffoldConfig(showTopBar = false, showBottomBar = true)
            )
        }
    }

    // 화면 진입 시 위치 초기화 및 Scaffold 설정
    LaunchedEffect(initialRestaurantId, uiState.isLocationLoading) {
        viewModel.initializeLocation { latLng ->

            if (initialRestaurantId == null)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 18f)

            // 위치 초기화 후, 전달받은 id가 있는 경우 선택 로직 실행
            if (initialRestaurantId != null && !uiState.isLocationLoading) {
                // 위치 로딩이 끝나고 ID가 있을 때 실행
                viewModel.selectRestaurantById(
                    initialRestaurantId,
                    viewModel.uiState.userLocation!!
                ) {
                    scope.launch {
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                }
            }
        }
    }

    // 선택된 식당이 변경되면 해당 위치로 지도 이동
    LaunchedEffect(uiState.selectedRestaurant) {
        uiState.selectedRestaurant?.let { restaurant ->
            val targetLatLng = LatLng(restaurant.latitude, restaurant.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLatLng, 18f)
        }
    }

    // 현재 위치 로딩 중일 때 프로그레스 처리
    if (uiState.isLocationLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.8f)), // 배경을 살짝 어둡게 하거나 흰색으로 덮음
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF3B7CFF),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "내 위치를 불러오는 중입니다...",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                )
            }
        }
    }

    if (!uiState.isLocationLoading) {
        // 바텀 시트와 지도를 포함하는 스캐폴드
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 100.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            scope.launch {
                                                listState.scrollToItem(0)
                                            }
                                            isForceClosing = true
                                        }
                                        PointerEventType.Release -> {
                                            isForceClosing = false
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 실제 시각적인 핸들 모양 (회색 바)
                    Surface(
                        modifier = Modifier
                            .width(34.dp)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    ) {}
                }

            },
            sheetContent = {
                RestaurantListSheet(
                    uiState = uiState,
                    onItemClick = { restaurant ->
                        viewModel.selectRestaurant(restaurant, {
                            scope.launch { scaffoldState.bottomSheetState.expand() }
                        })
                    },
                    viewModel = viewModel,
                    navController = navController,
                    listState = listState
                )
            },
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
                    // 검색 반경 표시 (Circle)
                    if (uiState.isSearchPerformed) {
                        Circle(
                            center = viewModel.uiState.lastCameraLocation,
                            radius = uiState.lastSearchRadius, // 미터(m) 단위
                            fillColor = Color(0x224285F4), // 반투명한 파란색
                            strokeColor = Color(0xFF4285F4), // 진한 파란색 테두리
                            strokeWidth = 2f
                        )
                    }
                    uiState.restaurants.forEach { rest ->
                        val isSelected = uiState.selectedRestaurant == rest
                        // 평점 기반의 커스텀 마커 생성
                        val ratingIcon = remember(rest.id, rest.rating, isSelected) {
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

                // 십자선 표시
                AnimatedVisibility(
                    visible = cameraPositionState.isMoving, // 카메라가 움직일 때만 노출
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CrosshairPointer()
                }

                // 검색창, 검색 버튼, 내 위치 버튼
                MapOverlayUI(
                    viewModel = viewModel,
                    cameraPositionState = cameraPositionState,
                    uiState = uiState,
                    scaffoldState = scaffoldState
                )
            }
        }
    }
}

@Composable
fun RestaurantListSheet(
    uiState: TastyMapUiState,
    onItemClick: (RestaurantData) -> Unit,
    viewModel: TastyMapViewmodel,
    navController: NavController,
    listState: LazyListState,
) {
    // 선택된 식당이 있으면 단일 항목만, 없으면 전체 리스트 노출
    val displayList = uiState.selectedRestaurant?.let { listOf(it) } ?: uiState.restaurants

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
                    .background(Color.White)
                    // 내부 스크롤이 시트 드래그보다 우선시 하게 설정
                    .nestedScroll(rememberNestedScrollInteropConnection()),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ), // 상하 패딩을 16dp에서 8dp로 조정
                verticalArrangement = Arrangement.spacedBy(16.dp) // 항목 간 간격을 24dp에서 16dp로 축소
            ) {
                // 바텀 시트 정렬 버튼
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp), // 하단 여백을 8dp에서 4dp로 축소
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if(!uiState.isCommentVisible) {
                            // 거리순 정렬 버튼
                            FilterChip(
                                selected = uiState.sortType == SortType.DISTANCE,
                                onClick = { viewModel.setSortType(SortType.DISTANCE) },
                                label = { Text("거리순", fontSize = 12.sp) }, // 폰트 사이즈 미세 조정 가능
                                leadingIcon = if (uiState.sortType == SortType.DISTANCE) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp) // 아이콘 크기 18dp에서 16dp로 축소
                                        )
                                    }
                                } else null
                            )

                            // 평점순 정렬 버튼
                            FilterChip(
                                selected = uiState.sortType == SortType.RATING,
                                onClick = { viewModel.setSortType(SortType.RATING) },
                                label = { Text("평점순", fontSize = 12.sp) },
                                leadingIcon = if (uiState.sortType == SortType.RATING) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
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

    // 위치 설정 팝업 결과 처리를 위한 런처
    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 사용자가 '확인'을 눌러 GPS를 켰을 때
            viewModel.initializeLocation { latLng ->
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                    )
                }
            }

        } else {
            // 사용자가 '취소'를 눌렀을 때 처리
            Log.d("test", "위치 서비스 활성화 거부됨")
        }
    }

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
                viewModel.selectRestaurantById(restaurantId, viewModel.uiState.userLocation!!, {
                    scope.launch {
                        scaffoldState.bottomSheetState.show()
                        scaffoldState.bottomSheetState.partialExpand()
                    }
                })
            }
        )

        // 반경 선택 및 검색 버튼 영역
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !uiState.isSearchFocused) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val radiusOptions = listOf(
                        "정밀 검색(500m)" to 500.0,
                        "표준 검색(1km)" to 1000.0,
                        "광역 검색(5km)" to 5000.0
                    )

                    radiusOptions.forEach { (label, value) ->
                        FilterChip(
                            selected = uiState.searchRadius == value,
                            onClick = { viewModel.setSearchRadius(value) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B7CFF),
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.searchRadius == value,
                                borderColor = Color(0xFF3B7CFF)
                            )
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !uiState.isSearchFocused,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp)
        ) {
            Button(
                onClick = {
                    viewModel.setLastCameraLocation(cameraPositionState.position.target)
                    viewModel.searchAndSyncRestaurants(
                        viewModel.uiState.lastCameraLocation,
                        {
                            scope.launch {
                                scaffoldState.bottomSheetState.show()
                                val targetZoom = when {
                                    uiState.searchRadius <= 500.0 -> 15.5f
                                    uiState.searchRadius <= 1000.0 -> 14.5f
                                    uiState.searchRadius <= 5000.0 -> 12f
                                    else -> 10.0f
                                }
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        viewModel.uiState.lastCameraLocation,
                                        targetZoom
                                    ),
                                    durationMs = 500
                                )
                                viewModel.setSearchState()
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
                if (uiState.isInitializingLocation) return@FloatingActionButton // 로딩 중 클릭 방지
                // 위치 서비스 상태를 체크
                viewModel.checkAndLoadLocation(
                    onResolvableException = { exception ->
                        // 서비스가 꺼져 있다면 다이얼로그 출력
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(exception.resolution).build()
                        settingResultLauncher.launch(intentSenderRequest)
                    },
                    onReady = {
                        // 서비스가 켜져 있다면 위치를 초기화하고 카메라를 이동
                        viewModel.initializeLocation { latLng ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(latLng, 18f)
                                )
                            }
                        }
                    }
                )
            },
            containerColor = if (uiState.isInitializingLocation) Color.LightGray else Color.White,
            contentColor = if (uiState.isInitializingLocation) Color.Gray else Color.Blue
        ) {
            if (uiState.isInitializingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.Blue
                )
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = "내 위치")
            }
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
    val context = LocalContext.current
    // 영업시간 상세 목록의 펼침 상태를 관리하는 변수
    var isHoursExpanded by remember { mutableStateOf(false) }

    val ratingText =
        if (restaurant.rating != null && restaurant.rating > 0) restaurant.rating.toString() else "0.0"
    val displayRatingText =
        if (restaurant.feedCount > 0) "$ratingText(${restaurant.feedCount})" else ratingText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!showComments) Modifier.clickable { onItemClick() }
                else Modifier
            )
            .padding(vertical = 8.dp)
    ) {
        // 식당 이름, 별점, 현재 영업 상태를 표시하는 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom // 이름과 영업 상태의 하단 라인을 맞춤
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 별점 표시 (이름 위로 이동)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB400),
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = displayRatingText,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 식당 이름
                Text(
                    text = restaurant.name,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 영업 상태에 따라 텍스트 색상을 분기 처리
            Text(
                text = restaurant.businessStatus,
                color = if (restaurant.businessStatus == "영업 중") Color(0xFF4CAF50)
                else if (restaurant.businessStatus == "영업 종료") Color.Red
                else Color.Gray,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        // 카테고리, 가격대, 거리를 보여주는 요약 정보 띠
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 대표 카테고리 칩
            Surface(
                color = Color(0xFFF0F4FF),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = restaurant.getPrimaryCategory(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = TextStyle(
                        color = Color(0xFF3B7CFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // 가격 수준 정보
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = restaurant.formatPriceLevel(),
                    style = TextStyle(color = Color(0xFF666666), fontSize = 13.sp)
                )
            }

            // 시각적 구분을 위한 수직선
            Box(
                modifier = Modifier
                    .size(1.dp, 12.dp)
                    .background(Color(0xFFDDDDDD))
            )

            // 계산된 거리 표시 (포맷팅 함수 사용)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF3B7CFF)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDistance(restaurant.distance),
                    style = TextStyle(
                        color = Color(0xFF333333),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // 지번/도로명 주소 텍스트
        Text(
            text = restaurant.address,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // 구글 플레이스 등에서 가져온 식당 사진 목록
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(restaurant.photoMetadata) { metadata ->
                RestaurantPhotoItem(metadata, placesManager)
            }
        }

        // 특정 식당이 선택되었을 때만 노출되는 상세 영역 (전화, 영업시간, 리뷰)
        if (showComments) {
            Spacer(modifier = Modifier.height(16.dp))

            // 전화번호 버튼: 클릭 시 기기의 전화 앱으로 연결
            val hasPhoneNumber = !restaurant.phoneNumber.isNullOrBlank()
            val displayPhone = if (restaurant.phoneNumber.isNullOrBlank()) "전화번호 정보 없음"
            else formatKoreanPhoneNumber(restaurant.phoneNumber)

            Card(
                onClick = {
                    if (hasPhoneNumber) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${displayPhone}")
                        }
                        context.startActivity(intent)
                    }
                },
                enabled = hasPhoneNumber,
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPhoneNumber) Color(0xFF3B7CFF) else Color(0xFFF2F2F2),
                    contentColor = if (hasPhoneNumber) Color.White else Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = displayPhone,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 영업시간 정보: 클릭 시 요일별 리스트가 펼쳐짐
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8F9FA),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                onClick = {
                    if (!restaurant.openingHours.isNullOrEmpty()) isHoursExpanded = !isHoursExpanded
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "영업시간 안내",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!restaurant.openingHours.isNullOrEmpty()) {
                            Icon(
                                imageVector = if (isHoursExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    // 리스트가 펼쳐진 상태일 때 요일별 시간대 출력
                    if (isHoursExpanded && !restaurant.openingHours.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        restaurant.openingHours.forEach { hour ->
                            val parts = hour.split(": ", limit = 2)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    parts.getOrNull(0) ?: "",
                                    style = TextStyle(fontSize = 13.sp, color = Color(0xFF777777))
                                )
                                Text(
                                    parts.getOrNull(1) ?: "",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            var isNaviOptionsExpanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth()) {
                // 길찾기 메인 버튼
                Button(
                    onClick = { isNaviOptionsExpanded = !isNaviOptionsExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF3B7CFF)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3B7CFF)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("길찾기 / 외부 지도에서 보기", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    }
                }

                // 버튼 클릭 시 나타나는 외부 지도 앱 선택 레이아웃
                AnimatedVisibility(
                    visible = isNaviOptionsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val mapApps = listOf(
                            "NAVER" to "네이버 지도",
                            "KAKAO" to "카카오맵",
                            "GOOGLE" to "구글 지도"
                        )

                        mapApps.forEach { (type, label) ->
                            OutlinedButton(
                                onClick = {
                                    openExternalMap(context, restaurant, type)
                                    isNaviOptionsExpanded = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                            ) {
                                Text(
                                    text = label,
                                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }

            // 사용자 리뷰 피드 목록
            Spacer(modifier = Modifier.height(24.dp))
            Text("방문자 리뷰", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isFeedsLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (uiState.restaurantFeeds.isEmpty()) {
                Text(
                    "리뷰가 아직 없어요.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                uiState.restaurantFeeds.forEach { feed ->
                    CommentItem(
                        nickName = feed.authorNickname,
                        authorProfileUrl = feed.authorProfileUrl,
                        comment = feed.content,
                        onItemClick = {
                            // 리뷰 클릭 시 해당 피드의 상세 화면으로 이동
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

@Composable
fun CrosshairPointer() {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val color = Color(0xFF3B7CFF) // 기존 테마와 맞춘 파란색
            val lineLength = 15.dp.toPx()

            // 가로선
            drawLine(
                color = color,
                start = Offset(size.width / 2 - lineLength, size.height / 2),
                end = Offset(size.width / 2 + lineLength, size.height / 2),
                strokeWidth = strokeWidth
            )
            // 세로선
            drawLine(
                color = color,
                start = Offset(size.width / 2, size.height / 2 - lineLength),
                end = Offset(size.width / 2, size.height / 2 + lineLength),
                strokeWidth = strokeWidth
            )
        }

        // 중앙에 작은 점 하나 추가 (정밀도 향상)
        Surface(
            modifier = Modifier.size(4.dp),
            shape = CircleShape,
            color = Color(0xFF3B7CFF)
        ) {}
    }
}

fun openExternalMap(context: android.content.Context, restaurant: RestaurantData, type: String) {
    val uri = when (type) {
        "GOOGLE" -> {
            // 구글 지도: 쿼리에 식당 이름과 좌표를 함께 전달
            Uri.parse("geo:${restaurant.latitude},${restaurant.longitude}?q=${Uri.encode(restaurant.name)}")
        }
        "KAKAO" -> {
            // 카카오맵: 스키마를 이용해 좌표와 이름을 전달
            Uri.parse("kakaomap://look?p=${restaurant.latitude},${restaurant.longitude}")
        }
        "NAVER" -> {
            // 네이버 지도: 앱 호출 스키마 (설치 안 되어 있으면 마켓으로 이동하게 예외처리 필요)
            // dlat, dlng는 목적지 좌표, dname은 목적지 이름
            Uri.parse("nmap://place?lat=${restaurant.latitude}&lng=${restaurant.longitude}&name=${Uri.encode(restaurant.name)}&appname=com.tasty.android")
        }
        else -> return
    }

    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // 앱이 없을 경우 각 스토어로 연결
        val marketUrl = when (type) {
            "KAKAO" -> "market://details?id=net.daum.android.map"
            "NAVER" -> "market://details?id=com.nhn.android.nmap"
            else -> null
        }
        marketUrl?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }
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

    val text = rating.toString()

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