package com.tasty.android.feature.mypage

import EditProfileViewModel
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.design.component.TastyConfirmDialog
import com.tasty.android.core.design.theme.PrimaryColor
import com.tasty.android.core.design.theme.TextColor
import com.tasty.android.feature.vmfactory.EditProfileViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // 시스템 뒤로가기 처리
    BackHandler(enabled = true) {
        if (uiState.isChanged) {
            showExitDialog = true
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "프로필 수정",
                showTopBar = true,
                containsBackButton = true,
                showBottomBar = false,
                onBackClick = {
                    if (uiState.isChanged) {
                        showExitDialog = true
                    } else {
                        navController.popBackStack()
                    }
                },
                isCenterAligned = true
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 24.dp) // 하단 버튼 공간 확보
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 프로필 이미지 영역 + 연필 아이콘 오버레이
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                // 프로필 이미지 서클
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE4D8F5)),
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = uiState.selectedImageUri ?: uiState.initialProfileImageUrl
                    if (displayImage.toString().isBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "기본 프로필",
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF6A4FA3)
                        )
                    } else {
                        AsyncImage(
                            model = displayImage,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // 연필 아이콘 오버레이 (이미지 가이드에 맞춰 우측 하단 배치)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-10).dp, y = (-10).dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                   Box(
                       modifier = Modifier
                           .fillMaxSize()
                           .clip(CircleShape)
                           .background(Color.LightGray.copy(alpha = 0.5f))
                           ,
                       contentAlignment = Alignment.Center
                   ) {
                       Icon(
                           imageVector = Icons.Default.Edit,
                           contentDescription = "편집",
                           modifier = Modifier.size(16.dp),
                           tint = Color.Black
                       )
                   }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 닉네임 입력 필드
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "닉네임",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.nickname,
                    onValueChange = { viewModel.onNicknameChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("닉네임을 입력해주세요", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = PrimaryColor
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 소개글 입력 필드
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "소개",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.bio,
                    onValueChange = { viewModel.onBioChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("본인을 소개하는 글을 작성해주세요", color = Color.LightGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor.copy(alpha = 0.8f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = PrimaryColor
                    )
                )
            }
        }

        // 하단 저장 버튼
        Button(
            onClick = { viewModel.saveChanges() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .height(56.dp),
            enabled = uiState.isChanged && uiState.isNicknameValid && !uiState.isLoading,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                disabledContainerColor = PrimaryColor.copy(alpha = 0.7f)
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "저장",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 수정사항 다이얼로그
        if (showExitDialog) {
            TastyConfirmDialog(
                title = "수정사항이 있어요!",
                message = "취소하시면 수정하신 내용이 반영되지 않아요. 수정을 그만두시겠어요?",
                confirmLabel = "확인",
                cancelLabel = "취소",
                onConfirm = {
                    showExitDialog = false
                    navController.popBackStack()
                },
                onDismiss = {
                    showExitDialog = false
                }
            )
        }

        // 에러 메시지 처리
        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("확인", color = PrimaryColor)
                    }
                }
            ) {
                Text(uiState.errorMessage!!)
            }
        }
    }
}
