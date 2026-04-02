package com.tasty.android.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.tasty.android.MyApplication
import com.tasty.android.core.place.PlaceManager
import com.tasty.android.feature.tastymap.TastyMapViewmodel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaceSearchBarViewModel(
    private val placesManager: PlaceManager
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApplication
                PlaceSearchBarViewModel(
                    placesManager = app.container.placeManager
                )
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _predictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val predictions: StateFlow<List<AutocompletePrediction>> = _predictions.asStateFlow()

    private val _isFocused = MutableStateFlow(false)
    val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank()) {
            _predictions.value = emptyList()
        } else {
            placesManager.getPredictions(newQuery) { res ->
                _predictions.value = res
            }
        }
    }

    fun onFocusChanged(focused: Boolean) {
        _isFocused.value = focused
        if (!focused) {
            clearSearch()
        }
    }

    fun selectPlace(prediction: AutocompletePrediction, onPlaceSelected: (LatLng) -> Unit) {
        val placeId = prediction.placeId
        placesManager.getPlaceLatLng(placeId) { latLng ->
            latLng?.let {
                onPlaceSelected(it)
                clearSearch()
            }
        }
    }

    private fun clearSearch() {
        _searchQuery.value = ""
        _predictions.value = emptyList()
    }
}