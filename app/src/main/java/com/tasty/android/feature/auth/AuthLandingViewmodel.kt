package com.tasty.android.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel

// 로그인 뷰 모델
class AuthLandingViewmodel: ViewModel() {
    fun onSignUpClick(){
        Log.d("test", "회원가입 버튼이 클릭되었습니다!")
    }
    fun onLoginClicked() {
        Log.d("test", " 로그인 화면으로 이동합니다.")
    }
}