package com.tasty.android.feature.vmfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasty.android.core.firebase.*
import com.tasty.android.feature.profile.UserProfileViewModel

class UserProfileViewModelFactory(
    private val targetUserId: String,
    private val userStoreManager: UserStoreManager = UserStoreManager(),
    private val myPageStoreManager: MyPageStoreManager = MyPageStoreManager(),
    private val tastyStoreManager: TastyStoreManager = TastyStoreManager(),
    private val feedStoreManager: FeedStoreManager = FeedStoreManager()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            return UserProfileViewModel(
                targetUserId,
                userStoreManager,
                myPageStoreManager,
                tastyStoreManager,
                feedStoreManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
