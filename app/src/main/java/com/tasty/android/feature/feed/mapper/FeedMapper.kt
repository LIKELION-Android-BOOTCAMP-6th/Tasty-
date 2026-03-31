package com.tasty.android.feature.feed.mapper


import android.os.Build
import androidx.annotation.RequiresApi
import com.tasty.android.core.util.toFormattedDate
import com.tasty.android.feature.feed.FeedDetailPostUiModel
import com.tasty.android.feature.feed.FeedPostUiModel
import com.tasty.android.feature.feed.model.Feed
import java.time.LocalDate


@RequiresApi(Build.VERSION_CODES.O)
fun Feed.toFeedPostUiModel(
    authorNickname: String = "작성자",
    userHandle: String = "tastier",
    authorProfileUrl: String? = null,
    isLiked: Boolean = false
): FeedPostUiModel {
    return FeedPostUiModel(
        feedId = feedId,
        authorId = authorId,
        authorNickname = authorNickname,
        userHandle = userHandle,
        authorProfileUrl = authorProfileUrl,
        placeName = restaurantName,
        address = addressInfo.roadAddress,
        dateText = createdAt?.toFormattedDate() ?: LocalDate.now().toString(),
        likeCount = likeCount,
        commentCount = commentCount,
        rating = rating,
        description = content,
        isLiked = isLiked,
        thumbnailImageUrl = feedImageUrls.firstOrNull()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun Feed.toFeedDetailPostUiModel(
    authorNickname: String = "작성자",
    userHandle: String = "tastier",
    authorProfileUrl: String? = null,
    isLiked: Boolean = false
): FeedDetailPostUiModel {
    return FeedDetailPostUiModel(
        id = feedId,
        authorId = authorId,
        authorNickname = authorNickname,
        userHandle = userHandle,
        authorProfileUrl = authorProfileUrl,
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