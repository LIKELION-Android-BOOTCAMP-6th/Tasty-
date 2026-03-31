package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.feature.mypage.MyPageViewModel

val MyPageViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            MyPageViewModel(
                feedStoreManager = app.container.feedStoreManager,
                myPageStoreManager = app.container.myPageStoreManager,
                userStoreManager = app.container.userStoreManager
            )
        }
    }