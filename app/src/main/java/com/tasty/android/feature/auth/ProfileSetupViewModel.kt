package com.tasty.android.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel


class ProfileSetupViewModel: ViewModel() {
    fun onCompleteClick(){
        Log.d("test", "완료 버튼이 클릭되었습니다!")
    }
}