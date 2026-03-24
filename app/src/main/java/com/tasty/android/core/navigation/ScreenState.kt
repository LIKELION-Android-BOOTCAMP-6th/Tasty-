package com.tasty.android.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

// 화면 상태 정의
enum class ScreenState(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    FEED("feed","피드", Icons.Filled.Home, inactiveIcon = Icons.Outlined.Home),
    // 아래 아이콘은 추후 extended 아이콘 main에 Merge 후에 라이브러리 추가 후 변경
    TASTY_LIST("tasty_list","테이스티 리스트", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow),
    MAP("map","지도", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    MY_PAGE("my_page","마이페이지", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}