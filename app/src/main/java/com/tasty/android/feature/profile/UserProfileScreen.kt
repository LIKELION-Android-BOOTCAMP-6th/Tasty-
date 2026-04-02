package com.tasty.android.feature.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen
import com.tasty.android.feature.mypage.*
import com.tasty.android.feature.vmfactory.UserProfileViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun UserProfileScreen(
    navController: NavHostController,
    targetUserId: String,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {

    val viewModel: UserProfileViewModel = viewModel(
        key = targetUserId,
        factory = UserProfileViewModelFactory(targetUserId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> viewModel.selectTab(MyPageTab.FEED)
            1 -> viewModel.selectTab(MyPageTab.TASTY_LIST)
        }
    }

    LaunchedEffect(uiState.userHandle) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "@${uiState.userHandle}",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() },
                isCenterAligned = true
            )
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 프로필 헤더 (팔로우 버튼 포함)
        UserProfileHeader(
            nickname = uiState.profileInfo?.nickname ?: "",
            userHandle = "@${uiState.userHandle}",
            profileImageUrl = uiState.profileInfo?.profileImageUrl,
            bio = uiState.profileInfo?.bio ?: "",
            feedCount = uiState.feedCount,
            followerCount = uiState.followerCount,
            followingCount = uiState.followingCount,
            isFollowing = uiState.isFollowing,
            onFollowClick = { viewModel.toggleFollow() },
            isMe = uiState.isMe
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 탭 바 (MyPage 컴포넌트 재사용)
        UserProfileTabBar(
            selectedTab = uiState.selectedTab,
            onFeedTabClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                viewModel.selectTab(MyPageTab.FEED)
            },
            onTastyTabClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                viewModel.selectTab(MyPageTab.TASTY_LIST)
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> UserFeedPage(
                    feeds = uiState.myFeeds,
                    onFeedClick = { feedId ->
                        navController.navigate("${Screen.FEED_DETAIL.route}/$feedId")
                    }
                )
                1 -> UserTastyListPage(
                    tastyLists = uiState.myTastyLists,
                    onTastyListClick = { tastyListId ->
                        navController.navigate("tasty_detail/$tastyListId")
                    }
                )
            }
        }
    }
}

@Composable
private fun UserProfileHeader(
    nickname: String,
    userHandle: String,
    profileImageUrl: String?,
    bio: String,
    feedCount: Int,
    followerCount: Int,
    followingCount: Int,
    isFollowing: Boolean,
    isMe: Boolean,
    onFollowClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                //프로필 이미지
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4D8F5)),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF6A4FA3),
                            modifier = Modifier.size(42.dp)
                        )
                    } else {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nickname,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = userHandle,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            //통계
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryColor.copy(alpha = 0.30f))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(feedCount, "피드")
                StatItem(followerCount, "팔로워")
                StatItem(followingCount, "팔로잉")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // bio
            Text(
                text = if (bio.isBlank()) "소개가 아직 없어요." else bio,
                fontSize = 14.sp,
                color = if (bio.isBlank()) Color.Gray else Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 팔로우 버튼
            Button(
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMe) Color.Red.copy(alpha = 0.7f) else PrimaryColor,
                    contentColor = TextColor
                ),
                enabled = !isMe,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isMe)  {
                        "자기 자신은 팔로우 할 수 없어요."
                    } else if (isFollowing) {
                        "팔로우 취소"
                    } else {
                        "팔로우"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            color = TextColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.Black,
        )
    }
}

@Composable
private fun UserProfileTabBar(
    selectedTab: MyPageTab,
    onFeedTabClick: () -> Unit,
    onTastyTabClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F1F1))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileTabButton(
            modifier = Modifier.weight(1f),
            selected = selectedTab == MyPageTab.FEED,
            icon = Icons.Default.GridOn,
            text = "피드",
            onClick = onFeedTabClick
        )

        ProfileTabButton(
            modifier = Modifier.weight(1f),
            selected = selectedTab == MyPageTab.TASTY_LIST,
            icon = Icons.Default.Bookmarks,
            text = "Tasty 리스트",
            onClick = onTastyTabClick
        )
    }
}

@Composable
private fun ProfileTabButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val containerColor = if (selected) PrimaryColor else Color.Transparent
    val contentColor = if (selected) TextColor else Color(0xFF6F6F6F)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun UserFeedPage(feeds: List<MyFeedItem>, onFeedClick: (String) -> Unit) {
    if (feeds.isEmpty()) {
        EmptyContent("작성한 피드가 없습니다.", "첫 피드를 기다려주세요.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(feeds) { feed ->
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray).clickable { onFeedClick(feed.feedId) }
                ) {
                    if (!feed.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = feed.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (feed.hasImages) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "사진 포함",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserTastyListPage(
    tastyLists: List<MyTastyListItem>,
    onTastyListClick: (String) -> Unit
) {
    if (tastyLists.isEmpty()) {
        EmptyContent("작성한 Tasty 리스트가 없습니다.", "공유된 입맛이 아직 없어요.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tastyLists) { tasty ->
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                        .background(PrimaryColor.copy(alpha = 0.3f))
                        .clickable { onTastyListClick(tasty.tastyListId) }
                ) {
                    if (tasty.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = tasty.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(8.dp)) {
                        Text(text = tasty.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = TextColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
