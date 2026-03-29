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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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

//@Composable
//fun PlaceSearchScreen(placesManager: PlacesManager, labelText: String) {
//
//    // 포커스 상태를 저장하는 변수
//    val focusManager = LocalFocusManager.current
//    var isFocused by remember { mutableStateOf(false) }
//
//    var searchQuery by remember { mutableStateOf("") }
//    var predictions by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            // 포커스 상태일 때만 전체 화면을 하얀색으로 덮음
//            .background(if (isFocused) Color.White else Color.Transparent)
//            .clickable(
//                onClick = { focusManager.clearFocus() },
//            )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//                .statusBarsPadding()
//        )
//        {
//            // 검색창
//            OutlinedTextField(
//                value = searchQuery,
//                onValueChange = {
//                    searchQuery = it
//                    // 글자를 지웠을 때 리스트 비워줌
//                    if (it.isBlank()) {
//                        predictions = emptyList()
//                        isFocused = false
//                    } else {
//                        placesManager.getPredictions(it) { res ->
//                            predictions = res
//                        }
//                    }
//                },
//                label = { Text(labelText) },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .onFocusChanged { focusState ->
//                        // 포커스 여부에 따라 상태 업데이트
//                        isFocused = focusState.isFocused
//                        // 포커스를 잃었을 때
//                        if(!focusState.isFocused)
//                        {
//                            searchQuery = ""
//                            predictions = emptyList()
//                            isFocused = false
//                        }
//                    },
//                singleLine = true
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // 결과 리스트
//            LazyColumn {
//                items(predictions) { prediction ->
//                    PredictionItem(prediction) {
//                        // 배경을 다시 투명하게
//                        focusManager.clearFocus()
//
//                        // 클릭 시 검색창 텍스트를 전체 주소로 바꾸고 리스트 닫기
//                        searchQuery = prediction.getFullText(null).toString()
//
//                        // TODO: 여기서 placeId를 이용해 상세 좌표(LatLng)를 가져오는 로직을 추가
//                        val placeId = prediction.placeId
//                        Log.d("test", "선택된 장소 ID: $placeId")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun PredictionItem(prediction: AutocompletePrediction, onClick: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(12.dp)
//    ) {
//        Text(text = prediction.getPrimaryText(null).toString(), fontWeight = FontWeight.Bold)
//        Text(
//            text = prediction.getSecondaryText(null).toString(),
//            fontSize = 12.sp,
//            color = Color.Gray
//        )
//        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
//    }
//}