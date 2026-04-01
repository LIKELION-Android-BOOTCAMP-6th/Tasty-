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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.feature.auth.LoginScreen
import com.tasty.android.feature.auth.OnboardingScreen
import com.tasty.android.feature.auth.OnboardingViewModel
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {


    // 로그인 상태 여부 확인
    val isLoggedIn = Firebase.auth.currentUser != null
    // 로그인 상태에 따라 첫 화면 동적으로 결정
    val startDestination = if(isLoggedIn) TabScreen.FEED.route else Screen.AUTH_ON_BOARDING.route
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        /** AUTH **/
        composable(Screen.AUTH_ON_BOARDING.route) {
            // 온보딩 화면 컴포저블
            OnboardingScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.AUTH_SIGN_UP_EMAIL_PWD.route) {
            // 회원가입 이메일/비밀번호 설정 화면 컴포저블
            SignUpScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.AUTH_SIGN_UP_SET_PROFILE.route) {
            // 회원가입 프로필 정보(닉네임 등) 설정 화면 컴포저블
            ProfileSetupScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.AUTH_LOGIN.route) {
            // 로그인 화면 컴포저블
            LoginScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }

        /** FEED **/
        composable(TabScreen.FEED.route) {
            FeedScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )  // 피드 메인 화면 컴포저블(TAB)
        }
        composable("${Screen.FEED_DETAIL.route}/{feedId}") {backStackEntry ->
            val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
            FeedDetailScreen(
                navController = navController,
                feedId = feedId,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.FEED_CREATE_FEED.route) {backStackEntry ->
            val viewModel: FeedWriteViewModel = viewModel(
                backStackEntry,
                factory = FeedWriteViewModelFactory
            )
            FeedWriteScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange,
                viewModel = viewModel
            )
            // 피드 생성 화면 컴포저블
        }
        composable(Screen.FEED_SEARCH_RESTAURANT.route) { backStackEntry ->

            val parentEntry = remember( backStackEntry) {
                navController.getBackStackEntry(Screen.FEED_CREATE_FEED.route)
            }
            val sharedViewModel: FeedWriteViewModel = viewModel(
                parentEntry,
                factory = FeedWriteViewModelFactory
            )

            FeedSearchRestaurantScreen(
                navController = navController,
                viewModel = sharedViewModel,
                onScaffoldConfigChange
            )
            // 피드 생성 시 식당 검색 화면 컴포저블

        }

        /** Tasty **/
        composable(TabScreen.TASTY.route) {
            TastyScreen(
                onClickTastyItem = { tastyId ->
                    navController.navigate("${Screen.TASTY_DETAIL.route}/$tastyId")
                },
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }

        composable("${Screen.TASTY_DETAIL.route}/{tastyId}") { backStackEntry ->
            val tastyId = backStackEntry.arguments?.getString("tastyId") ?: ""
            TastyDetailScreen(
                onBackClick = { navController.popBackStack() },
                onClickFeed = { feedId ->
                    navController.navigate("${Screen.FEED_DETAIL.route}/$feedId")
                },
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }

        /** Map **/
        composable(TabScreen.MAP.route){
            // 맵 화면 컴포저블(TAB)
        }
        composable(Screen.MAP_SEARCH_LOCATION.route){
            // 맵 지역 검색 화면 컴포저블
        }
        composable(Screen.MAP_RESTAURANT_DETAIL.route){
            // 맵 식당 세부 화면 컴포저블
        }

        /** My Page **/
        composable(TabScreen.MY_PAGE.route) {
            MyPageScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
            // 마이페이지 메인 화면 컴포저블(TAB)
        }
        composable(Screen.MY_PAGE_SELECT_FEEDS.route) {
            TastyListCreateSelectFeedsScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route) {
            TastyListCreateSetupScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.MY_PAGE_EDIT_PROFILE.route) {
            EditProfileScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.EDIT_TASTY_LIST.route) { backStackEntry ->
            val tastyListId = backStackEntry.arguments?.getString("tastyListId") ?: ""
            EditTastyListScreen(
                tastyListId = tastyListId,
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }

        /** User Profile **/
        composable(Screen.USER_PROFILE.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                navController = navController,
                targetUserId = userId,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
    }
}