package com.tasty.android.core.design.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen
import okhttp3.internal.immutableListOf

val feedRootList = immutableListOf(
    TabScreen.FEED.route, // 피드 홈
    Screen.USER_PROFILE.route, // 유저 프로필
)

val tastyRootList = immutableListOf(
    TabScreen.TASTY.route, // 테이스티 홈
    Screen.TASTY_DETAIL.route, // 테이스티 상세
)

// 바텀 앱 바 컴포저블
@Composable
fun CustomBottomAppBar(navController: NavHostController) {
    // 현재 네비게이션 백 스택 상태 추적
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 화면 정보 정의
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = PrimaryColor,
        modifier = Modifier.height(60.dp).background(Color.Transparent),
        windowInsets = WindowInsets(0)
    ) {
        TabScreen.entries.forEach{ tabScreen ->

            val isSelected = currentDestination?.hierarchy?.any { navDestination ->
                navDestination.route == "${tabScreen.route}_graph"
            } == true

            // 네비게이션 아이템 정의
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // 해당 탭 그래프의 루트로 이동
                    navController.navigate("${tabScreen.route}_graph") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true // 이전 상태 저장
                        }
                        launchSingleTop = true // 중복 생성 방지
                        restoreState = true // 이전 상태 복원
                    }
                },

                icon = { // 바텀 네비게이션바 아이콘
                    Icon(
                        imageVector = if (isSelected) tabScreen.activeIcon else tabScreen.inactiveIcon,
                        contentDescription = tabScreen.label
                    )
                },
                // 네비게이션바 아이콘 색상 정의
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Black,
                    indicatorColor = Color.White,
                )
            )
        }
    }
}