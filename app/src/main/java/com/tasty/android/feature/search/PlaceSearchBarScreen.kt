package com.tasty.android.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place

@Composable
fun PlaceSearchScreen(
    labelText: String,
    onFocusChange: (Boolean) -> Unit,
    onPlaceSelectedLocation: (LatLng) -> Unit,
    onPlaceSelectedRestaurant: (String) -> Unit,
    viewModel: PlaceSearchBarViewModel = viewModel(factory = PlaceSearchBarViewModel .Factory)
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val isFocused by viewModel.isFocused.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .then(if (isFocused) Modifier.fillMaxSize() else Modifier.wrapContentHeight())
            .background(if (isFocused) Color.White else Color.Transparent)
            .statusBarsPadding()
            .clickable(enabled = isFocused) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onQueryChanged,
                label = { Text(labelText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        viewModel.onFocusChanged(it.isFocused)
                        onFocusChange(it.isFocused)
                    },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = Color.Gray, // 커서 색상
                    focusedLabelColor = Color.Black, // 포커스 시 레이블 색상
                    unfocusedLabelColor = Color.Gray,      // 포커스 해제 시 레이블 색상
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            if (isFocused) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(predictions) { prediction ->
                        PredictionItem(prediction) {
                            focusManager.clearFocus()

                            // 장소 유형(placeTypes)
                            val types = prediction.placeTypes
                            if (types.contains(Place.Type.RESTAURANT) ||
                                types.contains(Place.Type.FOOD)) {
                                // 음식점인 경우
                                onPlaceSelectedRestaurant(prediction.placeId)
                            } else {
                                // 그 외(지역/주소 등)인 경우
                                viewModel.selectPlace(prediction, onPlaceSelectedLocation)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionItem(prediction: AutocompletePrediction, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = prediction.getPrimaryText(null).toString(),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = prediction.getSecondaryText(null).toString(),
            fontSize = 12.sp,
            color = Color.Gray
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}