package com.tasty.android.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await

class LocationManager(private val context: Context) {
    // 로케이션 클라이언트 선언
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    // Remove fusedLocationClient from parameters here
    fun getCurrentLocation(onLocationRetrieved: (Double, Double) -> Unit) {
        // Use the internal locationClient instead
        locationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationRetrieved(location.latitude, location.longitude)
            }
        }
    }

    // 현재 위치 get 반환 : latitude, longitude
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>> {
        return try {
            val location = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            if (location != null) {
                Result.success(
                    Pair(
                        location.latitude,
                        location.longitude
                    )
                )
            } else {
                Result.failure(
                    Exception("위치 정보를 가져올 수 없습니다. 네트워크와 GPS 설정을 확인해주세요.")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 식당과 현재 내 위치 사이의 거리 계산
    fun calculateDistanceBetween(
        currentLat: Double,
        currentLon: Double,
        targetLat: Double,
        targetLon: Double
    ) :Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            currentLat,
            currentLon,
            targetLat,
            targetLon,
            results
        )
        return results[0].toDouble()
    }

    fun checkLocationSettings(
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // 앱에서 요구하는 위치 정확도 설정
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // 위치가 꺼져 있으면 무조건 다이얼로그 표시

        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { onSuccess() }
        task.addOnFailureListener { exception -> onFailure(exception) }
    }
}