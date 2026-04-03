package com.tasty.android.core.design.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.navigation.CustomNavHost
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Send
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tasty.android.core.design.theme.TextColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding

// 스캐폴드 커스텀 설정 클래스 정의
data class ScaffoldConfig(
    val title: String = "Tasty",
    val showTopBar: Boolean = true, // TopBar 표시 여부
    val showBottomBar: Boolean = true, // BottomBar 표시 여부
    val containsBackButton: Boolean = false, // 뒤로가기 버튼 여부
    val onBackClick: () -> Unit = {}, // 뒤로가기 버튼 이벤트 등록
    val topBarActions: List<AppBarAction> = emptyList(), // 상단 앱바 액션 리스트 등록
    val floatingActionButton: (@Composable () -> Unit)? = null, // null = No FAB,
    val containerColor: Color = PrimaryColor, // 컨테이너 컬러
    val isCenterAligned: Boolean = false // 가운데 정렬 여부
)


private fun getPredictiveConfig(route: String?, navController: NavHostController): ScaffoldConfig {
    if (route == null) return ScaffoldConfig()

    // 경로 파라미터가 포함된 경우를 위해 matching 로직 처리
    return when {

        route == TabScreen.FEED.route -> ScaffoldConfig(
            title = "Tasty",
            showTopBar = true,
            showBottomBar = true,
            isCenterAligned = true,
            floatingActionButton = {
                FeedFab(
                    onWriteClick = { navController.navigate(Screen.FEED_CREATE_FEED.route) },
                    onFilterClick = {
                        // 필터 시트는 Screen에서 LaunchedEffect로 Override하여 처리
                    }
                )
            }
        )
        route == TabScreen.TASTY.route -> ScaffoldConfig(
            title = "Tasty",
            showTopBar = true,
            showBottomBar = true,
            isCenterAligned = true
        )

        route == TabScreen.MAP.route -> ScaffoldConfig(
            showTopBar = false,
            showBottomBar = true,
            isCenterAligned = true
        )
        route == TabScreen.MY_PAGE.route -> ScaffoldConfig(
            title = "마이 페이지",
            showTopBar = true,
            showBottomBar = true,
            isCenterAligned = true,
            topBarActions = listOf(
                AppBarAction(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = "더보기",
                    onActionClick = {} // 상세 동작은 Screen에서 Override됨
                )
            )
        )


        route == Screen.AUTH_ON_BOARDING.route ||
        route == Screen.AUTH_LOGIN.route ||
        route == Screen.AUTH_SIGN_UP_EMAIL_PWD.route ||
        route == Screen.AUTH_SIGN_UP_SET_PROFILE.route -> ScaffoldConfig(
            showTopBar = false,
            showBottomBar = false
        )


        route.startsWith(Screen.FEED_DETAIL.route) -> ScaffoldConfig(
            title = "게시글 상세",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )
        route == Screen.FEED_CREATE_FEED.route -> ScaffoldConfig(
            title = "게시글 작성",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true,
            topBarActions = listOf(
                AppBarAction(
                    icon = Icons.Outlined.Send,
                    contentDescription = "게시",
                    onActionClick = {}
                )
            )
        )
        route == Screen.FEED_SEARCH_RESTAURANT.route -> ScaffoldConfig(
            title = "식당 검색",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )
        route.startsWith("tasty_detail") || route.startsWith(Screen.TASTY_DETAIL.route) -> ScaffoldConfig(
            title = "Tasty 상세",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true,
            isCenterAligned = false
        )
        route.startsWith("user_profile") || route.startsWith(Screen.USER_PROFILE.route.split("/")[0]) -> ScaffoldConfig(
            showTopBar = true,
            showBottomBar = true,
            containsBackButton = true,
            isCenterAligned = true
        )
        route == Screen.MY_PAGE_EDIT_PROFILE.route -> ScaffoldConfig(
            title = "프로필 수정",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )
        route.startsWith("edit_tasty_list") -> ScaffoldConfig(
            title = "리스트 수정",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )
        route == Screen.MY_PAGE_SELECT_FEEDS.route -> ScaffoldConfig(
            title = "피드 선택",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )
        route == Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route -> ScaffoldConfig(
            title = "리스트 설정",
            showTopBar = true,
            showBottomBar = false,
            containsBackButton = true
        )

        else -> ScaffoldConfig()
    }
}

@Composable
fun FeedFab(
    onWriteClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
        modifier = Modifier.navigationBarsPadding()
    ) {
        FloatingActionButton(
            onClick = onWriteClick,
            containerColor = PrimaryColor,
            contentColor = TextColor
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "게시글 작성"
            )
        }

        FloatingActionButton(
            onClick = onFilterClick,
            containerColor = PrimaryColor,
            contentColor = TextColor
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "필터"
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScaffold(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 루트 바뀔 때마다 스크롤 상태 초기화
    LaunchedEffect(currentRoute) {
        scrollBehavior.state.contentOffset = 0f
        scrollBehavior.state.heightOffset = 0f
    }


    val predictiveConfig = remember(currentRoute) {
        getPredictiveConfig(currentRoute, navController)
    }

    var screenOverride by remember { mutableStateOf<ScaffoldConfig?>(null) }

    LaunchedEffect(currentRoute) {
        screenOverride = null
    }

    val activeConfig = screenOverride ?: predictiveConfig

    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        modifier = if (activeConfig.showTopBar) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else Modifier,
        topBar = {
            if (activeConfig.showTopBar) {
                CustomTopAppBar(
                    title = activeConfig.title,
                    containsBackButton = activeConfig.containsBackButton,
                    onBackClick = activeConfig.onBackClick,
                    actions = activeConfig.topBarActions,
                    scrollBehavior = scrollBehavior,
                    containerColor = activeConfig.containerColor,
                    isCenterAligned = activeConfig.isCenterAligned
                )
            }
        },
        bottomBar = {
            if (activeConfig.showBottomBar) {
                CustomBottomAppBar(navController)
            }
        },
        floatingActionButton = {
            activeConfig.floatingActionButton?.invoke()
        },
        containerColor = Color.White
    ) { innerPadding ->

        val contentPadding = if (activeConfig.showBottomBar) {
            innerPadding
        } else {
            PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = 0.dp
            )
        }

        CustomNavHost(
            navController = navController,
            modifier = Modifier.padding(contentPadding),
            onScaffoldConfigChange = { newConfig ->
                screenOverride = newConfig
            }
        )
    }
}
