package com.tasty.android.core.model

data class AddressInfo(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mainRegion: String = "",
    val roadAddress: String = "",
    val subRegion: String = ""
)