package com.tasty.android.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.feature.auth.LoginScreen
import com.tasty.android.feature.auth.OnboardingScreen
import com.tasty.android.feature.auth.ProfileSetupScreen
import com.tasty.android.feature.auth.SignUpScreen
import com.tasty.android.feature.feed.FeedDetailScreen
import com.tasty.android.feature.feed.FeedScreen
import com.tasty.android.feature.feed.FeedSearchRestaurantScreen
import com.tasty.android.feature.feed.FeedWriteScreen
import com.tasty.android.feature.feed.FeedWriteViewModel
import com.tasty.android.feature.mypage.EditProfileScreen
import com.tasty.android.feature.mypage.MyPageScreen
import com.tasty.android.feature.tasty.TastyDetailScreen
import com.tasty.android.feature.tasty.TastyScreen
import com.tasty.android.feature.tastylist.TastyListCreateSelectFeedsScreen
import com.tasty.android.feature.tastylist.TastyListCreateSetupScreen
import com.tasty.android.feature.mypage.tastylist.EditTastyListScreen
import com.tasty.android.feature.vmfactory.FeedWriteViewModelFactory
import com.tasty.android.feature.profile.UserProfileScreen
import com.tasty.android.feature.tastymap.TastyMapScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    // 로그인 상태 여부 확인
    val isLoggedIn = Firebase.auth.currentUser != null
    // 로그인 상태에 따라 첫 시작 그래프를 결정
    val startDestination = if(isLoggedIn) "feed_graph" else "auth_graph"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        /**AUTH GRAPH **/
        navigation(
            startDestination = Screen.AUTH_ON_BOARDING.route,
            route = "auth_graph"
        ) {
            // 온보딩
            composable(Screen.AUTH_ON_BOARDING.route) {
                OnboardingScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }
            // 이메일/패스워드 입력
            composable(Screen.AUTH_SIGN_UP_EMAIL_PWD.route) {
                SignUpScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }
            // 닉네임 입력
            composable(Screen.AUTH_SIGN_UP_SET_PROFILE.route) {
                ProfileSetupScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }
            // 로그인 화면
            composable(Screen.AUTH_LOGIN.route) {
                LoginScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }
        }

        /** FEED GRAPH **/
        navigation(
            startDestination = TabScreen.FEED.route,
            route = "feed_graph"
        ) {
            // 피드 홈
            composable(TabScreen.FEED.route) {
                FeedScreen(
                    navController,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 맵 인자: 플레이스 아이디
            composable(
                route = "${Screen.FEED_MAP.route}?placeId={placeId}",
                arguments = listOf(
                    navArgument("placeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val placeId = backStackEntry.arguments?.getString("placeId")
                TastyMapScreen(
                    navController = navController,
                    onScaffoldConfigChange = onScaffoldConfigChange,
                    initialRestaurantId = placeId
                )
            }
            // 피드 디테일 인자: 피드 아이디
            composable(
                route = "${Screen.FEED_DETAIL.route}/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.StringType })
            ) { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 작성
            composable(Screen.FEED_CREATE_FEED.route) { backStackEntry ->
                val viewModel: FeedWriteViewModel = viewModel(backStackEntry, factory = FeedWriteViewModelFactory)
                FeedWriteScreen(
                    navController,
                    viewModel = viewModel,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 작성-식당 검색
            composable(Screen.FEED_SEARCH_RESTAURANT.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.FEED_CREATE_FEED.route)
                }
                val sharedViewModel: FeedWriteViewModel = viewModel(
                    parentEntry,
                    factory = FeedWriteViewModelFactory
                )

                FeedSearchRestaurantScreen(
                    navController = navController,
                    viewModel = sharedViewModel,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 - 유저 프로필
            composable(
                route = "${Screen.USER_PROFILE.route}/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController = navController,
                    targetUserId = userId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 테이스티 상세 인자: 테이스티 아이디
            composable(
                route = "${Screen.TASTY_DETAIL.route}/{tastyId}",
                arguments = listOf(navArgument("tastyId") { type = NavType.StringType })
            ) {
                it.arguments?.getString("tastyId")
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { tastyId -> navController.navigate("${Screen.FEED_DETAIL.route}/$tastyId") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
        }

        /** TASTY GRAPH **/
        navigation(
            startDestination = TabScreen.TASTY.route,
            route = "tasty_graph"
        ) {
            // 테이스티
            composable(TabScreen.TASTY.route) {
                TastyScreen(
                    onClickTastyItem = { id -> navController.navigate("${Screen.TASTY_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 테이스티 상세 인자: 테이스티 아이디
            composable(
                route = "${Screen.TASTY_DETAIL.route}/{tastyId}",
                arguments = listOf(navArgument("tastyId") { type = NavType.StringType })
            ) {
                it.arguments?.getString("tastyId")
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { tastyId -> navController.navigate("${Screen.FEED_DETAIL.route}/$tastyId") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 상세 인자: 피드 아이디
            composable(
                route = "${Screen.FEED_DETAIL.route}/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.StringType })
            ) { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 유저 프로필 인자: 유저 아이디
            composable(
                route = "${Screen.USER_PROFILE.route}/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController = navController,
                    targetUserId = userId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 맵 인자: 플레이스 아이디
            composable(
                route = "${Screen.TASTY_MAP.route}?placeId={placeId}",
                arguments = listOf(
                    navArgument("placeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val placeId = backStackEntry.arguments?.getString("placeId")
                TastyMapScreen(
                    navController = navController,
                    onScaffoldConfigChange = onScaffoldConfigChange,
                    initialRestaurantId = placeId
                )
            }
        }

        /** MAP GRAPH **/
        navigation(
            startDestination = "${TabScreen.MAP.route}?placeId={placeId}",
            route = "map_graph"
        ) {
            // 맵 인자: 플레이스 아이디
            composable(
                route = "${TabScreen.MAP.route}?placeId={placeId}",
                arguments = listOf(
                    navArgument("placeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val placeId = backStackEntry.arguments?.getString("placeId")
                TastyMapScreen(
                    navController = navController,
                    onScaffoldConfigChange = onScaffoldConfigChange,
                    initialRestaurantId = placeId
                )
            }
            // 유저 프로필 인자: 유저 아이디
            composable(
                route = "${Screen.USER_PROFILE.route}/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController = navController,
                    targetUserId = userId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 상세 인자: 피드 아이디
            composable(
                route = "${Screen.FEED_DETAIL.route}/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.StringType })
            ) { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 테이스티 디테일 인자: 테이스티 아이디
            composable(
                route = "${Screen.TASTY_DETAIL.route}/{tastyId}",
                arguments = listOf(navArgument("tastyId") { type = NavType.StringType })
            ) { backStackEntry ->
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { id -> navController.navigate("${Screen.FEED_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
        }

        /** MY PAGE GRAPH **/
        navigation(
            startDestination = TabScreen.MY_PAGE.route,
            route = "my_page_graph"
        ) {
            // 마이페이지
            composable(TabScreen.MY_PAGE.route) {
                MyPageScreen(navController, onScaffoldConfigChange)
            }
            // 유저 프로필 인자: 유저 아이디
            composable(
                route = "${Screen.USER_PROFILE.route}/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController = navController,
                    targetUserId = userId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 피드 선택
            composable(Screen.MY_PAGE_SELECT_FEEDS.route) {
                TastyListCreateSelectFeedsScreen(navController, onScaffoldConfigChange)
            }
            // 제목/썸네일 입력
            composable(Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route) {
                TastyListCreateSetupScreen(navController, onScaffoldConfigChange)
            }
            // 프로필 수정
            composable(Screen.MY_PAGE_EDIT_PROFILE.route) {
                EditProfileScreen(navController, onScaffoldConfigChange)
            }
            // 테이스티 리스트 수정
            composable(
                route = "${Screen.EDIT_TASTY_LIST.route}/{tastyListId}",
                arguments = listOf(navArgument("tastyListId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tastyListId = backStackEntry.arguments?.getString("tastyListId") ?: ""
                EditTastyListScreen(
                    tastyListId = tastyListId,
                    navController = navController,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
            // 테이스티 디테일 인자: 테이스티 아이디
            composable(
                route = "${Screen.TASTY_DETAIL.route}/{tastyId}",
                arguments = listOf(navArgument("tastyId") { type = NavType.StringType })
            ) {
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { id -> navController.navigate("${Screen.FEED_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            // 피드 상세 인자: 피드 아이디
            composable(
                route = "${Screen.FEED_DETAIL.route}/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.StringType })
            ) { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            // 맵 인자: 플레이스 아이디
            composable(
                route = "${Screen.MY_PAGE_MAP.route}?placeId={placeId}",
                arguments = listOf(
                    navArgument("placeId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val placeId = backStackEntry.arguments?.getString("placeId")
                TastyMapScreen(
                    navController = navController,
                    onScaffoldConfigChange = onScaffoldConfigChange,
                    initialRestaurantId = placeId
                )
            }
        }
    }
}