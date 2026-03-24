package com.tasty.android.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasty.android.core.design.theme.MyApplicationTheme

/*import com.tasty.android.core.design.component.CustomTopAppBar
import com.tasty.android.core.design.component.CustomBottomAppBar*/

@Composable // 이 함수가 Compose UI라는 의미
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(), // ViewModel 가져오기 (상태 관리)
    onAddPostClick: () -> Unit = {},        // 글 작성 버튼
    onFilterClick: () -> Unit = {},         // 필터 버튼
    onHomeClick: () -> Unit = {},           // 하단 홈
    onListClick: () -> Unit = {},           // 리스트
    onMapClick: () -> Unit = {},            // 지도
    onMyPageClick: () -> Unit = {},         // 마이페이지
    onFeedDetailClick: (Int) -> Unit = {},  // 피드 클릭 시 상세 이동
    onProfileClick: (String) -> Unit = {}   // 프로필 클릭
) {
    // ViewModel의 상태를 Compose에서 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeedScreenContent(
        uiState = uiState,
        onAddPostClick = onAddPostClick,
        onFilterClick = onFilterClick,
        onHomeClick = onHomeClick,
        onListClick = onListClick,
        onMapClick = onMapClick,
        onMyPageClick = onMyPageClick,
        onFeedDetailClick = onFeedDetailClick,
        onProfileClick = onProfileClick
    )
}

@Composable
private fun FeedScreenContent(
    uiState: FeedUiState,
    onAddPostClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onListClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onMyPageClick: () -> Unit = {},
    onFeedDetailClick: (Int) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    Scaffold( // 화면 기본 구조 (상단, 하단, FAB 포함)
/*     topBar = {
            // Scaffold의 상단 영역에 들어갈 UI를 정의하는 자리
            // 여기 안에 넣은 Composable이 화면 맨 위에 고정됨
            CustomTopAppBar()
        },
        bottomBar = {
            CustomBottomAppBar(
                 onHomeClick = onHomeClick,
                 onListClick = onListClick,
                 onMapClick = onMapClick,
                 onMyPageClick = onMyPageClick
            )
        },*/

        // 배경 색
        containerColor = Color(0xFFF8F8F8),

        // 오른쪽 아래 버튼 (플로팅 버튼 영역)
        floatingActionButton = {
            Column( // 버튼 2개를 세로로 배치
                verticalArrangement = Arrangement.spacedBy(12.dp), // 버튼 간격
                horizontalAlignment = Alignment.End, // 오른쪽 정렬
                modifier = Modifier.navigationBarsPadding() // 하단 네비게이션 영역 피하기
            ) {

                // ➕ 게시글 작성 버튼
                FloatingActionButton(
                    onClick = onAddPostClick,
                    containerColor = Color(0xFFC9E4B7),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Add, contentDescription = "게시글 작성")
                }

                // ⚙️ 필터 버튼
                FloatingActionButton(
                    onClick = onFilterClick,
                    containerColor = Color(0xFFC9E4B7),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "필터")
                }
            }
        }
    ) { innerPadding -> // Scaffold 내부 패딩 (상단/하단 바 공간)

        LazyColumn( // 세로 스크롤 리스트
            modifier = Modifier
                .fillMaxSize() // 화면 꽉 채움
                .padding(innerPadding), // Scaffold 패딩 적용
            contentPadding = PaddingValues(bottom = 24.dp) // 하단 여백
        ) {

            // 🔹 상단 Tasty 리스트 영역
            item {
                FeedHeaderSection(
                    title = "Tasty 리스트",
                    tastyLists = uiState.tastyLists
                )
            }

            // 🔹 카드 위 간격
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 🔹 피드 리스트 반복
            items(uiState.feedPosts) { post ->
                FeedCard(
                    post = post,
                    userRegion = uiState.userRegion,
                    onClick = { onFeedDetailClick(post.id) },
                    onProfileClick = { onProfileClick(post.authorName) }
                )
            }
        }
    }
}

