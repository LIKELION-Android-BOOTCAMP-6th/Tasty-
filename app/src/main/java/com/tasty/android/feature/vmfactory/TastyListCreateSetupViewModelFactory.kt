package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tasty.android.MyApplication
import com.tasty.android.feature.tastylist.TastyListCreateSetupViewModel

val TastyListCreateSetupViewModelFactory: ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as MyApplication
            TastyListCreateSetupViewModel(
                tastyStoreManager = app.container.tastyStoreManager,
                storageManager = app.container.storageManager
            )
        }
    }
