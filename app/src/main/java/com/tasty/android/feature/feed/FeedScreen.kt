package com.tasty.android.feature.feed

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.asset.RegionData
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen
import com.tasty.android.feature.vmfactory.FeedViewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavHostController,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModelFactory),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // 리스트 상태 저장
    val listState = rememberLazyListState()
    // refresh 상태 저장
    val currentEntry = navController.currentBackStackEntry
    val savedStateHandle = currentEntry?.savedStateHandle

    val shouldRefresh = savedStateHandle?.get<Boolean>("refreshFeed") ?: false

    var showFilterSheet by remember { mutableStateOf(false) }
    var showRegionSelection by remember { mutableStateOf(false) }

    // 적용 전 임시 필터 상태
    var tempFilter by remember { mutableStateOf(FeedFilterUiState()) }

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "Tasty",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
                isCenterAligned = true,
                floatingActionButton = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(Screen.FEED_CREATE_FEED.route)
                            },
                            containerColor = PrimaryColor,
                            contentColor = TextColor
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "게시글 작성"
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                tempFilter = uiState.filter
                                showFilterSheet = true
                                showRegionSelection = false
                            },
                            containerColor = PrimaryColor,
                            contentColor = TextColor
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "필터"
                            )
                        }
                    }
                }
            )
        )
    }


    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    // Observer for Loading Feeds
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.distinctUntilChanged().collect {lastIdx ->
            if (lastIdx != null && lastIdx >= uiState.feedPosts.size - 1) {
                viewModel.loadMoreFeeds()
            }
        }
    }

    // Refresh 후에 초기값으로 복구
    LaunchedEffect(shouldRefresh) {
        if(shouldRefresh) {
            viewModel.invalidateCacheAndRefresh()
            savedStateHandle["refreshFeed"] = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                FeedHeaderSection(
                    tastyLists = uiState.tastyLists,
                    onTastyListClick = { tastyListId ->
                        navController.navigate("${Screen.TASTY_DETAIL.route}/$tastyListId")
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(
                items = uiState.feedPosts,
                key = { it.feedId }
            ) { feedPost ->
                FeedCard(
                    post = feedPost,
                    onCardClick = {
                        navController.navigate("${Screen.FEED_DETAIL.route}/${feedPost.feedId}")
                    },
                    onProfileClick = {
                        navController.navigate("user_profile/${feedPost.authorId}")
                    },
                    onLikeClick = {
                        viewModel.toggleLike(feedPost.feedId)
                    }
                )
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFilterSheet = false
                    showRegionSelection = false
                }
            ) {
                if (showRegionSelection) {
                    RegionSelectionSheet(
                        selectedMainRegion = tempFilter.mainRegion,
                        selectedSubRegion = tempFilter.subRegion,
                        onBackClick = {
                            showRegionSelection = false
                        },
                        onMainRegionSelected = { mainRegion ->
                            tempFilter = tempFilter.copy(
                                mainRegion = mainRegion,
                                subRegion = ""
                            )
                        },
                        onSubRegionSelected = { subRegion ->
                            tempFilter = tempFilter.copy(
                                subRegion = subRegion
                            )
                        },
                        onConfirmClick = {
                            showRegionSelection = false
                        }
                    )
                } else {
                    FeedFilterSheet(
                        filter = tempFilter,
                        onResetClick = {
                            tempFilter = FeedFilterUiState()
                        },
                        onSortSelected = { sortType ->
                            tempFilter = tempFilter.copy(sortType = sortType)
                        },
                        onRegionClick = {
                            showRegionSelection = true
                        },
                        onApplyClick = {
                            viewModel.applyFilter(tempFilter)
                            showFilterSheet = false
                            showRegionSelection = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeaderSection(
    tastyLists: List<TastyListUiModel>,
    onTastyListClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            color = Color(0xFF704B21),
            thickness = 0.6.dp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryColor)
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Tasty 리스트",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(
                    items = tastyLists.take(4),
                    key = { it.tastyListId }
                ) { item ->

                    TastyListCard(
                        item = item,
                        onClick = { onTastyListClick(item.tastyListId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TastyListCard(
    item: TastyListUiModel,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape

                )
                .background(Color(0xFFD9D9D9)),
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbnailImageUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "기본 썸네일",
                    tint = Color(0xFFB5B5B5),
                    modifier = Modifier.fillMaxSize(0.78f)
                )
            } else {
                AsyncImage(
                    model = item.thumbnailImageUrl,
                    contentDescription = "테이스티 리스트 썸네일",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.authorNickname,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextColor.copy(alpha = 0.9f)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeedCard(
    post: FeedPostUiModel,
    onCardClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Black.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9D9D9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.authorProfileUrl.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "기본 프로필",
                            modifier = Modifier.fillMaxSize(0.8f),
                            tint = Color(0xFFB5B5B5)
                        )
                    } else {
                        AsyncImage(
                            model = post.authorProfileUrl,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.authorNickname,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    )
                    Text(
                        text = "@${post.userHandle}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color(0xFFBEBEBE)),
                contentAlignment = Alignment.Center
            ) {
                post.thumbnailImageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "피드 대표 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if (post.isLiked) Color.Red else TextColor,
                        modifier = Modifier.clickable { onLikeClick() }
                    )
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "댓글",
                        tint = TextColor
                    )
                    Text(
                        text = post.commentCount.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextColor
                        )
                    )
                }

                Spacer(modifier = Modifier.size(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "식당",
                            tint = TextColor
                        )

                        Spacer(modifier = Modifier.size(4.dp))

                        Text(
                            text = post.placeName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextColor
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "별점",
                                tint = if (index < post.rating) TextColor else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))

                        Text(
                            text = post.dateText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = post.address,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextColor
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedFilterSheet(
    filter: FeedFilterUiState,
    onResetClick: () -> Unit,
    onSortSelected: (FeedSortType) -> Unit,
    onRegionClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "필터",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "정렬",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChipButton(
                text = "최신순",
                selected = filter.sortType == FeedSortType.LATEST,
                onClick = { onSortSelected(FeedSortType.LATEST) }
            )

            Spacer(modifier = Modifier.width(10.dp))

            FilterChipButton(
                text = "거리순",
                selected = filter.sortType == FeedSortType.DISTANCE,
                onClick = { onSortSelected(FeedSortType.DISTANCE) }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "지역",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFFD9D9D9),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onRegionClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "지역 선택",
                tint = TextColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = filter.selectedRegionText.ifBlank { "지역 선택" },
                color = TextColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ">",
                color = TextColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE5E5E5))
                    .clickable { onResetClick() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "초기화",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryColor)
                    .clickable { onApplyClick() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "적용",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) PrimaryColor else Color(0xFFE5E5E5)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        )
    }
}

@Composable
private fun RegionSelectionSheet(
    selectedMainRegion: String,
    selectedSubRegion: String,
    onBackClick: () -> Unit,
    onMainRegionSelected: (String) -> Unit,
    onSubRegionSelected: (String) -> Unit,
    onConfirmClick: () -> Unit
) {
    val mainRegions = RegionData.mainRegions
    val currentMainRegion =
        mainRegions.find { it.name == selectedMainRegion } ?: mainRegions.firstOrNull()
    val currentSubRegions = currentMainRegion?.subRegions ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = TextColor,
                modifier = Modifier.clickable { onBackClick() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "지역 선택",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFFD9D9D9),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(Color(0xFFF3F3F3))
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .background(Color(0xFFE9E9E9))
            ) {
                items(
                    items = mainRegions,
                    key = { it.name }
                ) { mainRegion ->
                    val isSelected = mainRegion.name == currentMainRegion?.name

                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color.White else Color(0xFFE9E9E9)
                                )
                                .clickable {
                                    onMainRegionSelected(mainRegion.name)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = mainRegion.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = TextColor
                                )
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFD3D3D3),
                            thickness = 1.dp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1.7f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                items(
                    items = currentSubRegions,
                    key = { it.name }
                ) { subRegion ->
                    val isSelected = subRegion.name == selectedSubRegion

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFFF6F1F0) else Color.White
                                )
                                .clickable {
                                    onSubRegionSelected(subRegion.name)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subRegion.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = TextColor
                                )
                            )

                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryColor
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFE7E7E7),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryColor)
                .clickable { onConfirmClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "선택 완료",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
