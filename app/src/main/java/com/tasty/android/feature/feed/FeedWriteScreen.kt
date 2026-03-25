package com.tasty.android.feature.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig
import com.tasty.android.core.navigation.Screen

@Composable
fun FeedWriteScreen(
    navController: NavHostController,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "Tasty",
                showTopBar = true,
                showBottomBar = false,
                containsBackButton = false


            )
        )
    }
    Text(text = "피드 작성 화면")
}
