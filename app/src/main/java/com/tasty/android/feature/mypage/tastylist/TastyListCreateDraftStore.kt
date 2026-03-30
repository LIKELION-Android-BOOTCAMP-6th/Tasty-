package com.tasty.android.feature.tastylist

object TastyListCreateDraftStore {
    var selectedFeedIds: List<String> = emptyList()
    var selectedFeedCount: Int = 0
    var thumbnailImageUrl: String = ""
    var title: String = ""

    fun clear() {
        selectedFeedIds = emptyList()
        selectedFeedCount = 0
        thumbnailImageUrl = ""
        title = ""
    }
}