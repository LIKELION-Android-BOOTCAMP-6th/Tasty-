package com.tasty.android.feature.feed

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.util.toFormattedDate
import com.tasty.android.feature.feed.model.FeedComment
import com.tasty.android.feature.vmfactory.FeedDetailViewModelFactory

private val Gray100 = Color(0xFFF7F7F7)
private val Gray200 = Color(0xFFE5E5E5)
private val Gray300 = Color(0xFFD9D9D9)
private val Gray400 = Color(0xFFB5B5B5)
private val AccentRed = Color(0xFFE97B7B)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FeedDetailScreen(
    navController: NavHostController,
    feedId: String,
    viewModel: FeedDetailViewModel = viewModel(factory = FeedDetailViewModelFactory),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "게시글 상세",
                showTopBar = true,
                showBottomBar = false,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        )
    }

    LaunchedEffect(feedId) {
        viewModel.loadFeedDetail(feedId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                uiState.post?.let { post ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            FeedDetailHeader(post = post)

                            Spacer(modifier = Modifier.height(18.dp))

                            if (post.shortReview.isNotBlank()) {
                                Text(
                                    text = post.shortReview,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AccentRed
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Text(
                                text = post.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextColor
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (post.imageUrls.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(post.imageUrls) { imageUrl ->
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = "피드 이미지",
                                            modifier = Modifier
                                                .fillParentMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Gray200),
                                            contentScale = ContentScale.Crop
                                        )

                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .background(Gray200, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "게시글 이미지 없음",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Gray400
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FeedRestaurantInfo(
                                placeName = post.placeName,
                                address = post.address
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if(post.isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "좋아요",
                                        tint = if(post.isLiked) Color.Red else TextColor,
                                        modifier = Modifier.clickable {
                                            viewModel.toggleLike(feedId)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = post.likeCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextColor
                                        )
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = "댓글",
                                        tint = TextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = post.commentCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextColor
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Gray200)
                    }
                }

                item {
                    Text(
                        text = "댓글 ${uiState.comments.size}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                    )
                }

                items(
                    items = uiState.comments,
                    key = { it.commentId }
                ) { comment ->
                    CommentItem(comment = comment)
                }

                item {
                    if (uiState.hasMoreComments) {
                        LaunchedEffect(uiState.comments.size) {
                            viewModel.loadMoreComments(feedId)
                        }
                    }
                    if (uiState.isLoadingMoreComments) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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

            CommentInputBar(
                value = uiState.commentInput,
                onValueChange = viewModel::updateCommentInput,
                onSendClick = { viewModel.submitComment(feedId) },
                enabled = uiState.canSubmitComment
            )
        }
    }
}

@Composable
private fun FeedDetailHeader(
    post: FeedDetailPostUiModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Gray300),
            contentAlignment = Alignment.Center
        ) {
            if (post.authorProfileUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "기본 프로필",
                    modifier = Modifier.fillMaxSize(0.8f),
                    tint = Gray400
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = post.authorNickname,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "@${post.userHandle}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < post.rating) TextColor else Gray300,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.dateText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextColor
                )
            )
        }
    }
}

@Composable
private fun FeedRestaurantInfo(
    placeName: String,
    address: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Gray200,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "식당 위치",
                tint = TextColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = placeName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextColor
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = address,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextColor
            )
        )
    }
}

@Composable
private fun CommentItem(
    comment: FeedComment
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Gray300),
            contentAlignment = Alignment.Center
        ) {
            if (comment.authorProfileUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "기본 프로필",
                    modifier = Modifier.fillMaxSize(0.8f),
                    tint = Gray400
                )
            } else {
                AsyncImage(
                    model = comment.authorProfileUrl,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorNickname,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "@${comment.authorHandle}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextColor
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.createdAt?.toFormattedDate() ?: "",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Gray400
                )
            )
        }
    }
}

@Composable
private fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gray100)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = Gray300,
                    shape = RoundedCornerShape(24.dp)
                )
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TextColor
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = "댓글을 입력해주세요",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Gray400
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (enabled) PrimaryColor else Gray300,
                    CircleShape
                )
                .clickable(enabled = enabled) { onSendClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "댓글 전송",
                tint = TextColor
            )
        }
    }
}