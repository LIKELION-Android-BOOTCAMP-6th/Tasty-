package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.feature.mypage.tastylist.EditTastyListViewModel

val EditTastyListViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            EditTastyListViewModel(
                tastyStoreManager = app.container.tastyStoreManager,
                myPageStoreManager = app.container.myPageStoreManager,
                feedStoreManager = app.container.feedStoreManager
            )
        }
    }
