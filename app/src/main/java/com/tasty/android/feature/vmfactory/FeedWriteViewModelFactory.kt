package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.core.firebase.FeedStoreManager
import com.tasty.android.core.firebase.StorageManager
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.feed.FeedWriteViewModel

val FeedWriteViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            FeedWriteViewModel(
                feedStoreManager = app.container.feedStoreManager,
                storageManager = app.container.storageManager,
                placeManager = app.container.placeManager,
            )
        }
    }