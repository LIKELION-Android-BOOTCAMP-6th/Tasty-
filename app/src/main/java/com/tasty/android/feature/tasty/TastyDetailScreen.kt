package com.tasty.android.feature.tasty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor

@Composable
fun TastyDetailScreen(
    onBackClick: () -> Unit,
    onClickFeed: (String) -> Unit,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TastyDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "Tasty",
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = true,
                onBackClick = onBackClick,
                isCenterAligned = true
            )
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    TastyDetailScreenContent(
        uiState = uiState,
        onClickLike = viewModel::toggleLike,
        onClickFeed = onClickFeed,
        modifier = modifier
    )
}

@Composable
private fun TastyDetailScreenContent(
    uiState: TastyDetailUiState,
    onClickLike: () -> Unit,
    onClickFeed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6F6)),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TastyDetailHeader(
                title = uiState.title,
                author = uiState.author,
                likeCount = uiState.likeCount,
                viewCount = uiState.viewCount,
                isLiked = uiState.isLiked,
                onClickLike = onClickLike
            )
        }

        items(uiState.feedList, key = { it.feedId }) { feed ->
            TastyFeedListItem(
                item = feed,
                onClick = { onClickFeed(feed.feedId) }
            )
        }
    }
}

@Composable
private fun TastyDetailHeader(
    title: String,
    author: TastyAuthorUiModel,
    likeCount: Int,
    viewCount: Int,
    isLiked: Boolean,
    onClickLike: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PrimaryColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextColor,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = author.profileImageUrl,
                    contentDescription = author.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = author.nickname,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = author.username,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextColor
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = "조회수",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCount(viewCount),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.clickable(onClick = onClickLike),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "좋아요",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCount(likeCount),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = author.introduction,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextColor
                )
            )
        }
    }
}

@Composable
private fun TastyFeedListItem(
    item: TastyFeedItemUiModel,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.restaurantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 120.dp, height = 96.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.restaurantName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.category} ${item.address} ${item.distanceText}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "평점",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "평점 ${item.rating} 리뷰 ${item.reviewCount}개",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF4A4A4A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.oneLineReview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color(0xFFE2E2E2)
        )
    }
}

private fun formatCount(value: Int): String {
    return when {
        value >= 10000 -> {
            val man = value / 10000.0
            if (man % 1.0 == 0.0) "${man.toInt()}만" else "${String.format("%.1f", man)}만"
        }
        value >= 1000 -> {
            val thousand = value / 1000.0
            if (thousand % 1.0 == 0.0) "${thousand.toInt()}천" else "${String.format("%.1f", thousand)}천"
        }
        else -> value.toString()
    }
}