@Composable
private fun FeedHeaderSection(
    title: String,
    tastyLists: List<TastyListUiModel>
) {
    Column( // 세로 레이아웃
        modifier = Modifier
            .fillMaxWidth() // 가로 꽉 채움
            .background(Color(0xFFC9E4B7)) // 배경색
            .padding(vertical = 16.dp) // 위아래 패딩
    ) {

        // "Tasty 리스트" 제목
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.CenterHorizontally) // 가운데 정렬
        )

        Spacer(modifier = Modifier.height(12.dp)) // 간격

        // 가로 스크롤 리스트
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp), // 아이템 간격
            contentPadding = PaddingValues(horizontal = 16.dp) // 좌우 여백
        ) {
            items(tastyLists) { item ->

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // 지역 이름
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 동그란 썸네일 자리
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape) // 원형
                            .background(Color(0xFFE8E8E8))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 설명 텍스트
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    post: FeedPostUiModel,
    userRegion: String,
    onClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card( // 카드 컨테이너
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // 바깥 여백
        shape = RoundedCornerShape(20.dp), // 둥근 모서리
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC9E4B7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF3B3B3B), RoundedCornerShape(20.dp)) // 테두리
                .clip(RoundedCornerShape(20.dp)) // 카드 모양 유지
        ) {

            // 🔹 상단 프로필 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 프로필 이미지 자리
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9D9D9))
                )

                Spacer(modifier = Modifier.size(10.dp))

                // 지역 이름
                Text(
                    text = userRegion,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f) // 남은 공간 차지
                )
            }

            // 🔹 이미지 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f) // 비율 유지
                    .background(Color(0xFFB7B7B7))
            ) {
                Text(
                    text = "피드 이미지",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 🔹 하단 정보 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {

                // 좋아요 / 댓글 영역
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(end = 14.dp)
                ) {

                    Icon(Icons.Default.FavoriteBorder, contentDescription = "좋아요")
                    Text(text = post.likeCount.toString())

                    Spacer(modifier = Modifier.height(8.dp))

                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "댓글")
                    Text(text = post.commentCount.toString())
                }

                // 오른쪽 정보 영역
                Column(modifier = Modifier.weight(1f)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(Icons.Default.Place, contentDescription = "장소")

                        Spacer(modifier = Modifier.size(4.dp))

                        // 식당 이름
                        Text(
                            text = post.placeName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        // 별점 표시
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "별점",
                                tint = if (index < post.rating.toInt())
                                    Color.Black else Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row {

                        // 주소
                        Text(
                            text = post.address,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.size(8.dp))

                        // 날짜
                        Text(text = post.date)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 설명 텍스트
                    Text(
                        text = post.description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedScreenPreview() {
    MyApplicationTheme {
        FeedScreenContent(
            uiState = FeedUiState(
                userRegion = "홍길동동",
                tastyLists = listOf(
                    TastyListUiModel(1, "홍길동동", "강원 맛집 모음"),
                    TastyListUiModel(2, "홍길동동", "강원 맛집 모음"),
                    TastyListUiModel(3, "홍길동동", "강원 맛집 모음"),
                    TastyListUiModel(4, "홍길동동", "강원 맛집 모음")
                ),
                feedPosts = listOf(
                    FeedPostUiModel(
                        id = 1,
                        authorName = "홍길동동",
                        authorRegion = "홍길동동",
                        placeName = "길동이네 식당",
                        address = "송파구 XX동 XX로 1...",
                        date = "2026-08-06",
                        likeCount = 500,
                        commentCount = 500,
                        rating = 5f,
                        description = "여기 진짜 맛있는 집 같아요. 분위기부터 근데 점자가 너무 많고 글씨 완전 작음 약간 광고충 불러놓음...",
                        imageUrl = null
                    )
                )
            )
        )
    }
}
