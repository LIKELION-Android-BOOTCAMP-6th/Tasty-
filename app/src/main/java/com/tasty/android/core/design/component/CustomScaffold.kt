package com.tasty.android.core.design.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tasty.android.core.navigation.CustomNavHost
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen
import kotlin.collections.setOf

// 스캐폴드 커스텀 설정 클래스 선언
data class ScaffoldConfig(
    val title: String = "Tasty",
    val showTopBar: Boolean = true, // TopBar 표시 여부
    val showBottomBar: Boolean = true, // BottomBar 표시 여부
    val containsBackButton: Boolean = false, // 뒤로가기 버튼 여부
    val onBackClick: () -> Unit = {}, // 뒤로가기 버튼 이벤트 등록
    val topBarActions: List<AppBarAction> = emptyList(), // 상단 앱바 액션 리스트 등록
    val floatingActionButton: (@Composable () -> Unit)? = null // null = No FAB
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScaffold(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var config by remember {
        mutableStateOf(ScaffoldConfig())
    }
    Scaffold(
        // TopBar 숨김 여부(Config)에 따라 Modifier 동적 결정
        modifier = if (config.showTopBar) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else Modifier,
        topBar = {
            // 현재 화면이 상단 앱바가 없는 루트에 포함되면 CustomAppBar 미호출
            // 현재 화면이 상단 검색 바 루트에 포함되면 CustomSearchBar 호출
            if (config.showTopBar) {
                CustomTopAppBar(
                    title = config.title,
                    containsBackButton = config.containsBackButton,
                    onBackClick = config.onBackClick,
                    actions = config.topBarActions,
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (config.showBottomBar) {
                CustomBottomAppBar(navController)
            }
        },
        floatingActionButton = {
            config.floatingActionButton?.invoke()
        }
    ) { innerPadding ->
        CustomNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onScaffoldConfigChange = {config = it}
        )
    }
}