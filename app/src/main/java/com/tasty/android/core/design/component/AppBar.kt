package com.tasty.android.core.design.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.tasty.android.core.design.theme.MainColor
import com.tasty.android.core.design.theme.TextColor


// 상단 앱바 컴포저블, 예: 피드화면의 테이스티 리스트 상단 앱바
@Composable
fun BuildTitleAppBar() {

}


// 피드 화면에서의 테이스티 리스트 앱바
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildTastyListAppBar(tastyList: List<Unit>, context: Context) {
    Column() {
        // 상단 Title AppBar
        BuildTitleAppBar()
        // 하단 테이스티 리스트
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MainColor),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tastyList.size){ idx ->
                TastyItemButton(
                    tastyItem = tastyList[idx],
                    context = context
                )
            }
        }
    }
}

// 일단 tastyListItem으로 들어오는 객체는 Unit으로 선언 3/23
@Composable
fun TastyItemButton(tastyItem: Unit, context: Context) { // 테이스티 리스트 객체
    // val nickname = tastyItem.nickname
    // val imageUrl = tastyItem.imageUrl
    // val title = tastyItem.title
    Button(
        onClick = {}
    ) {
        Column(
        ) {
            // 아이템 상단 닉네임 섹션
            Text(
                text = "", // nickname
                style = typography.labelSmall,
                color = TextColor,
            )
            // 썸네일 이미지 로드
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("imageUrl") // imageUrl
                    .crossfade(true)
                    .build(),
                contentDescription = ""
            )
            Text(
                text = "", // title
                style = typography.labelMedium,
                color = TextColor,
            )
        }
    }
}

// 테이스티 리스트 미리보기 함수
@Preview(showBackground = true, name = "테이스티 리스트 미리보기", device = "")
@Composable
fun TastyItemButtonPreview() { // 테이스티 리스트 객체
    // val nickname = tastyItem.nickname
    // val imageUrl = tastyItem.imageUrl
    // val title = tastyItem.title
    Button(
        onClick = {
        }
    ) {
        Column(
        ) {
            // 아이템 상단 닉네임 섹션
            Text(
                text = "", // nickname
                style = typography.labelSmall,
                color = TextColor,
            )
            // 썸네일 이미지 로드
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("imageUrl") // imageUrl
                    .crossfade(true)
                    .build(),
                contentDescription = ""
            )
            Text(
                text = "", // title
                style = typography.labelMedium,
                color = TextColor,
            )
        }
    }
}