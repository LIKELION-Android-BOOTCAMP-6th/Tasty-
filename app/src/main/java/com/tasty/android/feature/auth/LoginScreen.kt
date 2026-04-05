package com.tasty.android.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen
import com.tasty.android.core.navigation.TabScreen

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()

    LoginScreen(
        navController = navController,
        onScaffoldConfigChange = { }
    )
}

@Composable
fun LoginScreen(
    navController: NavController,
    viewmodel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {

    // 입력값 상태 관리
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 버튼 활성화 조건: 이메일과 비밀번호 모두 비어있지 않을 때 true
    val isLoginButtonEnabled = email.isNotBlank() && password.isNotBlank()

    //  색상 설정
    val pointGreen = Color(0xFF8DEB8D)
    val lightGreenText = Color(0xFF76D676)

    val uiState by viewmodel.uiState.collectAsState()

    var showErrorDialog by remember { mutableStateOf(false) }

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

    // 로그인 성공 시 메인 화면으로 이동
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigate(TabScreen.FEED.route) {
                popUpTo(Screen.AUTH_ON_BOARDING.route) { inclusive = true } // 로그인 화면/온 보딩을 스택에서 제거
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // 환영 문구
        Text(
            text = "다시 만나서 반가워요",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = lightGreenText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "이메일과 비밀번호를 입력해주세요.",
            fontSize = 14.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(70.dp))

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
            placeholder = { Text("비밀번호를 입력해주세요.", color = Color.LightGray) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = pointGreen,
                focusedBorderColor = pointGreen
            ),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

    // 에러 메시지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(top = 4.dp, end = 4.dp)
        ) {
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 로그인 버튼
        Button(
            onClick = { viewmodel.onLoginClicked(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = pointGreen),
            enabled = isLoginButtonEnabled
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
                    text = "로그인",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 오류 다이얼로그 출력
        if (showErrorDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showErrorDialog = false
                        }
                    ) {
                        Text("확인", color = pointGreen)
                    }
                },
                title = {
                    Text(text = "로그인 실패", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = uiState.errorMessage ?: "알 수 없는 오류가 발생했습니다.")
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}