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

// 화면 상태 정의
enum class ScreenState(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    FEED("피드", Icons.Filled.Home, inactiveIcon = Icons.Outlined.Home),
    // 아래 아이콘은 추후 extended 아이콘 main에 Merge 후에 라이브러리 추가 후 변경
    TASTY_LIST("테이스티 리스트", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow),
    MAP("지도", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    MY_PAGE("마이페이지", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun CustomBottomAppBar(navController: NavController) {
    // 현재 네비게이션 백 스택 상태 추적
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 화면 정보 정의
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        ScreenState.entries.forEach{ screenState ->
            // 현재 화면 경로(루트) 확인
            val isSelected = currentDestination?.hierarchy?.any{navDestination ->
                navDestination.route == screenState.name
            } == true
            // 네비게이션 아이템 정의
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // 해당 스크린으로 이동
                    navController.navigate(screenState.name) {
                        popUpTo(navController.graph.findStartDestination()) {
                            saveState = true // 스크린 이전 상태 저장
                        }
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
                    indicatorColor = Color.White
                )
            )
        }
    }
}