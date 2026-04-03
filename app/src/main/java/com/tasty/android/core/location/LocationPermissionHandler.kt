package com.tasty.android.core.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException

@Composable
fun rememberLocationPermissionState(
    locationManager: LocationManager,
    onPermissionGranted: () -> Unit,
): LocationPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity


    val gpsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // GPS를 켰으므로 성공 콜백 실행
            onPermissionGranted()
        }
    }

    // 2. 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            // 권한이 허용됨 GPS 하드웨어가 켜져 있는지 확인
            locationManager.checkLocationSettings(
                onResolutionRequired = { exception ->
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    gpsLauncher.launch(intentSenderRequest)
                },
                onSuccess = {
                    onPermissionGranted()
                }
            )
        }
    }

    return remember(locationManager) {
        LocationPermissionState(
            context = context,
            locationManager = locationManager,
            permissionLauncher = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onPermissionGranted = onPermissionGranted,
            gpsLauncher = { exception ->
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                gpsLauncher.launch(intentSenderRequest)
            }
        )
    }
}

class LocationPermissionState(
    private val context: Context,
    private val locationManager: LocationManager,
    private val permissionLauncher: () -> Unit,
    private val onPermissionGranted: () -> Unit,
    private val gpsLauncher: (ResolvableApiException) -> Unit
) {
    fun checkAndRequestLocation() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission == PackageManager.PERMISSION_GRANTED
        ) {
            // 권한은 이미 있음 -> GPS 하드웨어 확인
            locationManager.checkLocationSettings(
                onResolutionRequired = { exception ->
                    gpsLauncher(exception)
                },
                onSuccess = {
                    onPermissionGranted()
                }
            )
        } else {
            // 권한 요청
            permissionLauncher()
        }
    }
}
