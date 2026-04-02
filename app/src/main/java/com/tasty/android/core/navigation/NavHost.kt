package com.tasty.android.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.feature.auth.LoginScreen
import com.tasty.android.feature.auth.OnboardingScreen
import com.tasty.android.feature.auth.ProfileSetupScreen
import com.tasty.android.feature.auth.SignUpScreen
import com.tasty.android.feature.feed.FeedDetailScreen
import com.tasty.android.feature.feed.FeedScreen
import com.tasty.android.feature.feed.FeedSearchRestaurantScreen
import com.tasty.android.feature.feed.FeedWriteScreen
import com.tasty.android.feature.feed.FeedWriteViewModel
import com.tasty.android.feature.mypage.EditProfileScreen
import com.tasty.android.feature.mypage.MyPageScreen
import com.tasty.android.feature.tasty.TastyDetailScreen
import com.tasty.android.feature.tasty.TastyScreen
import com.tasty.android.feature.tastylist.TastyListCreateSelectFeedsScreen
import com.tasty.android.feature.tastylist.TastyListCreateSetupScreen
import com.tasty.android.feature.mypage.tastylist.EditTastyListScreen
import com.tasty.android.feature.vmfactory.FeedWriteViewModelFactory
import com.tasty.android.feature.profile.UserProfileScreen
import com.tasty.android.feature.tastymap.TastyMapScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    // 로그인 상태 여부 확인
    val isLoggedIn = Firebase.auth.currentUser != null
    // 로그인 상태에 따라 첫 시작 그래프를 결정
    val startDestination = if(isLoggedIn) "feed_graph" else "auth_graph"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        /**AUTH GRAPH **/
        navigation(
            startDestination = Screen.AUTH_ON_BOARDING.route,
            route = "auth_graph"
        ) {
            composable(Screen.AUTH_ON_BOARDING.route) {
                OnboardingScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }

            composable(Screen.AUTH_SIGN_UP_EMAIL_PWD.route) {
                SignUpScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }

            composable(Screen.AUTH_SIGN_UP_SET_PROFILE.route) {
                ProfileSetupScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }

            composable(Screen.AUTH_LOGIN.route) {
                LoginScreen(navController, onScaffoldConfigChange = onScaffoldConfigChange)
            }
        }

        /** FEED GRAPH **/
        navigation(
            startDestination = TabScreen.FEED.route,
            route = "feed_graph"
        ) {
            composable(TabScreen.FEED.route) {
                FeedScreen(
                    navController,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable("${Screen.FEED_DETAIL.route}/{feedId}") { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable(Screen.FEED_CREATE_FEED.route) { backStackEntry ->
                val viewModel: FeedWriteViewModel = viewModel(backStackEntry, factory = FeedWriteViewModelFactory)
                FeedWriteScreen(
                    navController,
                    viewModel = viewModel,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable(Screen.FEED_SEARCH_RESTAURANT.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.FEED_CREATE_FEED.route)
                }
                val sharedViewModel: FeedWriteViewModel = viewModel(
                    parentEntry,
                    factory = FeedWriteViewModelFactory
                )

                FeedSearchRestaurantScreen(
                    navController = navController,
                    viewModel = sharedViewModel,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable(Screen.USER_PROFILE.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController,
                    userId,
                    onScaffoldConfigChange
                )
            }

            composable("${Screen.TASTY_DETAIL.route}/{tastyId}") { backStackEntry ->
                val tastyId = backStackEntry.arguments?.getString("tastyId") ?: ""
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { id -> navController.navigate("${Screen.FEED_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
        }

        /** TASTY GRAPH **/
        navigation(
            startDestination = TabScreen.TASTY.route,
            route = "tasty_graph"
        ) {
            composable(TabScreen.TASTY.route) {
                TastyScreen(
                    onClickTastyItem = { id -> navController.navigate("${Screen.TASTY_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable("${Screen.TASTY_DETAIL.route}/{tastyId}") { backStackEntry ->
                val tastyId = backStackEntry.arguments?.getString("tastyId") ?: ""
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { id -> navController.navigate("${Screen.FEED_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable("${Screen.FEED_DETAIL.route}/{feedId}") { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable("${Screen.USER_PROFILE.route}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController,
                    userId,
                    onScaffoldConfigChange
                )
            }
        }

        /** MAP GRAPH **/
        navigation(
            startDestination = TabScreen.MAP.route,
            route = "map_graph"
        ) {
            composable(TabScreen.MAP.route) {
                TastyMapScreen(navController, onScaffoldConfigChange)
            }

            composable(Screen.MAP_SEARCH_LOCATION.route) { }

            composable(Screen.MAP_RESTAURANT_DETAIL.route) { }

            composable("${Screen.USER_PROFILE.route}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserProfileScreen(
                    navController,
                    userId,
                    onScaffoldConfigChange
                )
            }

            composable("${Screen.FEED_DETAIL.route}/{feedId}") { backStackEntry ->
                val feedId = backStackEntry.arguments?.getString("feedId") ?: ""
                FeedDetailScreen(
                    navController,
                    feedId,
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }

            composable("${Screen.TASTY_DETAIL.route}/{tastyId}") { backStackEntry ->
                val tastyId = backStackEntry.arguments?.getString("tastyId") ?: ""
                TastyDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onClickFeed = { id -> navController.navigate("${Screen.FEED_DETAIL.route}/$id") },
                    onScaffoldConfigChange = onScaffoldConfigChange
                )
            }
        }

        /** MY PAGE GRAPH **/
        navigation(
            startDestination = TabScreen.MY_PAGE.route,
            route = "my_page_graph"
        ) {
            composable(TabScreen.MY_PAGE.route) {
                MyPageScreen(navController, onScaffoldConfigChange)
            }

            composable(Screen.MY_PAGE_SELECT_FEEDS.route) {
                TastyListCreateSelectFeedsScreen(navController, onScaffoldConfigChange)
            }

            composable(Screen.MY_PAGE_SET_THUMBNAIL_TITLE.route) {
                TastyListCreateSetupScreen(navController, onScaffoldConfigChange)
            }

            composable(Screen.MY_PAGE_EDIT_PROFILE.route) {
                EditProfileScreen(navController, onScaffoldConfigChange)
            }

            composable("${Screen.EDIT_TASTY_LIST.route}") { backStackEntry ->
                val tastyListId = backStackEntry.arguments?.getString("tastyListId") ?: ""
                EditTastyListScreen(
                    tastyListId,
                    navController,
                    onScaffoldConfigChange
                )
            }
        }
    }
}