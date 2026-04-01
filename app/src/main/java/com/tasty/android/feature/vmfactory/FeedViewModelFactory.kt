package com.tasty.android.feature.vmfactory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.feature.feed.FeedViewModel

@RequiresApi(Build.VERSION_CODES.O)
val FeedViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            FeedViewModel(
                locationManager = app.container.locationManager,
                feedStoreManager = app.container.feedStoreManager,
                userStoreManager = app.container.userStoreManager,
                tastyStoreManager = app.container.tastyStoreManager
            )
        }
    }