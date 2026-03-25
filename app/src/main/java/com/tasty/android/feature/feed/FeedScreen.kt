package com.tasty.android.feature.feed

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavHostController,
    viewModel: FeedViewModel = viewModel(),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showRegionSelection by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "Tasty",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                FeedHeaderSection(
                    tastyLists = uiState.tastyLists
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(
                items = uiState.feedPosts,
                key = { it.id }
            ) { feedPost ->
                FeedCard(
                    post = feedPost,
                    userRegion = uiState.userRegion,
                    onCardClick = {
                        navController.navigate(Screen.FEED_DETAIL.route)
                    },
                    onProfileClick = {
                        navController.navigate(Screen.USER_PROFILE.route)
                    },
                    onLikeClick = {
                        viewModel.increaseLike(feedPost.id)
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
                        selectedRegion = uiState.filter.selectedRegion,
                        onBackClick = {
                            showRegionSelection = false
                        },
                        onRegionSelected = { region ->
                            viewModel.updateRegion(region)
                        },
                        onConfirmClick = {
                            showRegionSelection = false
                        }
                    )
                } else {
                    FeedFilterSheet(
                        filter = uiState.filter,
                        onResetClick = {
                            viewModel.resetFilter()
                        },
                        onSortSelected = { sortType ->
                            viewModel.updateSortType(sortType)
                        },
                        onRegionClick = {
                            showRegionSelection = true
                        },
                        onApplyClick = {
                            viewModel.applyFilter()
                            showFilterSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeaderSection(
    tastyLists: List<TastyListUiModel>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryColor)
            .padding(top = 10.dp, bottom = 14.dp)
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(
                items = tastyLists.take(4),
                key = { it.id }
            ) { item ->
                TastyListCard(item = item)
            }
        }
    }
}

@Composable
private fun TastyListCard(
    item: TastyListUiModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFD9D9D9))
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.subTitle,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextColor
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
    userRegion: String,
    onCardClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFF2F2F2F),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(bottom = 14.dp)
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
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9D9D9))
                )

                Spacer(modifier = Modifier.size(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    )
                    Text(
                        text = userRegion,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextColor
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
                Text(
                    text = "피드 이미지",
                    color = TextColor
                )
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
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = TextColor,
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "필터",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                ),
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = "초기화",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextColor
                ),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onResetClick() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "정렬",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipButton(
                text = "최신순",
                selected = filter.sortType == FeedSortType.LATEST,
                onClick = { onSortSelected(FeedSortType.LATEST) }
            )

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
            )
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
                text = if (filter.selectedRegion.isBlank()) "지역 선택하기" else filter.selectedRegion,
                color = TextColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ">",
                color = TextColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF8A8080))
                .clickable { onApplyClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "적용",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
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
                if (selected) PrimaryColor else Color(0xFFF2F2F2)
            )
            .border(
                width = 1.dp,
                color = if (selected) PrimaryColor else Color(0xFFD9D9D9),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextColor
        )
    }
}

@Composable
private fun RegionSelectionSheet(
    selectedRegion: String,
    onBackClick: () -> Unit,
    onRegionSelected: (String) -> Unit,
    onConfirmClick: () -> Unit
) {
    val regions = listOf(
        "서울 강남구",
        "서울 서초구",
        "서울 송파구",
        "서울 마포구",
        "서울 종로구",
        "서울 서대문구"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
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

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "지역 선택",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        regions.forEach { region ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRegionSelected(region) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = region,
                        color = TextColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (selectedRegion == region) {
                        Text(
                            text = "선택됨",
                            color = TextColor
                        )
                    }
                }

                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF8A8080))
                .clickable { onConfirmClick() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "선택 완료",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}