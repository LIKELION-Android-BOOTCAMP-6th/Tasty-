package com.tasty.android.feature.feed

import androidx.lifecycle.ViewModel
import com.tasty.android.feature.feed.model.FeedComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeedDetailPostUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val placeName: String,
    val address: String,
    val rating: Int,
    val shortReview: String,
    val content: String,
    val imageUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val dateText: String
)

data class FeedDetailUiState(
    val isLoading: Boolean = false,
    val post: FeedDetailPostUiModel? = null,
    val comments: List<FeedComment> = emptyList(),
    val commentInput: String = ""
) {
    val canSubmitComment: Boolean
        get() = commentInput.trim().isNotBlank()
}

class FeedDetailViewModel : ViewModel() {

    private val detailDummyMap = mapOf(
        "feed_1" to FeedDetailPostUiModel(
            id = "feed_1",
            authorId = "user_1",
            authorName = "tasty_user",
            placeName = "테이스티 파스타",
            address = "서울 성동구 성수동 123-45",
            rating = 4,
            shortReview = "분위기도 좋고 음식도 깔끔해서 재방문하고 싶은 곳이었어요.",
            content = """
                파스타가 맛있고 사장님이 친절하시네요.
                
                이집은 크림 파스타가 제일 맛있는 것 같아요..
                
                재방문 의사 200%입니다!
            """.trimIndent(),
            imageUrl = null,
            likeCount = 12,
            commentCount = 3,
            dateText = "03.25"
        ),
        "feed_2" to FeedDetailPostUiModel(
            id = "feed_2",
            authorId = "user_2",
            authorName = "foodie",
            placeName = "성수 브런치 하우스",
            address = "서울 성동구 성수동 77-10",
            rating = 5,
            shortReview = "브런치 메뉴가 다양하고 사진 찍기에도 좋았어요.",
            content = """
                사진 찍기 좋고 음식도 전체적으로 무난하게 맛있었어요.
                친구랑 가볍게 방문하기 좋았습니다.
            """.trimIndent(),
            imageUrl = null,
            likeCount = 25,
            commentCount = 8,
            dateText = "03.24"
        ),
        "feed_3" to FeedDetailPostUiModel(
            id = "feed_3",
            authorId = "user_3",
            authorName = "yummy",
            placeName = "강남 스테이크",
            address = "서울 강남구 테헤란로 10",
            rating = 3,
            shortReview = "고기 맛은 좋았고 전체적으로 무난했어요.",
            content = """
                양도 꽤 있었고 메뉴도 빨리 나와서 점심 식사로 괜찮았어요.
                근처에서 간단히 먹기 좋았습니다.
            """.trimIndent(),
            imageUrl = null,
            likeCount = 7,
            commentCount = 1,
            dateText = "03.23"
        ),
        "feed_4" to FeedDetailPostUiModel(
            id = "feed_4",
            authorId = "user_4",
            authorName = "mukstar",
            placeName = "서초 태국요리",
            address = "서울 서초구 서초대로 20",
            rating = 5,
            shortReview = "향신료가 강하지 않아서 먹기 편했고 전체적으로 만족스러웠어요.",
            content = """
                태국 요리는 이 지점이 근방에서 제일 맛있는 것 같아요.
                여러분도 오셔서 드셔보세요 꼭이요.
            """.trimIndent(),
            imageUrl = null,
            likeCount = 18,
            commentCount = 4,
            dateText = "03.22"
        ),
        "feed_5" to FeedDetailPostUiModel(
            id = "feed_5",
            authorId = "user_5",
            authorName = "ricecat",
            placeName = "송파 덮밥집",
            address = "서울 송파구 올림픽로 33",
            rating = 4,
            shortReview = "가성비가 좋고 점심으로 먹기 좋은 메뉴가 많았어요.",
            content = """
                점심으로 먹기 간단하고 음식도 빨리 나와서
                저희같은 직장인들에게는 엄청 좋은 식당인 것 같네요!
                가격도 이정도면 합리적이고 좋습니다.
            """.trimIndent(),
            imageUrl = null,
            likeCount = 10,
            commentCount = 2,
            dateText = "03.21"
        )
    )

    private val dummyComments = mapOf(
        "feed_1" to listOf(
            FeedComment(
                commentId = "comment_1",
                feedId = "feed_1",
                authorId = "comment_1",
                content = "파스타가 맛있다고? 가봐야겠네",
                createdAt = "3일전"
            )
        ),
        "feed_2" to listOf(
            FeedComment(
                commentId = "comment_2",
                feedId = "feed_2",
                authorId = "brunch_day",
                content = "여기 저도 가봤는데 괜찮았어요!",
                createdAt = "1일전"
            ),
            FeedComment(
                commentId = "comment_3",
                feedId = "feed_2",
                authorId = "latte_cat",
                content = "사진 찍기 좋은 곳 맞아요",
                createdAt = "1일전"
            )
        ),
        "feed_3" to listOf(
            FeedComment(
                commentId = "comment_4",
                feedId = "feed_3",
                authorId = "office_lunch",
                content = "점심 메뉴로 저장해둬야겠다",
                createdAt = "방금 전"
            )
        )
    )

    private val _uiState = MutableStateFlow(FeedDetailUiState())
    val uiState: StateFlow<FeedDetailUiState> = _uiState.asStateFlow()

    fun loadFeedDetail(feedId: String) {
        val post = detailDummyMap[feedId] ?: detailDummyMap["feed_1"]
        val comments = dummyComments[feedId].orEmpty()

        _uiState.update {
            it.copy(
                isLoading = false,
                post = post?.copy(commentCount = comments.size),
                comments = comments,
                commentInput = ""
            )
        }
    }

    fun updateCommentInput(input: String) {
        _uiState.update { currentState ->
            currentState.copy(commentInput = input)
        }
    }

    fun submitComment(feedId: String) {
        val input = _uiState.value.commentInput.trim()
        if (input.isBlank()) return

        val newComment = FeedComment(
            commentId = "comment_${System.currentTimeMillis()}",
            feedId = feedId,
            authorId = "me",
            content = input,
            createdAt = "방금 전"
        )

        _uiState.update { currentState ->
            val updatedComments = currentState.comments + newComment
            currentState.copy(
                comments = updatedComments,
                commentInput = "",
                post = currentState.post?.copy(commentCount = updatedComments.size)
            )
        }
    }
}