package com.tasty.android.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.core.firebase.AuthManager
import com.tasty.android.core.firebase.FirestoreManager
import com.tasty.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false, // 회원가입 성공
    val isFirestoreSuccess: Boolean = false, // Firestore 유저 데이터 생성 완료
    val errorMessage: String? = null
)

class SignUpAccountViewmodel(
    private val authManager: AuthManager,
    private val firestoreManager: FirestoreManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                SignUpAccountViewmodel(
                    authManager = app.container.authManager,
                    firestoreManager = app.container.firestoreManager
                )
            }
        }
    }

    fun onNextClick(email: String, password: String) {
        viewModelScope.launch {
            // 1. 로딩 상태 시작
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 2. Firebase Auth 회원가입 시도
            val authResult = authManager.signUp(email, password)

            authResult.onSuccess { uid ->
                // 인증 성공 시 UI 상태 업데이트 (isSuccess = true)
                _uiState.update { it.copy(isSuccess = true) }

                // 3. User 객체 구성
                val newUser = User(
                    userId = uid,
                    userHandle = email.substringBefore("@"), // @ 앞부분 추출
                    nickname = "",             // 닉네임 정보는 이후에 업데이트
                )

                // 4. Firestore에 유저 정보 저장
                val firestoreResult = firestoreManager.saveUser(newUser)

                firestoreResult.onSuccess {
                    // 최종 성공: 모든 과정 완료
                    _uiState.update { it.copy(isFirestoreSuccess = true, isLoading = false) }
                }.onFailure { e ->
                    // Firestore 저장 실패 시
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "유저 데이터 저장 실패: ${e.localizedMessage}"
                        )
                    }
                }

            }.onFailure { e ->
                // Auth 회원가입 실패 시 (중복 이메일 등)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
        Log.d("test", "다음 버튼이 클릭되었습니다!")
    }
}