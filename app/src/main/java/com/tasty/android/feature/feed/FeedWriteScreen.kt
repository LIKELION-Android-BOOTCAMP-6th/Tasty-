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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tasty.android.core.design.component.CustomBottomAppBar

@Composable
fun FeedWriteScreen(
    navController: NavController,
    viewModel: FeedWriteViewModel = viewModel(),
    onSubmitClick: () -> Unit = {},
    onSearchRestaurantClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            FeedWriteTopBar(
                canSubmit = uiState.canSubmit,
                onBackClick = { navController.popBackStack() },
                onSubmitClick = onSubmitClick
            )
        },
        bottomBar = {
            CustomBottomAppBar(navController = navController)
        },
        containerColor = Color(0xFFF8F8F8)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            RestaurantSelectField(
                restaurantName = uiState.selectedRestaurant?.name,
                onClick = onSearchRestaurantClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            RatingSection(
                rating = uiState.rating,
                onRatingChange = viewModel::setRating
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "오늘 먹은 음식은 어땠나요?",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            InputBox(
                value = uiState.content,
                onValueChange = viewModel::setContent,
                placeholder = "최소 10글자 이상 입력해주세요.",
                height = 180.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "한줄평",
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            InputBox(
                value = uiState.shortReview,
                onValueChange = viewModel::setShortReview,
                placeholder = "한줄평을 입력해주세요.",
                height = 48.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "사진 추가",
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            PhotoPickerSection(
                images = uiState.imageUris,
                onAddClick = {
                    viewModel.addImage("sample_image_${uiState.imageUris.size + 1}")
                }
            )
        }
    }
}

@Composable
private fun FeedWriteTopBar(
    canSubmit: Boolean,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
        }

        Text(
            text = "게시글 작성",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        TextButton(
            onClick = onSubmitClick,
            enabled = canSubmit
        ) {
            Text(
                text = "게시",
                color = if (canSubmit) Color.Black else Color.Gray
            )
        }
    }
}

@Composable
private fun RestaurantSelectField(
    restaurantName: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "식당 선택"
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = restaurantName ?: "식당을 선택해주세요",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(text = ">")
    }
}

@Composable
private fun RatingSection(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) { index ->
            val starNumber = index + 1
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "별점 $starNumber",
                tint = if (starNumber <= rating) Color(0xFFFFC107) else Color.LightGray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChange(starNumber) }
            )
        }
    }
}

@Composable
private fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    height: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        if (value.isBlank()) {
            Text(
                text = placeholder,
                color = Color.LightGray
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
        )
    }
}

@Composable
private fun PhotoPickerSection(
    images: List<String>,
    onAddClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "사진 추가",
                    tint = Color.LightGray
                )
            }
        }

        itemsIndexed(images) { _, _ ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFEAEAEA), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("사진")
            }
        }
    }
}