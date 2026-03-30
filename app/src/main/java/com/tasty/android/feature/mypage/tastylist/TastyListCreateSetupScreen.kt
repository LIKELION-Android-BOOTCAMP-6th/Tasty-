package com.tasty.android.feature.tastylist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.core.navigation.TabScreen

@Composable
fun TastyListCreateSetupScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: TastyListCreateSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateThumbnailImageUrl(it.toString())
        }
    }

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

        Spacer(modifier = Modifier.padding(top = 20.dp))

        Text(
            text = "Tasty 리스트의 제목과 썸네일을 정해주세요.",
            color = TextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.padding(top = 28.dp))

        Box(
            modifier = Modifier
                .size(164.dp)
                .align(Alignment.CenterHorizontally)
                .border(
                    width = 1.dp,
                    color = TextColor.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .background(Color.White, CircleShape)
                .clickable {
                    imagePickerLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.thumbnailImageUrl.isBlank()) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "썸네일 추가",
                        tint = TextColor,
                        modifier = Modifier.size(52.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "추가",
                        tint = TextColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = uiState.thumbnailImageUrl,
                    contentDescription = "선택한 썸네일",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
        }



        Spacer(modifier = Modifier.padding(top = 24.dp))

        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::updateTitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = {
                Text(
                    text = "제목을 설정해주세요. (4~20자)",
                    color = TextColor.copy(alpha = 0.6f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = PrimaryColor.copy(alpha = 0.5f),
                focusedTextColor = TextColor,
                unfocusedTextColor = TextColor,
                cursorColor = TextColor
            )
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Text(
            text = "${uiState.title.length}/20",
            color = TextColor,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                val isSuccess = viewModel.completeCreation()
                if (isSuccess) {
                    viewModel.clearDraft()
                    navController.navigate(TabScreen.MY_PAGE.route) {
                        popUpTo(TabScreen.MY_PAGE.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            },
            enabled = uiState.canComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                contentColor = TextColor,
                disabledContainerColor = PrimaryColor.copy(alpha = 0.5f),
                disabledContentColor = TextColor.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = if (uiState.isSaving) "저장 중..." else "완료",
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
