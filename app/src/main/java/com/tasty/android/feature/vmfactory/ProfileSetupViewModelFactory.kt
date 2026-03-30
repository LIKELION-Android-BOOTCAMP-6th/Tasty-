package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.feature.feed.FeedViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.tasty.android.feature.auth.ProfileSetupViewModel

//val ProfileSetupViewModelFactory: ViewModelProvider.Factory =
//    viewModelFactory {
//        initializer {
//            val app = this[APPLICATION_KEY] as MyApplication
//            ProfileSetupViewModel(
//                // 의존성 주입 ex) app.container.firestoreManager
//            )
//        }
//    }