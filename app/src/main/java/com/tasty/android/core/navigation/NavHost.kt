package com.tasty.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.feature.auth.LoginScreen
import com.tasty.android.feature.auth.OnboardingScreen
import com.tasty.android.feature.auth.OnboardingViewModel
import com.tasty.android.feature.auth.ProfileSetupScreen
import com.tasty.android.feature.auth.SignUpScreen
import com.tasty.android.feature.feed.FeedDetailScreen
import com.tasty.android.feature.feed.FeedScreen
import com.tasty.android.feature.feed.FeedWriteScreen
import com.tasty.android.feature.mypage.MyPageScreen

@Composable
fun CustomNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    // 로그인 상태 여부 확인
    val isLoggedIn = false
    // 로그인 상태에 따라 첫 화면 동적으로 결정
    val startDestination = if(isLoggedIn) TabScreen.FEED.route else Screen.AUTH_ON_BOARDING.route
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
        composable(Screen.FEED_DETAIL.route) {
            FeedDetailScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
        }
        composable(Screen.FEED_CREATE_FEED.route) {
            FeedWriteScreen(
                navController = navController,
                onScaffoldConfigChange = onScaffoldConfigChange
            )
            // 피드 생성 화면 컴포저블
        }
        composable(Screen.FEED_SEARCH_RESTAURANT.route) {
            // 피드 생성 시 식당 검색 화면 컴포저블

        }

        /** Tasty **/
        composable(TabScreen.TASTY.route) {
            // 테이스티 화면 컴포저블(TAB)
        }
        composable(Screen.TASTY_DETAIL.route) {
            // 테이스티 상세 화면 컴포저블
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
            // 마이페이지 테이스티 리스트용 피드 선택 화면 컴포저블
        }
        composable(Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route) {
            // 마이페이지 테이스티 리스트용 썸네일/제목 설정 화면 컴포저블
        }
        composable(Screen.MY_PAGE_EDIT_PROFILE.route) {
            // 마이페이지 프로필 수정 화면 컴포저블
        }

        /** User Profile **/
        composable(Screen.USER_PROFILE.route) {
            // 유저 프로필 화면 컴포저블
        }
    }
}