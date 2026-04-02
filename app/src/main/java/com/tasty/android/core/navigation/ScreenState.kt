package com.tasty.android.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Dining
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

// 탭 화면 루트 및 상태 정의
enum class TabScreen(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    // route: 피드 탭 메인화면
    FEED("feed","피드", Icons.Filled.Home, inactiveIcon = Icons.Outlined.Home),

    // 아래 아이콘은 추후 extended 아이콘 main에 Merge 후에 라이브러리 추가 후 변경
    // route: 테이스티 리스트 탭 메인화면
    TASTY("tasty","테이스티 리스트", Icons.Filled.LocalDining,Icons.Outlined.LocalDining),
    // route: 지도 탭 메인화면
    MAP("map","지도", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    // route: 마이페이지 탭 메인화면
    MY_PAGE("my_page","마이페이지", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

// 개별 화면 루트 정의
enum class Screen(
    val route: String
) { 
    /** Auth **/
    AUTH_ON_BOARDING("auth_on_boarding"), // 온보딩 화면
    AUTH_SIGN_UP_EMAIL_PWD("auth_sign_up_email_pwd"), // 회원가입: 이메일/비밀번호 입력 화면
    AUTH_SIGN_UP_SET_PROFILE("auth_sign_up_set_profile"), // 회원가입: 프로필 정보(닉네임 등) 입력 화면
    AUTH_LOGIN("auth_login"), // 로그인: 이메일/비밀번호 입력 화면

    /** My Page **/
    MY_PAGE_SELECT_FEEDS("my_page_select_feeds"), // 마이페이지 - 새 테이스티 리스트 만들기 피드 선택 화면
    MY_PAGE_SET_THUMBNAIL_TITLE("my_page_set_thumbnail_title"), // 마이페이지 - 새 테이스티 리스트 만들기 썸네일/제목 설정 화면
    MY_PAGE_EDIT_PROFILE("my_page_edit_profile"), // 마이페이지 - 프로필 수정 화면

    /** Map **/
    MAP_SEARCH_LOCATION ("map_search_location"), // 지도 - 지역 검색 화면
    MAP_RESTAURANT_DETAIL("map_restaurant_detail"), // 지도 - 식당 세부 화면

    /** FEED **/
    FEED_CREATE_FEED("feed_create_feed"), // 피드 생성 화면
    FEED_SEARCH_RESTAURANT("feed_search_restaurant"), // 피드 - 작성 시 식당 검색 화면
    FEED_DETAIL("feed_detail"), // 피드 - 세부 화면
    /** Tasty **/
    TASTY_DETAIL("tasty_detail"), // 테이스티- 세부 화면
    EDIT_TASTY_LIST("edit_tasty_list"), // 테이스티 리스트 수정 화면

    /** User Profile **/
    USER_PROFILE("user_profile") // 유저 프로필 - 다른 유저 프로필 화면

}