package com.tasty.android.feature.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tasty.android.core.place.PlaceManager

@Composable
fun PlaceSearchScreen(
    placesManager: PlaceManager,
    labelText: String,
    onFocusChange: (Boolean) -> Unit,
    onPlaceSelected: (LatLng) -> Unit // 추가: 좌표를 전달할 콜백
) {
    // 포커스 상태를 저장하는 변수
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 포커스 상태일 때만 전체 화면을 하얀색으로 덮음
            .background(if (isFocused) Color.White else Color.Transparent)
            .statusBarsPadding() // 상태바 영역 침범 방지
            .clickable(
                onClick = { focusManager.clearFocus() },
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding()
        )
        {
            // 검색창
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    // 글자를 지웠을 때 리스트 비워줌
                    if (it.isBlank()) {
                        predictions = emptyList()
                        isFocused = false
                    } else {
                        placesManager.getPredictions(it) { res ->
                            predictions = res
                        }
                    }
                },
                label = { Text(labelText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // 포커스 여부에 따라 상태 업데이트
                        isFocused = focusState.isFocused
                        // 포커스 상태 콜백으로 전달
                        onFocusChange(focusState.isFocused)
                        // 포커스를 잃었을 때
                        if (!focusState.isFocused) {
                            searchQuery = ""
                            predictions = emptyList()
                        }
                    },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,   // 포커스 되었을 때 배경 흰색
                    unfocusedContainerColor = Color.White, // 포커스 없을 때 배경 흰색
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 결과 리스트
            LazyColumn {
                items(predictions) { prediction ->
                    PredictionItem(prediction) {
                        // 배경을 다시 투명하게
                        focusManager.clearFocus()

                        // 클릭 시 검색창 텍스트를 전체 주소로 바꾸고 리스트 닫기
                        searchQuery = prediction.getFullText(null).toString()

                        val placeId = prediction.placeId
                        Log.d("test", "선택된 장소 ID: $placeId")

                        // 검색 결과 반영
                        placesManager.getPlaceLatLng(placeId) { latLng ->
                            latLng?.let {
                                onPlaceSelected(it) // 찾은 좌표를 전달
                                searchQuery = "" // 검색창 초기화
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
        Text(text = prediction.getPrimaryText(null).toString(), fontWeight = FontWeight.Bold)
        Text(
            text = prediction.getSecondaryText(null).toString(),
            fontSize = 12.sp,
            color = Color.Gray
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}