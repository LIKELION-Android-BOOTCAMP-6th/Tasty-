package com.tasty.android.feature.feed.mapper

import com.tasty.android.core.model.Feed
import com.tasty.android.feature.feed.FeedPostItem

fun Feed.toFeedPostItem(
    authorName: String = "작성자",
    authorRegion: String = addressInfo.subRegion.ifBlank { addressInfo.mainRegion }
): FeedPostItem {
    return FeedPostItem(
        id = feedId,
        authorName = authorName,
        authorRegion = authorRegion,
        placeName = restaurantName,
        address = addressInfo.roadAddress,
        date = createdAt,
        likeCount = likeCount,
        commentCount = commentCount,
        rating = rating.toFloat(),
        description = content,
        imageUrl = imagesUrl.firstOrNull()
    )
}