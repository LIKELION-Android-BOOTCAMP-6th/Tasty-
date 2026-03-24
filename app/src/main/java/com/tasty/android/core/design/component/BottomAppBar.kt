package com.tasty.android.core.design.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.navigation.ScreenState


// 바텀 앱 바 컴포저블
@Composable
fun CustomBottomAppBar(navController: NavController) {
    // 현재 네비게이션 백 스택 상태 추적
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 화면 정보 정의
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = PrimaryColor
    ) {
        ScreenState.entries.forEach{ screenState ->
            // 현재 화면 경로(루트) 확인
            val isSelected = currentDestination?.hierarchy?.any{navDestination ->
                navDestination.route == screenState.route
            } == true
            // 네비게이션 아이템 정의
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // 해당 스크린으로 이동
                    navController.navigate(screenState.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true // 스크린 이전 상태 저장
                        }
                        launchSingleTop = true // 중복 스택 방지
                        restoreState = true // 스크린 이전 상태 복구
                    }
                },

                icon = { // 바텀 네비게이션바 아이콘
                    Icon(
                        imageVector = if (isSelected) screenState.activeIcon else screenState.inactiveIcon,
                        contentDescription = screenState.label
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