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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false, // 인증 성공
    val isDataLoaded: Boolean = false, // Firestore 데이터 로드 완료
    val errorMessage: String? = null
)

class LoginViewModel(
    private val authManager: AuthManager,
    private val firestoreManager: FirestoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                LoginViewModel(
                    authManager = app.container.authManager,
                    firestoreManager = app.container.firestoreManager
                )
            }
        }
    }

    fun onLoginClicked(email: String, pass: String) {
        viewModelScope.launch {
            // 로딩 시작 및 이전 에러 초기화
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. Firebase Auth 로그인 수행
                val authResult = authManager.signIn(email, pass)

                if (authResult.isSuccess) {
                    // 2. 로그인 성공 시 Firestore에서 유저 정보 가져오기
                    val userId = authManager.getCurrentUser()?.uid // 현재 유저 ID 획득

                    if (userId != null) {
                        // firestoreManager에서 유저 정보를 가져오는 함수 호출 (가정)
                        val userData = firestoreManager.getUser(userId)

                        if (userData != null) {
                            // 3. 데이터 로드까지 모두 성공 한 경우 상태 업데이트
                            _uiState.update {
                                it.copy(isLoading = false, isSuccess = true, isDataLoaded = true)
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = "유저 정보를 찾을 수 없습니다.")
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "로그인 정보가 일치하지 않습니다.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage)
                }
            }
        }
        Log.d("test", "로그인 버튼 클릭")
    }
}