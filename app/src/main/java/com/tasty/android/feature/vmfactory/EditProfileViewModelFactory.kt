package com.tasty.android.feature.vmfactory

import EditProfileViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication

val EditProfileViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            EditProfileViewModel(
                userStoreManager = app.container.userStoreManager,
                storageManager = app.container.storageManager,
                feedStoreManager = app.container.feedStoreManager
            )
        }
    }
