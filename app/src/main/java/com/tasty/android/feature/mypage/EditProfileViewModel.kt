import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.firebase.UserStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val initialNickname: String = "",
    val initialBio: String = "",
    val initialProfileImageUrl: String = "",
    val nickname: String = "",
    val bio: String = "",
    val selectedImageUri: Uri? = null,
    val isSaveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isChanged: Boolean
        get() = nickname != initialNickname || bio != initialBio || selectedImageUri != null

    val isNicknameValid: Boolean
        get() = nickname.isNotBlank() && nickname.length <= 20
}

class EditProfileViewModel(
    private val userStoreManager: UserStoreManager,
    private val storageManager: StorageManager,
    private val feedStoreManager: FeedStoreManager
) : ViewModel() {
    private val currentUserId: String get() = Firebase.auth.currentUser?.uid ?: ""
    
    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = userStoreManager.getUser(currentUserId)
            
            result.onSuccess { user ->
                if (user != null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            initialNickname = user.nickname,
                            initialBio = user.bio,
                            initialProfileImageUrl = user.profileImageUrl,
                            nickname = user.nickname,
                            bio = user.bio
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "유저 정보를 찾을 수 없습니다.") }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun onNicknameChange(newNickname: String) {
        if (newNickname.length <= 20) {
            _uiState.update { it.copy(nickname = newNickname) }
        }
    }

    fun onBioChange(newBio: String) {
        _uiState.update { it.copy(bio = newBio) }
    }

    fun saveChanges() {
        if (currentUserId.isBlank() || !_uiState.value.isNicknameValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            var profileImageUrl: String? = null
            
            // 1. 이미지가 선택된 경우 업로드 먼저 진행
            val selectedUri = _uiState.value.selectedImageUri
            if (selectedUri != null) {
                val uploadResult = storageManager.uploadProfileImage(selectedUri)
                uploadResult.onSuccess { url ->
                    profileImageUrl = url
                }.onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "이미지 업로드에 실패했습니다: ${e.message}") }
                    return@launch
                }
            }

            // 2. 프로필 정보 업데이트
            val result = userStoreManager.updateProfile(
                userId = currentUserId,
                nickname = _uiState.value.nickname,
                bio = _uiState.value.bio,
                profileImageUrl = profileImageUrl
            )

            result.onSuccess {
                // 3. 피드 정보 동기화 (전역 반영)
                viewModelScope.launch {
                    feedStoreManager.syncAuthorInfoInFeeds(
                        userId = currentUserId,
                        nickname = _uiState.value.nickname,
                        profileImageUrl = profileImageUrl ?: _uiState.value.initialProfileImageUrl
                    )
                }
                _uiState.update { it.copy(isLoading = false, isSaveSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = "저장에 실패했습니다: ${e.message}") }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
