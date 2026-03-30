package com.tasty.android.feature.tastylist

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.Screen

@Composable
fun TastyListCreateSelectFeedsScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: TastyListCreateSelectFeedsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "새 Tasty 리스트",
                showTopBar = true,
                showBottomBar = false,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() },
                isCenterAligned = true,
                containerColor = PrimaryColor
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tasty 리스트에 넣을 피드를 선택해주세요.",
            color = TextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${uiState.selectedCount}/10 선택",
            color = TextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = uiState.visibleFeeds,
                key = { _, item -> item.feedId }
            ) { index, item ->
                TastyListFeedSelectCard(
                    item = item,
                    onClick = { viewModel.toggleFeedSelection(item.feedId) }
                )

                if (index == uiState.visibleFeeds.lastIndex && uiState.hasNextPage) {
                    viewModel.loadNextPage()
                }
            }

            if (uiState.isPagingLoading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = Color.Red,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                viewModel.saveDraftSelection()
                navController.navigate(Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route)
            },
            enabled = uiState.canGoNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                contentColor = TextColor,
                disabledContainerColor = PrimaryColor.copy(alpha = 0.5f),
                disabledContentColor = TextColor.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "다음",
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TastyListFeedSelectCard(
    item: TastyListFeedSelectionItem,
    onClick: () -> Unit
) {
    val borderColor = if (item.isSelected) PrimaryColor else PrimaryColor.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = "식당 위치",
                tint = TextColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = item.restaurantName,
                color = TextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            SelectionCircle(
                selected = item.isSelected
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryColor.copy(alpha = 0.35f))
        ) {
            Text(
                text = item.firstImageLabel,
                color = TextColor,
                modifier = Modifier.align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.createdAt,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = item.oneLineReview,
            color = TextColor,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SelectionCircle(
    selected: Boolean
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(1.dp, TextColor.copy(alpha = 0.6f), CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(
                    if (selected) PrimaryColor else Color.Transparent
                )
        )
    }
}