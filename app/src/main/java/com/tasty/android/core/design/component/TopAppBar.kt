package com.tasty.android.core.design.component

import android.accessibilityservice.GestureDescription
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor

// 앱바 액션 데이터 클래스 정의
data class AppBarAction(
    val onActionClick: () -> Unit,
    val icon: ImageVector,
    val contentDescription: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    title: String = "",  // 제목
    appIcon: ImageVector? = null, // 앱 아이콘(선택)
    containsBackButton: Boolean = false, // 백 버튼 포함 여부 true/false| *기본 false
    onBackClick: () -> Unit = {}, // 백 버튼 클릭 시 이벤트 함수 주입
    // 스크롤 시 행동 주입
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: List<AppBarAction> = emptyList(), // 앱 바 액션 리스트,
    containerColor: Color = PrimaryColor, // 컨테이너 컬러 설정
    isCenterAligned: Boolean = false // 타이틀 센터 정렬 여부
) {
    if (isCenterAligned) {
        CenterAlignedTopAppBar(
            title = @Composable {
                Row(
                    verticalAlignment = Alignment.CenterVertically // 반대축 가운데 정렬
                ) {
                    appIcon?.let{icon -> // 로고 아이콘
                        Icon(
                            imageVector = icon,
                            contentDescription = "앱 로고 아이콘",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text( // 제목
                        text = title
                    )
                }
            },
            // 뒤로 가기 네비게이션 버튼
            navigationIcon = {
                if (containsBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back btn"
                        )
                    }
                }
            },
            // 색상 정의
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                navigationIconContentColor = TextColor,
                titleContentColor = TextColor,
                scrolledContainerColor = PrimaryColor,
                actionIconContentColor = TextColor
            ),
            // 상단 액션 정의
            actions = {
                actions.forEach { action ->
                    IconButton(
                        onClick = action.onActionClick
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = @Composable {
                Row(
                    verticalAlignment = Alignment.CenterVertically // 반대축 가운데 정렬
                ) {
                    appIcon?.let{icon -> // 로고 아이콘
                        Icon(
                            imageVector = icon,
                            contentDescription = "앱 로고 아이콘",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text( // 제목
                        text = title
                    )
                }
            },
            // 뒤로 가기 네비게이션 버튼
            navigationIcon = {
                if (containsBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back btn"
                        )
                    }
                }
            },
            // 색상 정의
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                navigationIconContentColor = TextColor,
                titleContentColor = TextColor,
                scrolledContainerColor = PrimaryColor,
                actionIconContentColor = TextColor
            ),
            // 상단 액션 정의
            actions = {
                actions.forEach { action ->
                    IconButton(
                        onClick = action.onActionClick
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.contentDescription
                        )
                    }
                }
            },
            scrollBehavior = scrollBehavior
        )
    }

}
