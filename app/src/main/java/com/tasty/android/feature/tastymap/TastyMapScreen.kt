package com.tasty.android.feature.tastymap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tasty.android.core.design.component.ScaffoldConfig

@Composable
fun TastyMapScreen(
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListBottomSheet() {
    // 아이템 개수가 바뀔 때마다 UI 업데이트
    var restaurantCount by remember { mutableIntStateOf(10) }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 350.dp,
        // 커스텀 핸들러 추가 (회색 바 역할)
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            // 바텀 시트 내부 리스트
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 리스트 내부 아이템 생성
                items(restaurantCount) { index ->
                    RestaurantItem(index + 1)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE0E0E0)) // 지도 백그라운드 대용
        ) {
        }
    }
}

@Composable
fun RestaurantItem(rank: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 이름 및 영업 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank. 란 짠타부리",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "영업중",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        // 위치
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "송파구, 문정 법조타운 920m",
            color = Color.Gray,
            fontSize = 14.sp
        )

        // 부가 정보 (평점, 리뷰 개수)
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            InfoText(text = "평점 4.6, 리뷰 55개")
        }

        // 음식 사진 가로 리스트
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F2))
                )
            }
        }
    }
}

@Composable
fun InfoText(text: String) {
    Text(
        text = text,
        color = Color(0xFF757575),
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}