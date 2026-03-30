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
import com.tasty.android.core.firebase.UserStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false, // 프로필 업데이트 성공
    val errorMessage: String? = null
)
class ProfileSetupViewModel(
    private val authManager: AuthManager,
    private val userstoreManager: UserStoreManager
): ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                ProfileSetupViewModel(
                    authManager = app.container.authManager,
                    userstoreManager = app.container.UserStoreManager
                )
            }
        }
    }

    fun onCompleteClick(nickName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val user = authManager.getCurrentUser()

                if (user != null) {
                    // 1. 유저 정보 가져오기 (성공 시 데이터 반환, 실패 시 catch로 이동)
                    val userData = userstoreManager.getUser(user.uid).getOrThrow()

                    if (userData != null) {
                        // 2. 데이터 수정 및 저장
                        // 닉네임만 변경된 새로운 User 객체 생성 (copy 사용)
                        val updatedUser = userData.copy(nickname = nickName)
                        userstoreManager.saveUser(updatedUser).getOrThrow()

                        // 3. 모든 작업 성공 시
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "사용자 데이터를 찾을 수 없습니다.")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "로그인 정보가 유효하지 않습니다.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileSetup", "프로필 설정 실패", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "서버 통신 중 오류가 발생했습니다.")
                }
            }
        }
        Log.d("test", "완료 버튼 클릭")
    }
}