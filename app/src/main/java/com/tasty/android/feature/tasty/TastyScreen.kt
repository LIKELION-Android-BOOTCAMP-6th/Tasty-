package com.tasty.android.feature.tasty

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RemoveRedEye
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tasty.android.core.design.theme.TextColor
import kotlinx.coroutines.delay

@Composable
fun TastyScreen(
    onClickTastyItem: (String) -> Unit,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TastyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState  = rememberLazyGridState()

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                showAppIcon = true,
                showTopBar = true,
                showBottomBar = true,
                containsBackButton = false,
                isCenterAligned = true
            )
        )
    }


    var lastSortType by remember { mutableStateOf<TastySortType?>(null) }

    LaunchedEffect(uiState.selectedSortType, uiState.tastyList) {
        val currentSort = uiState.selectedSortType


        if (lastSortType != null && lastSortType != currentSort && uiState.tastyList.isNotEmpty()) {
            delay(300)
            gridState.scrollToItem(0)
            lastSortType = currentSort
        }

        // 초기화
        if (lastSortType == null) {
            lastSortType = currentSort
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        //viewModel.refresh()
    }

    TastyScreenContent(
        uiState = uiState,
        onSelectSort = viewModel::selectSort,
        onClickTastyItem = onClickTastyItem,
        modifier = modifier,
        state = gridState
    )
}

@Composable
private fun TastyScreenContent(
    uiState: TastyUiState,
    onSelectSort: (TastySortType) -> Unit,
    onClickTastyItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6F6))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        RowSortButtons(
            selectedSort = uiState.selectedSortType,
            onSelectSort = onSelectSort
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
            )
        ) {
            items(uiState.tastyList, key = { it.tastyId }) { item ->
                TastyCard(
                    item = item,
                    onClick = { onClickTastyItem(item.tastyId) },
                    isLiked = item.isLiked
                )
            }
        }
    }
}

@Composable
private fun RowSortButtons(
    selectedSort: TastySortType,
    onSelectSort: (TastySortType) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SortChip(
            text = "최신순",
            selected = selectedSort == TastySortType.LATEST,
            onClick = { onSelectSort(TastySortType.LATEST) }
        )
        SortChip(
            text = "조회순",
            selected = selectedSort == TastySortType.VIEW_COUNT,
            onClick = { onSelectSort(TastySortType.VIEW_COUNT) }
        )
    }
}

@Composable
private fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) Color(0xFFD9D9D9) else Color(0xFFEAEAEA),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TastyCard(
    item: TastyItemUiModel,
    onClick: () -> Unit,
    isLiked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextColor,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            AsyncImage(
                model = item.thumbnailImageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFBEBEBE))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "좋아요",
                    tint = if (isLiked) Color.Red else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatCount(item.likeCount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Outlined.RemoveRedEye,
                    contentDescription = "조회수",
                    tint = TextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatCount(item.viewCount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
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