package com.tasty.android.feature.mypage.tastylist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.feature.mypage.MyFeedItem
import com.tasty.android.feature.vmfactory.EditTastyListViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTastyListScreen(
    tastyListId: String,
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: EditTastyListViewModel = viewModel(factory = EditTastyListViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }

    LaunchedEffect(tastyListId) {
        viewModel.initLoad(tastyListId, currentUserId)
    }

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "Tasty 리스트 수정",
                showTopBar = true,
                showBottomBar = false,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() },
                isCenterAligned = true,
                containerColor = PrimaryColor
            )
        )
    }

    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalDivider(color = Color.LightGray, thickness = 1.dp)

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "리스트 제목",
                                fontWeight = FontWeight.Bold,
                                color = TextColor,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = { viewModel.updateTitle(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("제목을 입력해주세요") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryColor,
                                    unfocusedBorderColor = Color.LightGray,
                                    cursorColor = PrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "포함된 피드 구성 (최소 1개 선택)",
                                fontWeight = FontWeight.Bold,
                                color = TextColor,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(uiState.myFeeds, key = { it.feedId }) { feed ->
                        val isSelected = uiState.selectedFeedIds.contains(feed.feedId)
                        EditFeedSelectCard(
                            feed = feed,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleFeedSelection(feed.feedId) }
                        )
                    }
                }
            }

            // 하단 저장 버튼
            Button(
                onClick = { viewModel.saveChanges() },
                enabled = uiState.isSaveEnabled && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = TextColor,
                    disabledContainerColor = PrimaryColor.copy(alpha = 0.5f),
                    disabledContentColor = TextColor.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "저장하기", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 80.dp),
                action = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("확인", color = Color.White)
                    }
                },
                containerColor = Color.Red.copy(alpha = 0.8f)
            ) {
                Text(uiState.errorMessage!!)
            }
        }
    }
}

@Composable
private fun EditFeedSelectCard(
    feed: MyFeedItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryColor else Color.LightGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (!feed.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = feed.thumbnailUrl,
                contentDescription = "피드 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                Text("이미지 없음", fontSize = 10.sp, color = Color.Gray)
            }
        }

        // 선택 상태 표시용 오버레이
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryColor.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "선택됨",
                    tint = TextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
