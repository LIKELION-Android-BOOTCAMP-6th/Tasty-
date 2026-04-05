package com.tasty.android.feature.auth

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen

// 회원가입/이메일/비밀번호 입력 화면
@Composable
fun SignUpScreen(
    navController: NavHostController,
    viewmodel: SignUpAccountViewmodel = viewModel(factory = SignUpAccountViewmodel.Factory),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    // 입력값 상태 관리
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 버튼 활성화 조건: 이메일과 비밀번호 모두 비어있지 않을 때 true
    val isNextButtonEnabled = email.isNotBlank() && password.isNotBlank()

    //  색상 설정
    val pointGreen = Color(0xFF8DEB8D)
    val lightGreenText = Color(0xFF76D676)

    var showErrorDialog by remember { mutableStateOf(false) }

    // ViewModel 상태 구독
    val uiState by viewmodel.uiState.collectAsState()

    val focusManager = LocalFocusManager.current

    // 에러 메세지가 업데이트되면 다이얼로그 출력
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            showErrorDialog = true
        }
    }

    // 스캐폴드(상단 및 하단 메뉴) 적용
    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                showTopBar = false,
                showBottomBar = false
            )
        )
    }

    // 회원가입 성공 시 프로필 기입 화면으로 이동
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(Screen.AUTH_SIGN_UP_SET_PROFILE.route) {
                popUpTo(Screen.AUTH_SIGN_UP_EMAIL_PWD.route) {
                    inclusive = true
                } // 이메일, 비밀번호 기입 화면을 스택에서 제거
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus() // 배경 터치 시 포커스 해제
                })
            },
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // 환영 문구
        Text(
            text = "테이스티에 오신 걸 환영해요!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = lightGreenText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "회원가입을 위해 이메일과 비밀번호를 입력해주세요.",
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 이메일 입력창
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("이메일을 입력해주세요.", color = Color.LightGray) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = pointGreen,
                focusedBorderColor = pointGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 비밀번호 입력창
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("비밀번호는 6자 이상 입력해 주세요.", color = Color.LightGray) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = pointGreen,
                focusedBorderColor = pointGreen
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 다음 버튼
        Button(
            onClick = {
                focusManager.clearFocus()
                viewmodel.onNextClick(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = pointGreen),
            enabled = isNextButtonEnabled
        ) {
            // 지연 프로그레스 UI표시
            if (uiState.isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "다음",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 예외 처리 다이얼로그
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = { showErrorDialog = false }
                    ) {
                        Text("확인", color = pointGreen)
                    }
                },
                title = {
                    Text(text = "알림", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = uiState.errorMessage ?: "오류가 발생했습니다. 다시 시도해주세요.")
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}