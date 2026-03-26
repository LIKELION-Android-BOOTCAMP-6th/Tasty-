package com.tasty.android.feature.feed.mapper


import com.tasty.android.feature.feed.FeedPostUiModel
import com.tasty.android.feature.feed.model.Feed


fun Feed.toFeedPostUiModel(
    authorName: String = "작성자",
    authorRegion: String = addressInfo.subRegion.ifBlank { addressInfo.mainRegion }
): FeedPostUiModel {
    return FeedPostUiModel(
        id = feedId,
        authorId = authorId,
        authorName = authorName,
        placeName = restaurantName,
        address = addressInfo.roadAddress,
        dateText = createdAt,
        likeCount = likeCount,
        commentCount = commentCount,
        rating = rating,
        description = content
    )
}