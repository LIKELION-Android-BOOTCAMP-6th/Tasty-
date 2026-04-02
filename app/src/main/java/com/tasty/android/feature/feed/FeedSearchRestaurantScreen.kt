package com.tasty.android.feature.feed

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.tasty.android.core.design.component.ScaffoldConfig

@Composable
fun FeedSearchRestaurantScreen(
    navController: NavHostController,
    viewModel: FeedWriteViewModel,
    onScaffoldConfigChange: (ScaffoldConfig) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    var query by remember {
        mutableStateOf("")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            viewModel.loadNearbyRestaurants()
        }
    }
    LaunchedEffect(Unit) {
        onScaffoldConfigChange(
            ScaffoldConfig(
                title = "식당 검색",
                showTopBar = true,
                containsBackButton = true,
                onBackClick = { navController.popBackStack() },
                showBottomBar = false
            )
        )

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 검색 바
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchRestaurant(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("식당 이름으로 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 로딩 뷰 or 리스트
        if (uiState.isLoadingRestaurants) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults, key = { it.restaurantId }) { item ->
                    Text(
                        text = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectRestaurant(item.restaurantId)
                                navController.popBackStack()
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = item.address,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectRestaurant(item.restaurantId)
                                navController.popBackStack()
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}