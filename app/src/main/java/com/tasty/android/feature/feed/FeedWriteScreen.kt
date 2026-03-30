package com.tasty.android.feature.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.design.component.AppBarAction
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen
import com.tasty.android.feature.vmfactory.FeedWriteViewModelFactory

private val TextColor = Color(0xFF4C4B4B)
private val Gray100 = Color(0xFFF7F7F7)
private val Gray200 = Color(0xFFE5E5E5)
private val Gray400 = Color(0xFFBDBDBD)

@Composable
fun FeedWriteScreen(
    navController: NavHostController,
    viewModel: FeedWriteViewModel = viewModel(factory = FeedWriteViewModelFactory),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authorId = Firebase.auth.currentUser?.uid ?: ""

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.addPhoto(selectedUri)
        }
    }

    LaunchedEffect(uiState.canSubmit, uiState.isSubmitting) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "게시글 작성",
                showTopBar = true,
                showBottomBar = false,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() },
                topBarActions = listOf(
                    AppBarAction(
                        onActionClick = {
                            if (uiState.canSubmit && !uiState.isSubmitting) {
                                viewModel.submitPost(authorId = authorId) {
                                    navController.previousBackStackEntry?.savedStateHandle?.set("refreshFeed", true)
                                    navController.popBackStack()
                                }
                            }
                        },
                        icon = Icons.Default.Send,
                        contentDescription = "게시",
                        isLoading = uiState.isSubmitting
                    )
                )
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RestaurantSelectSection(
                    selectedRestaurant = uiState.selectedRestaurant,
                    onClick = {
                        navController.navigate(Screen.FEED_SEARCH_RESTAURANT.route)
                    },
                    onClearClick = {
                        viewModel.clearRestaurant()
                    }
                )

                RatingSection(
                    rating = uiState.rating,
                    onRatingSelected = viewModel::updateRating
                )

                ContentInputSection(
                    content = uiState.content,
                    onValueChange = viewModel::updateContent
                )

                ShortReviewSection(
                    shortReview = uiState.shortReview,
                    onValueChange = viewModel::updateShortReview
                )

                PhotoSection(
                    photos = uiState.photos,
                    onAddPhotoClick = {
                        if (uiState.photos.size < 5) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    },
                    onRemovePhotoClick = viewModel::removePhoto
                )
            }
        }


        if (uiState.isLoadingRestaurants) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RestaurantSelectSection(
    selectedRestaurant: SelectedRestaurantUiModel?,
    onClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFD9D9D9),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "식당 선택",
            tint = TextColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (selectedRestaurant == null) {
            Text(
                text = "식당을 선택해주세요",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextColor
                ),
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = selectedRestaurant.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextColor
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedRestaurant.address,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextColor
                    )
                )
            }
        }

        if (selectedRestaurant != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onClearClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "선택 해제",
                    tint = TextColor
                )
            }
        }
    }
}

@Composable
private fun RatingSection(
    rating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(5) { index ->
            val star = index + 1

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onRatingSelected(star) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "별점 $star",
                    tint = if (star <= rating) TextColor else Color(0xFFD9D9D9),
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentInputSection(
    content: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        BasicTextField(
            value = content,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = TextColor
            ),
            modifier = Modifier.fillMaxSize(),
            decorationBox = { innerTextField ->
                if (content.isBlank()) {
                    Text(
                        text = "오늘 먹은 음식은 어땠나요?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFB5B5B5)
                        )
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun ShortReviewSection(
    shortReview: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = "한줄평",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextColor
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFFD9D9D9),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = shortReview,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TextColor
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (shortReview.isBlank()) {
                        Text(
                            text = "한줄평을 입력해주세요",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFB5B5B5)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun PhotoSection(
    photos: List<FeedPhotoUiModel>,
    onAddPhotoClick: () -> Unit,
    onRemovePhotoClick: (String) -> Unit
) {
    Column {
        Text(
            text = "사진 추가",
            style = MaterialTheme.typography.bodySmall,
            color = TextColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (photos.size < 5) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gray100)
                        .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddPhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "사진 추가",
                        tint = Gray400
                    )
                }
            }

            photos.forEach { photo ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gray100)
                        .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = "선택한 사진 ${photo.order}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${photo.order}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                onRemovePhotoClick(photo.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "사진 삭제",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${photos.size}/5",
            style = MaterialTheme.typography.bodySmall,
            color = Gray400
        )
    }
}