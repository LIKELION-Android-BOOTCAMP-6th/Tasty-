package com.tasty.android.feature.mypage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                                modifier = Modifier.padding(horizontal = 10.dp),
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
            .background(Color(0xFFFAFAFA))
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        MyPageProfileHeader(
            nickname = uiState.profileInfo?.nickname ?: "",
            userHandle = "@" + (uiState.profileInfo?.userHandle ?: ""),
            profileImageUrl = uiState.profileInfo?.profileImageUrl,
            bio = uiState.profileInfo?.bio ?: "",
            feedCount = uiState.feedCount,
            followerCount = uiState.followerCount,
            followingCount = uiState.followingCount
        )

        Spacer(modifier = Modifier.height(18.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

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
                    onFeedClick = { feedId ->
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
    }

    if (!uiState.errorMessage.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF1F1))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
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
                            navController.navigate(
                                "${Screen.EDIT_TASTY_LIST.route}/$id"
                            )
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
    profileImageUrl: String?,
    bio: String,
    feedCount: Int,
    followerCount: Int,
    followingCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            contentDescription = "기본 프로필",
                            modifier = Modifier.size(42.dp),
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

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = nickname.ifBlank { "이름 없음" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF181818),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userHandle.ifBlank { "@user" },
                        fontSize = 13.sp,
                        color = Color(0xFF7A7A7A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryColor.copy(alpha = 0.30f))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = feedCount, label = "피드")
                StatItem(count = followerCount, label = "팔로워")
                StatItem(count = followingCount, label = "팔로잉")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (bio.isBlank()) "소개가 아직 없어요." else bio,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = if (bio.isBlank()) Color(0xFF9A9A9A) else Color(0xFF2A2A2A)
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF181818)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7A7A7A)
        )
    }
}

@Composable
private fun MyPageTabBar(
    selectedTab: MyPageTab,
    onFeedTabClick: () -> Unit,
    onTastyTabClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MyPageTabButton(
            modifier = Modifier.weight(1f),
            selected = selectedTab == MyPageTab.FEED,
            icon = Icons.Default.GridOn,
            text = "내 피드",
            onClick = onFeedTabClick
        )

        MyPageTabButton(
            modifier = Modifier.weight(1f),
            selected = selectedTab == MyPageTab.TASTY_LIST,
            icon = Icons.Default.Bookmarks,
            text = "Tasty 리스트",
            onClick = onTastyTabClick
        )
    }
}

@Composable
private fun MyPageTabButton(
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
private fun MyFeedPage(
    feeds: List<MyFeedItem>,
    hasMoreFeeds: Boolean,
    isLoadingMoreFeeds: Boolean,
    onLoadMore: () -> Unit,
    onFeedClick: (String) -> Unit
) {
    if (feeds.isEmpty() && !isLoadingMoreFeeds) {
        EmptyContent(
            title = "아직 작성한 피드가 없어요",
            description = "첫 번째 피드를 작성해보세요"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = 14.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(feeds, key = { it.feedId }) { feed ->
                FeedThumbnailCard(
                    feed = feed,
                    onClick = { onFeedClick(feed.feedId) }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (hasMoreFeeds) {
                    LaunchedEffect(feeds.size) {
                        onLoadMore()
                    }
                }

                if (isLoadingMoreFeeds) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryColor,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedThumbnailCard(
    feed: MyFeedItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF1F3F5))
            .clickable(onClick = onClick)
    ) {
        if (!feed.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = feed.thumbnailUrl,
                contentDescription = "피드 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.08f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.InsertPhoto,
                    contentDescription = "이미지 없음",
                    tint = PrimaryColor,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "이미지 없음",
                    color = Color(0xFF777777),
                    fontSize = 12.sp
                )
            }
        }

        if (feed.hasImages) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "여러 장 이미지",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyTastyListPage(
    tastyLists: List<MyTastyListItem>,
    isLoading: Boolean = false,
    onTastyListClick: (String) -> Unit,
    onTastyListLongClick: (String) -> Unit
) {
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (tastyLists.isEmpty() && !isLoading) {
            EmptyContent(
                title = "아직 만든 Tasty 리스트가 없어요",
                description = "새로운 리스트를 만들어보세요"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    top = 14.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tastyLists, key = { it.tastyListId }) { tastyItem ->
                    TastyListThumbnailCard(
                        item = tastyItem,
                        onClick = { onTastyListClick(tastyItem.tastyListId) },
                        onLongClick = { onTastyListLongClick(tastyItem.tastyListId) }
                    )
                }
            }
        }


        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TastyListThumbnailCard(
    item: MyTastyListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF1F3F5))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = "테이스티 리스트 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.10f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "리스트 기본 이미지",
                    tint = PrimaryColor,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tasty List",
                    color = Color(0xFF6F6F6F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(
                text = item.title.ifBlank { "제목 없는 리스트" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "피드 ${item.feedCount}개 · 조회 ${item.viewCount}",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 11.sp,
                maxLines = 1
            )
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
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "빈 상태",
                    tint = PrimaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = Color(0xFF7A7A7A),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}