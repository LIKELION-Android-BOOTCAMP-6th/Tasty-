package com.tasty.android.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig

// 닉네임(프로필 정보) 설정 화면
@Composable
fun ProfileSetupScreen(
    navController: NavHostController,
    viewModel: ProfileSetupViewModel = viewModel(),
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    // 상태 관리: 입력된 닉네임
    var nickname by remember { mutableStateOf("") }

    // 버튼 활성화 조건: 닉네임을 입력했을 경우
    val isCompleteEnabled = nickname.isNotBlank()

    // 색상 설정
    val pointGreen = Color(0xFF8DEB8D)
    val lightGreenText = Color(0xFF76D676)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // 타이틀 영역
        Text(
            text = "회원가입이 거의 끝나가요.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = lightGreenText,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "마지막으로 테이스티에서 쓰실 닉네임을 설정해주세요.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "(공백/특수문자 제외 2~8자리로 설정해주세요.)",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 입력 필드 영역
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            placeholder = {
                Text(
                    text = "닉네임을 입력해주세요.",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = pointGreen,
                focusedBorderColor = pointGreen
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(60.dp))

        // 완료 버튼 영역
        Button(
            onClick = { viewModel.onCompleteClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = pointGreen),
            enabled = isCompleteEnabled
        ) {
            Text(
                text = "완료",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}