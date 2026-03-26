package com.tasty.android.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    fun onLoginClicked() {
        Log.d("test","로그인 버튼 클릭")
    }
}