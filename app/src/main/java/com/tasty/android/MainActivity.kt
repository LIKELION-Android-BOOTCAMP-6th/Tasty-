package com.tasty.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tasty.android.core.design.component.AppBarAction
import com.tasty.android.core.design.component.CustomBottomAppBar
import com.tasty.android.core.design.component.CustomScaffold
import com.tasty.android.core.design.component.CustomTopAppBar
import com.tasty.android.core.design.theme.MyApplicationTheme
import com.tasty.android.core.navigation.CustomNavHost
import com.tasty.android.core.navigation.TabScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // CustomScaffold 공통 스케폴드 구조 내에서 화면 정의
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as MyApplication
        val authManager = app.container.authManager
        val userStoreManager = app.container.userStoreManager
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        var isUserLoaded = false

        // Splash 화면 유지 조건 설정
        splashScreen.setKeepOnScreenCondition {
            !isUserLoaded
        }

        lifecycleScope.launch {
            // 현재 로그인된 유저가 있으면 정보를 미리 가져옴
            val currentUserId = authManager.getCurrentUser()?.uid
            if (currentUserId != null) {
                userStoreManager.fetchAndCacheUser(currentUserId)
            }

            delay(1000)
            isUserLoaded = true
        }
        enableEdgeToEdge()


        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {

            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController() // NavController 선언
                CustomScaffold(navController = navController)
            }
        }


    }

    override fun onStart() {
        super.onStart()
        // refresh user info
    }
}


