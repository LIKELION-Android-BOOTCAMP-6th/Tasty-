package com.tasty.android.feature.mypage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.AppBarAction
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.component.TastyConfirmDialog
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen
import com.tasty.android.feature.vmfactory.MyPageViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: MyPageViewModel = viewModel(factory = MyPageViewModelFactory)
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
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var selectedTastyListId by remember { mutableStateOf<String?>(null) }
    var showTastyListOptions by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val tastyOptionsSheetState = rememberModalBottomSheetState()

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> viewModel.selectTab(MyPageTab.FEED)
            1 -> viewModel.selectTab(MyPageTab.TASTY_LIST)
        }
    }

    LaunchedEffect(uiState.shouldShowTastyListFab, showBottomSheet) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "마이 페이지",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
                isCenterAligned = true,
                topBarActions = listOf(
                    AppBarAction(
                        icon = Icons.Default.MoreHoriz,
                        contentDescription = "더보기",
                        onActionClick = { showBottomSheet = true }
                    )
                ),
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        MyPageProfileHeader(
            nickname = uiState.profileInfo?.nickname ?: "",
            userHandle = ("@" + uiState.profileInfo?.userHandle),
            profileImageUrl = uiState.profileInfo?.profileImageUrl, // 추가
            bio = uiState.profileInfo?.bio ?: "",
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
                0 -> MyFeedPage(
                    feeds = uiState.myFeeds,
                    hasMoreFeeds = uiState.hasMoreFeeds,
                    isLoadingMoreFeeds = uiState.isLoadingMoreFeeds,
                    onLoadMore = { viewModel.loadMoreMyFeed() },
                    onFeedClick = {feedId ->
                        navController.navigate("${Screen.FEED_DETAIL.route}/$feedId")

                    }
                )
                1 -> MyTastyListPage(
                    tastyLists = uiState.myTastyLists,
                    isLoading = uiState.isLoading,
                    onTastyListClick = { tastyListId ->
                        navController.navigate("tasty_detail/$tastyListId")
                    },
                    onTastyListLongClick = { tastyListId ->
                        selectedTastyListId = tastyListId
                        showTastyListOptions = true
                    }
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(8.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text("프로필 수정") },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        navController.navigate(Screen.MY_PAGE_EDIT_PROFILE.route)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.White)
                )
                ListItem(
                    headlineContent = { Text("로그아웃", color = Color.Red) },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        showLogoutDialog = true
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.White)
                )
            }
        }
    }

    if (showLogoutDialog) {
        TastyConfirmDialog(
            title = "로그아웃 하시나요?",
            message = "로그아웃 하시면 로그인/회원가입 화면으로 돌아가요.",
            onConfirm = {
                showLogoutDialog = false
                viewModel.signOut()
                navController.navigate(Screen.AUTH_ON_BOARDING.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showTastyListOptions) {
        ModalBottomSheet(
            onDismissRequest = { showTastyListOptions = false },
            sheetState = tastyOptionsSheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text("리스트 편집") },
                    modifier = Modifier.clickable {
                        showTastyListOptions = false
                        selectedTastyListId?.let { id ->
                            navController.navigate("${Screen.EDIT_TASTY_LIST.route.replace("{tastyListId}", id)}")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.White)
                )
                ListItem(
                    headlineContent = { Text("리스트 삭제", color = Color.Red) },
                    modifier = Modifier.clickable {
                        showTastyListOptions = false
                        showDeleteConfirmDialog = true
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.White)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        TastyConfirmDialog(
            title = "리스트를 삭제하시겠습니까?",
            message = "삭제된 리스트는 복구할 수 없습니다.",
            confirmLabel = "삭제",
            cancelLabel = "취소",
            onConfirm = {
                showDeleteConfirmDialog = false
                selectedTastyListId?.let { id ->
                    viewModel.deleteTastyList(id)
                }
                selectedTastyListId = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                selectedTastyListId = null
            }
        )
    }
}

@Composable
private fun MyPageProfileHeader(
    nickname: String,
    userHandle: String,
    profileImageUrl: String?, // 추가
    bio: String,
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
                    .clip(CircleShape)
                    .background(Color(0xFFE4D8F5)),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "기본 프로필",
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFF6A4FA3)
                    )
                } else {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
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
                text = userHandle,
                color = TextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bio,
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
private fun MyFeedPage(
    feeds: List<MyFeedItem>,
    hasMoreFeeds: Boolean,
    isLoadingMoreFeeds: Boolean,
    onLoadMore: () -> Unit,
    onFeedClick: (String) -> Unit
) {
    if (feeds.isEmpty() && !isLoadingMoreFeeds) {
        EmptyContent(
            title = "아직 작성하신 피드가 없어요.",
            description = "새로운 피드를 작성해주세요."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(feeds, key = {it.feedId}) { feed ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .height(120.dp)
                        .background(Color.Gray)
                        .clickable { onFeedClick(feed.feedId) }
                ) {
                    if (!feed.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = feed.thumbnailUrl,
                            contentDescription = "피드 썸네일",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "${feed.feedId} 이미지 없음",
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = TextColor
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (hasMoreFeeds) {
                    LaunchedEffect(feeds.size) {
                        onLoadMore()
                    }
                }
                if (isLoadingMoreFeeds) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryColor,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MyTastyListPage(
    tastyLists: List<MyTastyListItem>,
    isLoading: Boolean = false,
    onTastyListClick: (String) -> Unit,
    onTastyListLongClick: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryColor)
        }
    } else if (tastyLists.isEmpty()) {
        EmptyContent(
            title = "아직 작성하신 Tasty 리스트가 없어요.",
            description = "새로운 Tasty 리스트를 작성해주세요."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 2개로 조정 (피드와 맞춤)
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tastyLists, key = { it.tastyListId }) { tastyItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryColor.copy(alpha = 0.3f))
                        .combinedClickable(
                            onClick = { onTastyListClick(tastyItem.tastyListId) },
                            onLongClick = { onTastyListLongClick(tastyItem.tastyListId) }
                        )
                ) {
                    if (tastyItem.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = tastyItem.thumbnailUrl,
                            contentDescription = "썸네일",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = tastyItem.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "피드 ${tastyItem.feedCount}개 · 조회 ${tastyItem.viewCount}",
                            color = Color.White.copy(alpha = 0.8f),
                            style = TextStyle(fontSize = 8.sp)
                        )
                    }
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