package com.tasty.android.feature.feed.mapper


import android.os.Build
import androidx.annotation.RequiresApi
import com.google.type.DateTime
import com.tasty.android.core.util.toFormattedDate
import com.tasty.android.feature.feed.FeedDetailPostUiModel
import com.tasty.android.feature.feed.FeedPostUiModel
import com.tasty.android.feature.feed.model.Feed
import okhttp3.internal.notify
import java.time.LocalDate


@RequiresApi(Build.VERSION_CODES.O)
fun Feed.toFeedPostUiModel(
    authorName: String = "작성자",
    authorRegion: String = addressInfo.subRegion.ifBlank { addressInfo.mainRegion },
    isLiked: Boolean = false
): FeedPostUiModel {
    return FeedPostUiModel(
        id = feedId,
        authorId = authorId,
        authorName = authorName,
        placeName = restaurantName,
        address = addressInfo.roadAddress,
        dateText = createdAt?.toFormattedDate() ?: LocalDate.now().toString(),
        likeCount = likeCount,
        commentCount = commentCount,
        rating = rating,
        description = content,
        isLiked = isLiked
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun Feed.toFeedDetailPostUiModel(
    authorName: String = "작성자",
    isLiked: Boolean = false
): FeedDetailPostUiModel {
    return FeedDetailPostUiModel(
        id = feedId,
        authorId = authorId,
        authorName = authorName,
        placeName = restaurantName,
        address = addressInfo.roadAddress,
        dateText = createdAt?.toFormattedDate() ?: LocalDate.now().toString(),
        likeCount = likeCount,
        commentCount = commentCount,
        rating = rating,
        isLiked = isLiked,
        content = content,
        shortReview = shortReview,
        imageUrls = feedImageUrls,
    )
}