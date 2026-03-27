package com.tasty.android.feature.mypage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen
import kotlinx.coroutines.launch
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor

@Composable
fun MyPageScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val initialPage = when (uiState.selectedTab) {
        MyPageTab.FEED -> 0
        MyPageTab.TASTY_LIST -> 1
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2 }
    )

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> viewModel.selectTab(MyPageTab.FEED)
            1 -> viewModel.selectTab(MyPageTab.TASTY_LIST)
        }
    }

    LaunchedEffect(uiState.shouldShowTastyListFab) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "마이 페이지",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
                isCenterAligned = true,
                floatingActionButton = {
                    AnimatedVisibility(visible = uiState.shouldShowTastyListFab) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(Screen.MY_PAGE_SELECT_FEEDS.route)
                            },
                            containerColor = PrimaryColor,
                            contentColor = TextColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "테이스티 리스트 생성"
                                )
                            }
                        }
                    }
                }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        MyPageProfileHeader(
            nickname = uiState.profileInfo.nickname,
            username = uiState.profileInfo.username,
            intro = uiState.profileInfo.intro,
            feedCount = uiState.feedCount,
            followerCount = uiState.followerCount,
            followingCount = uiState.followingCount
        )

        Spacer(modifier = Modifier.height(20.dp))

        MyPageTabBar(
            selectedTab = uiState.selectedTab,
            onFeedTabClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
                viewModel.selectTab(MyPageTab.FEED)
            },
            onTastyTabClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
                viewModel.selectTab(MyPageTab.TASTY_LIST)
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> MyFeedPage(feeds = uiState.myFeeds)
                1 -> MyTastyListPage(tastyLists = uiState.myTastyLists)
            }
        }
    }
}

@Composable
private fun MyPageProfileHeader(
    nickname: String,
    username: String,
    intro: String,
    feedCount: Int,
    followerCount: Int,
    followingCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFE4D8F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFF6A4FA3)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = PrimaryColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = feedCount, label = "피드")
                StatItem(count = followerCount, label = "팔로워")
                StatItem(count = followingCount, label = "팔로잉")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = PrimaryColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                text = nickname,
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = username,
                color = TextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = intro,
                color = TextColor
            )
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String
) {
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
            color = Color.Black
        )
    }
}

@Composable
private fun MyPageTabBar(
    selectedTab: MyPageTab,
    onFeedTabClick: () -> Unit,
    onTastyTabClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            MyPageTabItem(
                modifier = Modifier.weight(1f),
                selected = selectedTab == MyPageTab.FEED,
                icon = Icons.Default.GridOn,
                text = "내 피드",
                onClick = onFeedTabClick
            )

            MyPageTabItem(
                modifier = Modifier.weight(1f),
                selected = selectedTab == MyPageTab.TASTY_LIST,
                icon = Icons.Default.Bookmarks,
                text = "Tasty 리스트",
                onClick = onTastyTabClick
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.LightGray,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        if (selectedTab == MyPageTab.FEED) Color(0xFFB9E2C0) else Color.Transparent
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        if (selectedTab == MyPageTab.TASTY_LIST) Color(0xFFB9E2C0) else Color.Transparent
                    )
            )
        }
    }
}

@Composable
private fun MyPageTabItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val contentColor = if (selected) Color(0xFFB9E2C0) else Color.Gray

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun MyFeedPage(feeds: List<MyFeedItem>) {
    if (feeds.isEmpty()) {
        EmptyContent(
            title = "아직 작성하신 피드가 없어요.",
            description = "새로운 피드를 작성해주세요."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            items(feeds) { feed ->
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(PrimaryColor)
                ) {
                    Text(
                        text = feed.title.ifBlank { "피드 ${feed.id}" },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = TextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MyTastyListPage(tastyLists: List<MyTastyListItem>) {
    if (tastyLists.isEmpty()) {
        EmptyContent(
            title = "아직 작성하신 Tasty 리스트가 없어요.",
            description = "새로운 Tasty 리스트를 작성해주세요."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.navigationBars.asPaddingValues()
        ) {
            items(tastyLists) { tastyItem ->
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(PrimaryColor)
                ) {
                    Text(
                        text = tastyItem.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        color = TextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = TextColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = Color.Black
            )
        }
    }
}